package onetoone.programs

//Imports
import java.util.UUID

import onetoone.programs.http.PostProgramsRequest
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
//        parameter("userId", "date", "daysBack") { (userId, date, daysBack) =>
//          val allDaysToLookFor: List[String] = getAllDates(daysBack.toInt, date)
//          val allTransactions: List[String] =
//            allDaysToLookFor.flatMap { date: String =>
//              session.handle
//                .execute(s"select * from transactions.transactions where userId = '$userId' and date = '$date';")
//                .list.map(_.toString)
//            }
//          complete(StatusCodes.OK, PostTransactions200(allTransactions))
        complete()
        }
      } ~
        post {
          decodeRequest {
            entity(as[PostProgramsRequest]) { req: PostProgramsRequest =>
              val revisionId: String = req.revisionId.getOrElse(UUID.randomUUID().toString)
              val startDateTime: String = req.startDateTime.getOrElse("default")
              val finalDateTime: String = req.finalDateTime.getOrElse("default")
              session.handle
                .execute(s"insert into programs.program_revision (programId, startDateTime, finalDateTime, revisionId, name, tiers) values ('${req.programId}', '$startDateTime', '$finalDateTime', '$revisionId', '${req.name}', '${req.tiers.asJson.noSpaces}');")
                .toList
              complete(StatusCodes.Created, req.programId)
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

  def getAllDates(daysBack: Int, date: String): List[String] = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @tailrec
    def worker(currentDay: Int, dates: List[String], lastDate: String): List[String] = {
      if (currentDay == 0) dates
      else {
        val currentDate: String = LocalDate.parse(lastDate, formatter).minusDays(1).format(formatter)
        worker(currentDay - 1, dates ++ List(currentDate), currentDate)
      }
    }

    val lastDate: String = LocalDate.parse(date, formatter).format(formatter)
    worker(daysBack - 1, List(lastDate), lastDate)
  }

}
