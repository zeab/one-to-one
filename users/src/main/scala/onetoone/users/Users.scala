package onetoone.users

import io.circe.parser.decode
//Imports
import com.datastax.driver.core.Row
import onetoone.servicecore.Tier
import onetoone.servicecore.cassandra.ProgramRevisionRow
import onetoone.servicecore.service.ServiceShutdown
import onetoone.users.http.PostUserRequest
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

object Users extends App with HttpService with ServiceShutdown {

  //Akka
  implicit val system: ActorSystem = ActorSystem("Users", ConfigFactory.load())
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

  //Add the shutdown hooks
  shutdownHookThread

  val programs =
    session.handle
      .execute("select * from programs.program_revision;")
      .toMap{programRow: Row =>
        ProgramRevisionRow(
          programRow.getString("programId"),
          programRow.getString("name"),
          decode[List[Tier]](programRow.getString("tiers")) match {
            case Right(tier)=> tier
            case Left(ex) => throw ex
          },
          programRow.getString("startDateTime"),
          programRow.getString("finalDateTime")
        )
      }

}
