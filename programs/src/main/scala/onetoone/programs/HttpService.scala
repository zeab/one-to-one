package onetoone.programs

//Imports
import com.datastax.driver.core.Row
import onetoone.programs.http.{PostProgramRequest, PutProgramRequest}
import onetoone.servicecore.service.ServiceCore
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
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
                  val alreadyCreatedBaseProgram: List[Row] =
                    alreadyCreatedPrograms.filter(row => row.getString("startDateTime") == "base" && row.getString("endDateTime") == "base")
                  //i also need to check if the new program conflicts with any other program
                  if (alreadyCreatedBaseProgram.isEmpty) {
                    val startDateTime: String = req.startDateTime.getOrElse("base")
                    val endDateTime: String = req.endDateTime.getOrElse("base")
                    val revisionId: String = req.revisionId.getOrElse(UUID.randomUUID().toString)
                    session.executeSafe(s"insert into programs.program_revisions_by_program_id (programId, startDateTime, endDateTime, revisionId, name, levels, lastModified) values ('${req.programId}', '$startDateTime', '$endDateTime', '$revisionId', '${req.name}', '${req.levels.asJson.noSpaces}', '${System.currentTimeMillis()}');")
                    producer.handle.send(new ProducerRecord[String, String]("program-change", "a change happened..."))
                    producer.handle.flush()
                  }
                  else throw new Exception("cant create another base program... must update")
                  complete(StatusCodes.Created)
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
