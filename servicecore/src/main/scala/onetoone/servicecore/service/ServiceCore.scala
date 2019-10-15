package onetoone.servicecore.service

//Imports
import com.datastax.driver.core.ConsistencyLevel
import onetoone.servicecore.AppConf
import onetoone.servicecore.directives.{Exceptions, LoggingAndMetrics, Rejections, Unmarshallers}
import onetoone.servicecore.models.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.models.http.programs.Level
import onetoone.servicecore.models.http.statuscheck.StatusCheckResponse
import onetoone.servicecore.models.kafka.KafkaMsg
import onetoone.servicecore.util.MathUtil
import org.apache.kafka.clients.consumer.ConsumerConfig
//Kafka
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.apache.kafka.clients.producer.KafkaProducer
//Akka
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
//Scala
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
//Slf4j
//Circe
import io.circe.generic.AutoDerivation
//Java
import java.time.Duration
import java.util.{Properties, UUID}
//Datastax
import com.datastax.driver.core.{Cluster, Row, Session}
//Logback
import io.circe.parser.decode

trait ServiceCore extends LoggingAndMetrics
  with Exceptions with Rejections
  with Unmarshallers with AutoDerivation
  with ServiceHandlers with LoggingHandles
  with MathUtil {

  implicit val materializer: ActorMaterializer
  val cluster: Option[Cluster] = None
  val session: Option[Session] = None
  val producer: Option[KafkaProducer[String, String]] = None
  val consumer: Option[KafkaConsumer[String, String]] = None

  def getCurrentProgramWithValidDateTime(programId: String, startDateTime: Long, endDateTime: Long, programs: List[ProgramRevisionsByProgramIdRow]): ProgramRevisionsByProgramIdRow = {
    val validProgramsByDate: List[ProgramRevisionsByProgramIdRow] =
      programs.filter { programRow =>
        val startDateTimeCheck: Boolean = between(startDateTime, programRow.startDateTime, programRow.endDateTime)
        val endDateTimeCheck: Boolean = between(endDateTime, programRow.startDateTime, programRow.endDateTime)
        startDateTimeCheck && endDateTimeCheck
      }
    if (validProgramsByDate.isEmpty)
      programs.find { program => program.startDateTime == 0 && program.programId == programId } match {
        case Some(validProgram) => validProgram
        case None => throw new Exception("cant find a base program or any other program with that program id")
      }
    else {
      if (validProgramsByDate.size == 1) validProgramsByDate.headOption match {
        case Some(validProgram) => validProgram
        case None => throw new Exception("something else bad happened here...")
      }
      else throw new Exception("more than 1 program is valid... not processing till that is resolved")
    }
  }

  def prepare: Unit ={
    val getProgramPrepared = session.handle.prepare(s"select * from programs.program_revisions_by_program_id;")
    val getProgramStatement = getProgramPrepared.bind().setConsistencyLevel(ConsistencyLevel.ONE)
    val xx = session.handle.execute(getProgramStatement).toList
    println()
  }

  def getPrograms(programId: String = ""): List[ProgramRevisionsByProgramIdRow] = {
    val programIdQuery: String =
      if (programId == "") ""
      else s"where programId = '$programId'"
    session.executeSafe(s"select * from programs.program_revisions_by_program_id $programIdQuery;").toList.map { row: Row =>
      ProgramRevisionsByProgramIdRow(
        row.getString("programId"),
        row.getLong("startDateTime"),
        row.getLong("endDateTime"),
        row.getString("revisionId"),
        row.getString("name"),
        decode[Set[Level]](row.getString("levels")) match {
          case Right(tiers) => tiers
          case Left(ex) => throw ex
        }
      )
    }
  }

  def startCassandraCluster(implicit system: ActorSystem): Option[Cluster] =
    Try {
      if (AppConf.cassandraUsername != "" && AppConf.cassandraPassword != "") {
        Cluster.builder
          .addContactPoint(AppConf.cassandraHost)
          .withCredentials(AppConf.cassandraUsername, AppConf.cassandraPassword)
          .build
      }
      else {
        Cluster.builder
          .addContactPoint(AppConf.cassandraHost)
          .build
      }
    } match {
      case Success(openCluster) =>
        system.log.info("Cassandra Cluster - connected")
        Some(openCluster)
      case Failure(ex) =>
        system.log.error(s"Cassandra Cluster - $ex")
        None
    }

  def startCassandraSession(implicit system: ActorSystem, cluster: Option[Cluster]): Option[Session] =
    cluster match {
      case Some(openCluster) =>
        Try(openCluster.connect()) match {
          case Success(openSession) =>
            system.log.info("Cassandra Session - connected")
            Some(openSession)
          case Failure(ex) =>
            system.log.error(s"Cassandra Session - $ex")
            None
        }
      case None =>
        system.log.debug("Cassandra Session - no cluster provided")
        None
    }

  def startHttpService(route: Route)(implicit system: ActorSystem, ec: ExecutionContext): Future[Http.ServerBinding] = {
    val httpServiceHost: String = AppConf.httpHost
    val httpServicePort: Int = AppConf.httpPort
    val httpServerSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http().bind(interface = httpServiceHost, httpServicePort)
    val httpBinding: Future[Http.ServerBinding] =
      httpServerSource.to(Sink.foreach { connection: Http.IncomingConnection =>
        system.log.debug("Http Server - accepted new connection from {}", connection.remoteAddress)
        connection.handleWith(route)
      }).run()
    httpBinding.map { binding: Http.ServerBinding =>
      system.log.info(s"Http Server - is now online at http://$httpServiceHost:$httpServicePort")
      binding
    }
  }

  def statusCheck(pathName: String)(implicit externalId: String): Route =
    extractExecutionContext { implicit ec: ExecutionContext =>
      pathPrefix(pathName) {
        get {
          val uniqueId: String = UUID.randomUUID().toString
          val cassandraStatus: Future[Boolean] =
            Future {
              if (AppConf.cassandraEnabled) {
                (cluster, session) match {
                  case (Some(openCluster), Some(openSession)) =>
                    if (!openCluster.isClosed && !openSession.isClosed) true
                    else false
                  case _ => false
                }
              }
              else true
            }
          val kafkaProducerStatus: Future[Boolean] = Future(true)
          val kafkaConsumerStatus: Future[Boolean] =
            Future {
              if (AppConf.kafkaConsumerEnabled) {
                Try(consumer.handle.listTopics(Duration.ofMillis(2500))) match {
                  case Success(_) => true
                  case Failure(_) => false
                }
              }
              else true
            }
          val status: Future[(Boolean, Boolean, Boolean)] =
            for {
              cas <- cassandraStatus
              kafPro <- kafkaProducerStatus
              kafCon <- kafkaConsumerStatus
            } yield (cas, kafPro, kafCon)
          onComplete(status) {
            case Success(allStatus) =>
              allStatus match {
                case (true, true, true) =>
                  val status: Boolean = true
                  akkaLog.debug(pathName, logFlatten(logUniqueId(uniqueId), logExternalId, logStatus(status)): _*)
                  complete(StatusCodes.OK, StatusCheckResponse(pathName, status, uniqueId))
                case _ =>
                  val status: Boolean = false
                  akkaLog.debug(pathName, logFlatten(logUniqueId(uniqueId), logExternalId, logStatus(status)): _*)
                  complete(StatusCodes.InternalServerError, StatusCheckResponse(pathName, status, uniqueId))
              }
            case Failure(ex) => throw ex
          }
        }
      }
    }

  def startKafkaProducer(implicit system: ActorSystem): Option[KafkaProducer[String, String]] = {
    val producerProps: Properties = new Properties()
    producerProps.put("bootstrap.servers", s"${AppConf.kafkaHost}:${AppConf.kafkaPort}")
    producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    system.log.info("Kafka Producer - started")
    Some(new KafkaProducer[String, String](producerProps))
  }

  def startKafkaConsumer(consumerGroupId: String = UUID.randomUUID().toString, topics: List[String] = List.empty)(implicit system: ActorSystem, ec: ExecutionContext): Option[KafkaConsumer[String, String]] = {
    val consumerProps: Properties = new Properties()
    consumerProps.put("bootstrap.servers", s"${AppConf.kafkaHost}:${AppConf.kafkaPort}")
    consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    consumerProps.put("group.id", consumerGroupId)
    //TODO Make this configurable
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    val consumer: KafkaConsumer[String, String] =
      new KafkaConsumer[String, String](consumerProps)
    system.log.info("Kafka Consumer - started")
    consumer.subscribe(topics.asJavaCollection)
    Future {
      while (true) {
        val records: Iterable[ConsumerRecord[String, String]] =
          consumer.poll(1.second.toMillis).asScala
        records.foreach { msg: ConsumerRecord[String, String] =>
          system.eventStream.publish(KafkaMsg(msg.topic(), msg.value.mkString))
        }
      }
    }
    system.log.info("Kafka Consumer - polling")
    Some(consumer)
  }

}
