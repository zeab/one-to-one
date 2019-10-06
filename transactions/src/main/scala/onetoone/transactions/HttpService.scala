package onetoone.transactions

//Imports
import akka.actor.ActorRef
import akka.util.Timeout
import com.datastax.driver.core.Row
import io.circe.syntax._
import io.circe.parser.decode
import onetoone.servicecore.PointBucket
import onetoone.servicecore.cassandra.ProgramRevisionRow
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
import akka.pattern.ask
import scala.concurrent.duration._

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]
  val programs: List[ProgramRevisionRow]
//  implicit val timeout = Timeout(5.second)
  val transactions: Route =
    pathPrefix("transactions") {
      get {
        parameter("userId", "date", "daysBack") { (userId, date, daysBack) =>
          val allDaysToLookFor: List[String] = getAllDates(daysBack.toInt, date)
          val allTransactions: List[String] =
            allDaysToLookFor.flatMap { date: String =>
              session.handle
                .execute(s"select * from transactions.transactions where userId = '$userId' and date = '$date';")
                .toList.map(_.toString)
            }
          complete(StatusCodes.OK, PostTransactions200(allTransactions))
        }
      } ~
        post {
          decodeRequest {
            entity(as[PostTransactionRequest]) { req: PostTransactionRequest =>
              val alreadyCreatedUser: List[Row] = session.executeSafe(s"select * from accounts.accounts where accountId = '${req.accountId}';").toList
              alreadyCreatedUser.headOption match {
                case Some(userRow) =>
                  val programId: String = userRow.getString("programId")
                  val accountId: String = userRow.getString("accountId")
                  val walletId: String = userRow.getString("walletId")
                  val userId: String = userRow.getString("userId")
                  val userType: String = userRow.getString("userType")

                  session.executeSafe(s"select * from transactions.transaction_by_transaction_id where transactionId = '${req.transactionId}';").toList.headOption match {
                    case Some(transaction) => throw new Exception("transaction already processed")
                    case None =>
                      session.executeSafe(s"select * form wallets.wallet_by_user_id where userId = '$userId' and programId = $programId;").toList.headOption match {
                        case Some(wallet) =>
                          val currentTier = wallet.getString("currentTier")
                          val currentPoints = wallet.getString("currentPoints")
                          val lifetimePoints = wallet.getString("lifetimePoints")

                          programs.find(program => program.programId == programId && program.startDateTime == "default") match {
                            case Some(p) =>
                              p.tiers.find(_.name == currentTier) match {
                                case Some(value) =>
                                  val x = value
                                  println()
                                case None => throw new Exception("cant find ither stuff")
                              }
                            case None => throw new Exception("cant find stuff")
                          }
                        case None => throw new Exception("cant find the wallet")
                      }
                  }




                  //have i processed this transaction before...
                  //if i have not
                  //
                case None => throw new Exception("can not find user")
              }



              //val alreadyCreatedUser = session.handle.execute(s"select * from accounts.accounts where accountId = '${req.accountId}'").toList
              if (alreadyCreatedUser.isEmpty) throw new Exception("account id does not exist")
              else{
                val programId: String = alreadyCreatedUser.head.getString("programId")
                val alreadyProcessedTransaction =
                  session.handle.execute(s"select * from transactions.transaction_by_transaction_id where transactionId = '${req.transactionId}';").toList
                if (alreadyProcessedTransaction.isEmpty){
                  val userId = alreadyCreatedUser.head.getString("userId")
                  val userType = alreadyCreatedUser.head.getString("userType")
                  session.handle.execute(s"insert into transactions.transaction_by_transaction_id (transactionId, userId, timestamp) values ('${req.transactionId}', '$userId', now());")
                  val userWallet = session.handle.execute(s"select * from wallets.wallet_by_user_id where userId = '$userId' and programId = '$programId';").toList
                  if (userWallet.isEmpty) throw new Exception("unable to find users wallet")
                  else{
                    val currentPoints = decode[Set[PointBucket]](userWallet.head.getString("currentPoints"))
                    currentPoints match {
                      case Right(points) =>
                        val updatedPoints = points.map{bucket =>
                          if(bucket.name == userType) PointBucket(bucket.name, bucket.amount + 100)
                          else bucket
                        }
                        session.handle.execute(s"insert into wallets.wallet_by_user_id (currentPoints) values ('$updatedPoints.asJson.noSpaces')")
                      case Left(ex) => throw ex
                    }
                  }
                  session.handle.execute(s"insert into transactions.transaction_by_user_id (transactionId, userId, timestamp, transaction) values ('${req.transactionId}', '$userId', now(), 'the transaction here...');")
                  val usersWallet = session.handle.execute("select * from wallets.wallet_by_user_id;").toList
                  usersWallet.headOption match {
                    case Some(wallet) =>
                      val currentTier = wallet.getString("currentTier")
                      decode[Set[PointBucket]](wallet.getString("currentPoints")) match {
                        case Right(value) =>
                          val currentProgram = programs.filter(_.programId == programId)
                          val t = currentProgram.head.tiers.filter(_.name == currentTier).head
                          val kkkk = t.profiles.filter(_.`type` == userType).head

                          //pull the bucket
                          //decode it
                          //find the bucket in there that i need
                          //update it
                          //write it back to the database
                          session.handle.execute("")

                          value
                        case Left(ex) => throw ex
                      }
                    case None => throw new Exception("cant find the user wallet")
                  }
                }
                else throw new Exception("transaction already processed")
              }
              complete()
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
