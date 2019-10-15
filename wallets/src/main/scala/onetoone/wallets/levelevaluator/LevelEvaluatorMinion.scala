package onetoone.wallets.levelevaluator

import akka.actor.Actor
import com.datastax.driver.core.Session
import onetoone.servicecore.models.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.models.kafka.LevelEvaluateEvent
import onetoone.servicecore.service.ServiceHandlers

class LevelEvaluatorMinion(programs: List[ProgramRevisionsByProgramIdRow], session: Option[Session]) extends Actor with ServiceHandlers {

  def receive: Receive = {
    case msg: LevelEvaluateEvent =>
      val currentEarnProfile =
        programs.find(program => program.startDateTime == 0 && program.programId == msg.programId)
          .getOrElse(throw new Exception("soeee"))
          .levels.find(_.level == msg.currentLevel)
          .getOrElse(throw new Exception("asddd"))

      session.executeSafe(s"select * from wallets.wallet_by_user_id where userId = '${msg.userId}' and programId = '${msg.programId}';").toList


      //process one message and see if i need to update that users wallet info about the their tier level...
      //how do i know what needs to happen
      //the transaction will be tagged so ill use that data and filter though the programs like i do now
      //that way ill evaluate the program at the time of the program... or that going too far...?
  }

}
