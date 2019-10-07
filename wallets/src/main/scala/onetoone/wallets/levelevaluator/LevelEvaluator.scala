package onetoone.wallets.levelevaluator

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import com.datastax.driver.core.Session
import onetoone.servicecore.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.kafka.{KafkaMsg, LevelEvaluateEvent}
import org.apache.kafka.clients.consumer.ConsumerRecord
import io.circe.parser.decode
import io.circe.generic.auto._

class LevelEvaluator(programs: List[ProgramRevisionsByProgramIdRow], session: Option[Session]) extends Actor{

  val minions: ActorRef = context.actorOf(Props(classOf[LevelEvaluatorMinion], programs, session))

  def receive: Receive = {
    case msg: KafkaMsg =>
      //what kinda safety do i want on this...
      val levelEvaluateEvent = decode[LevelEvaluateEvent](msg.msg).getOrElse(throw new Exception("decode error"))
      minions ! levelEvaluateEvent
  }

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[KafkaMsg])
  }

  override def postStop(): Unit = {
    minions ! PoisonPill
    context.system.eventStream.unsubscribe(self, classOf[KafkaMsg])
  }

}
