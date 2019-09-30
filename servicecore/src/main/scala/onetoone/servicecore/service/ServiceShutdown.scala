package onetoone.servicecore.service

//Imports
import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http

import scala.util.{Failure, Success, Try}
//Datastax
import com.datastax.driver.core.{Cluster, Session}
//Scala
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait ServiceShutdown {

  def shutdownHookThread(
                       implicit system: ActorSystem,
                       ec: ExecutionContext,
                       httpBinding: Future[Http.ServerBinding],
                       cluster: Option[Cluster] = None,
                       session: Option[Session] = None
                     ): Unit ={
    sys.addShutdownHook{
      Await.result(serviceShutdown, 30.second)
      system.log.info("ActorSystem - terminated... exiting")
      Await.result(system.terminate(), 30.second)
    }
    httpBinding.onFailure{ case _ => System.exit(1) }
  }

  private def serviceShutdown(
           implicit system: ActorSystem,
           ec: ExecutionContext,
           httpBinding: Future[Http.ServerBinding],
           cluster: Option[Cluster] = None,
           session: Option[Session] = None
         ): Future[Done] ={
    val httpService: Future[Done] =
      (httpBinding.value match {
        case Some(binding) =>
          binding match {
            case Success(bind) => bind.unbind()
            case Failure(ex) => Future(Done)
          }
        case None => Future(Done)
      }).map(_ => system.log.info("Http Server - offline")).map(_ => Done)
    val cassandraConnection: Future[Done] =
      Future {
        cluster match {
          case Some(openCluster) =>
            openCluster.close()
            system.log.info("Cassandra Cluster - is closed")
          case None => system.log.info("Cassandra Cluster - nothing to close")
        }
        session match {
          case Some(openSession) =>
            openSession.close()
            system.log.info("Cassandra Session - is closed")
          case None => system.log.info("Cassandra Session - nothing to close")
        }
        Done
      }
    val kafkaProducer: Future[Done] = Future(Done)
    val kafkaConsumer: Future[Done] = Future(Done)
    for {
      _ <- httpService
      _ <- cassandraConnection
      _ <- kafkaProducer
      _ <- kafkaConsumer
    } yield Done
  }

}
