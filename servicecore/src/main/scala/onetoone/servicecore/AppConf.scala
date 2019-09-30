package onetoone.servicecore

//Imports
import com.typesafe.config.{Config, ConfigFactory}

trait AppConf {

  //Root Conf
  val appConf: Config = ConfigFactory.load()

  //Http Service
  val httpHost: String = appConf.getString("http.host")
  val httpPort: Int = appConf.getInt("http.port")

  //Env
  val envName: String = appConf.getString("env.name")

  //Cassandra
  val cassandraHost: String = appConf.getString("cassandra.host")
  val cassandraPort: String = appConf.getString("cassandra.port")
  val cassandraUsername: String = appConf.getString("cassandra.username")
  val cassandraPassword: String = appConf.getString("cassandra.password")
  val cassandraEnabled: Boolean = appConf.getBoolean("cassandra.enabled")

  //Kafka
  val kafkaHost: String = appConf.getString("kafka.host")
  val kafkaPort: String = appConf.getString("kafka.port")
  val kafkaConsumerEnabled: Boolean = appConf.getBoolean("kafka.consumer.enabled")
  val kafkaProducerEnabled: Boolean = appConf.getBoolean("kafka.producer.enabled")
}

object AppConf extends AppConf