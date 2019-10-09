package onetoone.wallets

//Imports
//Akka
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import onetoone.servicecore.service.ServiceShutdown
import onetoone.wallets.levelevaluator.LevelEvaluator
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
//Datastax
import com.datastax.driver.core.{Cluster, Session}
//Slf4j
import org.slf4j.{Logger, LoggerFactory}
//Scala
import scala.concurrent.{ExecutionContext, Future}

import onetoone.servicecore.encryption.Encryption._

object Wallets extends App with HttpService with ServiceShutdown {
  
  //Akka
  implicit val system: ActorSystem = ActorSystem("Wallets", ConfigFactory.load())
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
  override implicit val consumer: Option[KafkaConsumer[String, String]] = startKafkaConsumer("wallets", List("level-evaluation"))

  //Add the shutdown hooks
  shutdownHookThread

  val programs = getPrograms()
  val levelEvaluator: ActorRef = system.actorOf(Props(classOf[LevelEvaluator], programs, session))

}
