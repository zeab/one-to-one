package onetoone.servicecore.service

//Imports
import onetoone.servicecore.customexceptions.NoSessionException
//Datastax
import com.datastax.driver.core.{Cluster, Session}
//Kafka
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer

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
  }

}
