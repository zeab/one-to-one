package onetoone.servicecore.service

import com.datastax.driver.core.{ConsistencyLevel, Session}
import onetoone.servicecore.service.ServiceHandlers._

trait ServiceQueries {

  implicit val session: Option[Session]

  def prepare: Unit ={
    val getProgramPrepared = session.handle.prepare(s"select * from programs.program_revisions_by_program_id;")
    val getProgramStatement = getProgramPrepared.bind().setConsistencyLevel(ConsistencyLevel.ONE)
    val xx = session.handle.execute(getProgramStatement).toList
    println()
  }


}
