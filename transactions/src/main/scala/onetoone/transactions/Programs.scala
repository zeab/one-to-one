package onetoone.transactions

//Imports
import akka.actor.Actor

class Programs extends Actor {

  def list(programs:List[String] = List.empty): Receive = {
    case Start =>
      val allPrograms: List[String] = List.empty
      context.become(list(allPrograms))
    case _ =>
      //do i have a
  }

  def receive: Receive = list()

  override def preStart(): Unit = {
    self ! Start
  }

  case object Start
}


//on start up