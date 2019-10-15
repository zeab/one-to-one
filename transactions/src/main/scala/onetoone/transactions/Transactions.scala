package onetoone.transactions

//Imports
import java.util.UUID

import com.datastax.driver.core.PreparedStatement
import onetoone.servicecore.models.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.service.ServiceShutdown
import onetoone.transactions.http.PostTransactionRequest
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
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}

object Transactions extends App with HttpService with ServiceShutdown {

  val x = PostTransactionRequest(UUID.randomUUID().toString, 0, "926117809568517131379177", 1000, 840).asJson
  println(x)

  //Akka
  implicit val system: ActorSystem = ActorSystem("Transactions", ConfigFactory.load())
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

  val transactionByUserIdPrepared: PreparedStatement =
    session.handle.prepare(s"select * from transactions.transaction_by_user_id where userId = ?;")

  //Start Kafka
  override implicit val producer: Option[KafkaProducer[String, String]] = startKafkaProducer
  override implicit val consumer: Option[KafkaConsumer[String, String]] = startKafkaConsumer()

  //Shutdown the system
  shutdownHookThread

  val programs: List[ProgramRevisionsByProgramIdRow] = getPrograms()

}
