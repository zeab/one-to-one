package onetoone.programs

//Imports

import com.datastax.driver.core.Row
import onetoone.programs.http.{PostProgramRequest, PutProgramRequest}
import onetoone.servicecore.service.ServiceCore
import org.apache.kafka.clients.producer.KafkaProducer
//Akka
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
//Datastax
import com.datastax.driver.core.Session
//Java
import java.util.UUID
//Circe
import io.circe.generic.AutoDerivation
import io.circe.syntax._

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]
  val producer: Option[KafkaProducer[String, String]]

  val programs: Route =
    pathPrefix("programs") {
      get {
        parameter("programId") { programId: String =>
          complete(StatusCodes.OK, getPrograms(programId))
        }
      } ~
        post {
          decodeRequest {
            entity(as[PostProgramRequest]) { req: PostProgramRequest =>
              val alreadyCreatedPrograms: List[Row] =
                session.executeSafe(s"select * from programs.program_revisions_by_program_id where programId = '${req.programId}';").toList
              val (startDateTime, endDateTime): (Long, Long) =
                (req.startDateTime, req.endDateTime) match {
                  case (Some(startDateTime), Some(endDateTime)) => (startDateTime, endDateTime)
                  case (None, Some(_)) => throw new Exception("start time missing")
                  case (Some(_), None) => throw new Exception("end time missing")
                  case (None, None) => (0L, 0L)
                }
              val revisionId: String = UUID.randomUUID().toString
              if (startDateTime == 0 && endDateTime == 0) {
                if (alreadyCreatedPrograms.exists(program => program.getLong("startDateTime") == 0 && program.getLong("endDateTime") == 0))
                  throw new Exception("cant create because a program base already exists")
                else {
                  session.executeSafe(s"insert into programs.program_revisions_by_program_id (programId, startDateTime, endDateTime, revisionId, name, levels, lastModified) values ('${req.programId}', $startDateTime, $endDateTime, '$revisionId', '${req.name}', '${req.levels.asJson.noSpaces}', ${System.currentTimeMillis()});")
                  complete(StatusCodes.Created)
                }
              }
              else {
                alreadyCreatedPrograms
                  .filterNot(programRow => programRow.getLong("startDateTime") == 0 && programRow.getLong("endDateTime") == 0)
                  .find{programRow: Row =>
                    val startDateTimeCheck: Boolean = between(startDateTime, programRow.getLong("startDateTime"), programRow.getLong("endDateTime"))
                    val endDateTimeCheck: Boolean = between(endDateTime, programRow.getLong("startDateTime"), programRow.getLong("endDateTime"))
                    startDateTimeCheck && endDateTimeCheck
                  } match {
                  case Some(_) => throw new Exception("there are other programs that are active during that time")
                  case None =>
                    session.executeSafe(s"insert into programs.program_revisions_by_program_id (programId, startDateTime, endDateTime, revisionId, name, levels, lastModified) values ('${req.programId}', $startDateTime, $endDateTime, '$revisionId', '${req.name}', '${req.levels.asJson.noSpaces}', ${System.currentTimeMillis()});")
                }
                complete(StatusCodes.Created)
              }
            }
          }
        } ~
        put {
          decodeRequest {
            entity(as[PutProgramRequest]) { req: PutProgramRequest =>
              complete(StatusCodes.Accepted)
            }
          }
        }
    }

  //1546300799000 <- end of 2018
  //1514811600000 <- start of 2018

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
