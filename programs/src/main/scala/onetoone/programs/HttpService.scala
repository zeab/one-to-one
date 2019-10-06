package onetoone.programs

//Imports
import java.util.UUID

import com.datastax.driver.core.Row
import onetoone.programs.http.PostProgramRequest
import onetoone.servicecore.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.service.ServiceCore
//Scala
import scala.annotation.tailrec
//Akka
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
//Datastax
import com.datastax.driver.core.Session
//Java
import java.time.LocalDate
import java.time.format.DateTimeFormatter
//Circe
import io.circe.generic.AutoDerivation
import io.circe.syntax._

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]

  val programs: Route =
    pathPrefix("programs") {
      get {
        parameter("programId"){ programId: String =>
          complete(StatusCodes.OK, getPrograms(programId))
        }
      } ~
        post {
          decodeRequest {
            entity(as[PostProgramRequest]) { req: PostProgramRequest =>
              session.executeSafe(s"select * from programs.ledger where programId = '${req.programId}' and revisionId = '${req.revisionId}';").toList.headOption match{
                case Some(_) => throw new Exception("program already created")
                case None =>
                  val startDateTime: String = req.startDateTime.getOrElse("default")
                  val endDateTime: String = req.endDateTime.getOrElse("default")
                  val revisionId: String = req.revisionId.getOrElse(UUID.randomUUID().toString)
                  session.executeSafe(s"insert into programs.program_revisions_by_program_id (programId, startDateTime, endDateTime, revisionId, name, tiers) values ('${req.programId}', '$startDateTime', '$endDateTime', '$revisionId', '${req.name}', '${req.tiers.asJson.noSpaces}');")
                  session.executeSafe(s"insert into programs.ledger (programId, revisionId) values ('${req.programId}', '$revisionId');")
                  complete(StatusCodes.Created)
              }
            }
          }
        }
    }

  def all: Route =
    logsAndMetrics {
      extractExternalId { implicit externalId: String =>
        handleExceptions(exceptionHandler) {
          handleRejections(rejectionHandler) {
            statusCheck("readiness") ~ statusCheck("liveness") ~ programs
          }
        }
      }
    }

}
