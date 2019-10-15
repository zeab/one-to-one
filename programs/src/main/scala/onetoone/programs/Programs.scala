package onetoone.programs

//Imports
import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

import onetoone.servicecore.service.{ServiceQueries, ServiceShutdown}
//Kafka
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
//Akka
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
//Datastax
import com.datastax.driver.core.{Cluster, Session}
//Slf4j
import org.slf4j.{Logger, LoggerFactory}
//Scala
import scala.concurrent.{ExecutionContext, Future}

import io.circe.generic.auto._
import io.circe.syntax._

object Programs extends App with HttpService with ServiceShutdown with ServiceQueries {

  import java.time.Instant
  import java.time.ZonedDateTime

  println(ZonedDateTime.now())
  val i = Instant.ofEpochSecond(System.currentTimeMillis())
  val z = ZonedDateTime.ofInstant(i, ZoneOffset.UTC)
  val a = ZonedDateTime.ofInstant(i, ZoneId.of("America/Los_Angeles")).toString
  val tt = ZonedDateTime.ofInstant(i, ZoneId.systemDefault())
  println(tt)

  //Akka
  implicit val system: ActorSystem = ActorSystem("Programs", ConfigFactory.load())
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  //Udp Connection
  val udpLog = LoggerFactory.getLogger("udp")

  //Akka Logger (because akka.slf4j does not support standard arguments and this does)
  val akkaLog: Logger = LoggerFactory.getLogger("akka")

  //Start the http service
  implicit val httpBinding: Future[Http.ServerBinding] = startHttpService(all)

  //Start Cassandra
  override implicit val cluster: Option[Cluster] = startCassandraCluster
  override implicit val session: Option[Session] = startCassandraSession

  //Prepare statments here...

  //Start Kafka
  override implicit val producer: Option[KafkaProducer[String, String]] = startKafkaProducer
  override implicit val consumer: Option[KafkaConsumer[String, String]] = startKafkaConsumer()

  //Shutdown the system
  shutdownHookThread

}
