package onetoone.transactions

//Imports
import onetoone.servicecore.service.ServiceCore
import onetoone.transactions.http.{PostTransactionRequest, PostTransactions200}
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

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]

  val transactions: Route =
    pathPrefix("transactions") {
      get {
        parameter("userId", "date", "daysBack") { (userId, date, daysBack) =>
          val allDaysToLookFor: List[String] = getAllDates(daysBack.toInt, date)
          val allTransactions: List[String] =
            allDaysToLookFor.flatMap { date: String =>
              session.handle
                .execute(s"select * from transactions.transactions where userId = '$userId' and date = '$date';")
                .list.map(_.toString)
            }
          complete(StatusCodes.OK, PostTransactions200(allTransactions))
        }
      } ~
        post {
          decodeRequest {
            entity(as[PostTransactionRequest]) { req: PostTransactionRequest =>
              session.handle
                .execute(s"select * from wallets.user_id_by_card_number where cardNumber = '${req.cardNumber}';")
                .list.headOption match {
                case Some(userIdByCardNumberRow) =>
                  val userId: String = userIdByCardNumberRow.getString("userId")
                  session.handle
                    .execute(s"select * from transactions.ledger where userId = '$userId' and transactionId = '${req.transactionId}'")
                    .list.headOption match {
                    case Some(_) =>
                      complete(StatusCodes.NotAcceptable, "Already Processed Transaction")
                    case None =>
                      session.handle
                        .execute(s"insert into transactions.ledger (userId,transactionId) values ('$userId', '${req.transactionId}');")
                        .list
                      session.handle
                        .execute(s"insert into transactions.transactions (userId, date, timestamp, transactionId, amountInPennies) values ('$userId', '${req.timestamp}', now(), '${req.transactionId}', ${req.amountInPennies});")
                        .list
                      complete(StatusCodes.Accepted)
                  }
                case None => throw new Exception("no user id found for transaction")
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
            statusCheck("readiness") ~ statusCheck("liveness") ~ transactions
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
