package onetoone.programs

//Imports
import java.util.UUID

import onetoone.programs.http.PostProgramsRequest
import onetoone.servicecore.{Profile, Tier}
import onetoone.servicecore.service.ServiceShutdown
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

object Programs extends App with HttpService with ServiceShutdown {

  println(UUID.randomUUID())
  val x = PostProgramsRequest(
    "xxx", None, "us", List(Tier("white", 0, List(Profile("base", "base", "2.0"))))).asJson
  println(x)

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

  //Start Kafka
  override implicit val producer: Option[KafkaProducer[String, String]] = startKafkaProducer
  override implicit val consumer: Option[KafkaConsumer[String, String]] = startKafkaConsumer()

  //Shutdown the system
  shutdownHookThread

}
