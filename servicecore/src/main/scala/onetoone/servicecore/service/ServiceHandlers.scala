package onetoone.servicecore.service

//Imports
import onetoone.servicecore.customexceptions
import onetoone.servicecore.customexceptions.{InvalidQueryException, NoSessionException}

import scala.util.{Failure, Success, Try}
//Datastax
import com.datastax.driver.core.{Cluster, ResultSet, Row, Session}
//Kafka
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
//Scala
import scala.collection.JavaConverters._

trait ServiceHandlers {

  implicit class KafkaProducerHandler(val producer: Option[KafkaProducer[String, String]]) {
    def handle: KafkaProducer[String, String] =
      producer match {
        case Some(kp) => kp
        case None => throw new Exception("No Kafka Producer")
      }
  }

  implicit class KafkaConsumerHandler(val consumer: Option[KafkaConsumer[String, String]]) {
    def handle: KafkaConsumer[String, String] =
      consumer match {
        case Some(kc) => kc
        case None => throw new Exception("No Kafka Consumer")
      }
  }

  implicit class ClusterHandler(val cluster: Option[Cluster]) {
    def handle: Cluster =
      cluster match {
        case Some(openCluster) => openCluster
        case None => throw new Exception("No Cassandra Cluster")
      }
  }

  implicit class SessionHandler(val session: Option[Session]) {
    def handle: Session =
      session match {
        case Some(openSession) => openSession
        case None => throw NoSessionException()
      }
    def executeSafe(query: String): ResultSet =
      Try(session.handle.execute(query)) match{
        case Success(resultSet) => resultSet
        case Failure(ex) => throw InvalidQueryException(query, ex)
      }
  }

  implicit class ResultSetConverter(val resultSet: ResultSet) {
    def toList: List[Row] = resultSet.asScala.toList
    def toMap[A](map: Row => A, list: List[Row] = resultSet.toList): List[A] = {
      for {
        row <- list
      } yield map(row)
    }
  }

}
