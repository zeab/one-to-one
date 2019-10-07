package onetoone.transactions

//Imports
import onetoone.servicecore.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.models.programs.EarnProfile
import onetoone.servicecore.models.wallets.TankSummary
import onetoone.servicecore.service.ServiceCore
import onetoone.transactions.http.PostTransactionRequest
//Scala
//Akka
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
//Datastax
import com.datastax.driver.core.Session
//Java
//Circe
import io.circe.generic.AutoDerivation
import io.circe.parser.decode
import io.circe.syntax._

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]
  val programs: List[ProgramRevisionsByProgramIdRow]

  val transactions: Route =
    pathPrefix("transactions") {
      get{
        complete()
      } ~
      post{
        decodeRequest{
          entity(as[PostTransactionRequest]){ req: PostTransactionRequest =>
            //select * from accounts.account_by_account_id where accountId = '$accountId'
            session.executeSafe(s"select * from accounts.account_by_account_id where accountId = '${req.accountId}';").toList.headOption match {
              case Some(accountRow) =>
                val userId = accountRow.getString("userId")
                val programId = accountRow.getString("programId")
                val userType = accountRow.getString("userType")
                //select * from wallets.wallet_by_user_id where userId = '$userId' and programId = '$programId'
                session.executeSafe(s"select * from wallets.wallet_by_user_id where userId = '$userId' and programId = '$programId';").toList.headOption match {
                  case Some(walletRow) =>
                    val currentLevel = walletRow.getInt("currentLevel")
                    val currentTanks = decode[Set[TankSummary]](walletRow.getString("currentTanks")) match {
                      case Right(tank) => tank
                      case Left(ex) => throw ex
                    }
                    val lifetimeTanks = decode[Set[TankSummary]](walletRow.getString("lifetimeTanks")) match {
                      case Right(tank) => tank
                      case Left(ex) => throw ex
                    }
                    val currentEarnProfiles = programs.find(_.startDateTime == "base").getOrElse(throw new Exception("soeee")).levels.find(_.level == currentLevel).getOrElse(throw new Exception("asddd")).earnProfiles.filter(_.userType == userType)

                    def calculatePoints(earnProfiles:Set[EarnProfile], tanks: Set[TankSummary]): String ={
                      earnProfiles.flatMap{profile =>
                        tanks.filter(_.name == profile.tank).map{tank =>
                          TankSummary(tank.points + (req.amountInBase * profile.earnRate).toInt, tank.name)
                        }
                      }.asJson.noSpaces
                    }

                    val updatedCurrentTanks: String = calculatePoints(currentEarnProfiles, currentTanks)
                    val updatedLifetimeTanks: String = calculatePoints(currentEarnProfiles, lifetimeTanks)

                    session.executeSafe(s"UPDATE wallets.wallet_by_user_id SET currentTanks = '${updatedCurrentTanks}' , lifetimeTanks = '$updatedLifetimeTanks' WHERE userId = '$userId' and programId = '$programId';")

                    //get the active program (base) and then get the level info based on the current user level info
                    //take my current points and my lifetime points and do the math and write it back into the database
                    complete()
                  case None => throw new Exception("cant find wallet")
                }
              case None => throw new Exception("no account found")
            }
          }
        }
      }
    }
//    pathPrefix("transactions") {
//      get {
//        parameter("userId", "date", "daysBack") { (userId, date, daysBack) =>
//          val allDaysToLookFor: List[String] = getAllDates(daysBack.toInt, date)
//          val allTransactions: List[String] =
//            allDaysToLookFor.flatMap { date: String =>
//              session.handle
//                .execute(s"select * from transactions.transactions where userId = '$userId' and date = '$date';")
//                .toList.map(_.toString)
//            }
//          complete(StatusCodes.OK, PostTransactions200(allTransactions))
//        }
//      } ~
//        post {
//          decodeRequest {
//            entity(as[PostTransactionRequest]) { req: PostTransactionRequest =>
//              val alreadyCreatedUser: List[Row] = session.executeSafe(s"select * from accounts.accounts where accountId = '${req.accountId}';").toList
//              alreadyCreatedUser.headOption match {
//                case Some(userRow) =>
//                  val programId: String = userRow.getString("programId")
//                  val accountId: String = userRow.getString("accountId")
//                  val walletId: String = userRow.getString("walletId")
//                  val userId: String = userRow.getString("userId")
//                  val userType: String = userRow.getString("userType")
//
//                  session.executeSafe(s"select * from transactions.transaction_by_transaction_id where transactionId = '${req.transactionId}';").toList.headOption match {
//                    case Some(transaction) => throw new Exception("transaction already processed")
//                    case None =>
//                      session.executeSafe(s"select * form wallets.wallet_by_user_id where userId = '$userId' and programId = $programId;").toList.headOption match {
//                        case Some(wallet) =>
//                          val currentTier = wallet.getString("currentTier")
//                          val currentPoints = wallet.getString("currentPoints")
//                          val lifetimePoints = wallet.getString("lifetimePoints")
//
//                          programs.find(program => program.programId == programId && program.startDateTime == "default") match {
//                            case Some(p) =>
//                              p.tiers.find(_.name == currentTier) match {
//                                case Some(value) =>
//                                  val x = value
//                                  println()
//                                case None => throw new Exception("cant find ither stuff")
//                              }
//                            case None => throw new Exception("cant find stuff")
//                          }
//                        case None => throw new Exception("cant find the wallet")
//                      }
//                  }
//
//
//
//
//                  //have i processed this transaction before...
//                  //if i have not
//                  //
//                case None => throw new Exception("can not find user")
//              }
//
//
//
//              //val alreadyCreatedUser = session.handle.execute(s"select * from accounts.accounts where accountId = '${req.accountId}'").toList
//              if (alreadyCreatedUser.isEmpty) throw new Exception("account id does not exist")
//              else{
//                val programId: String = alreadyCreatedUser.head.getString("programId")
//                val alreadyProcessedTransaction =
//                  session.handle.execute(s"select * from transactions.transaction_by_transaction_id where transactionId = '${req.transactionId}';").toList
//                if (alreadyProcessedTransaction.isEmpty){
//                  val userId = alreadyCreatedUser.head.getString("userId")
//                  val userType = alreadyCreatedUser.head.getString("userType")
//                  session.handle.execute(s"insert into transactions.transaction_by_transaction_id (transactionId, userId, timestamp) values ('${req.transactionId}', '$userId', now());")
//                  val userWallet = session.handle.execute(s"select * from wallets.wallet_by_user_id where userId = '$userId' and programId = '$programId';").toList
//                  if (userWallet.isEmpty) throw new Exception("unable to find users wallet")
//                  else{
//                    val currentPoints = decode[Set[PointBucket]](userWallet.head.getString("currentPoints"))
//                    currentPoints match {
//                      case Right(points) =>
//                        val updatedPoints = points.map{bucket =>
//                          if(bucket.name == userType) PointBucket(bucket.name, bucket.amount + 100)
//                          else bucket
//                        }
//                        session.handle.execute(s"insert into wallets.wallet_by_user_id (currentPoints) values ('$updatedPoints.asJson.noSpaces')")
//                      case Left(ex) => throw ex
//                    }
//                  }
//                  session.handle.execute(s"insert into transactions.transaction_by_user_id (transactionId, userId, timestamp, transaction) values ('${req.transactionId}', '$userId', now(), 'the transaction here...');")
//                  val usersWallet = session.handle.execute("select * from wallets.wallet_by_user_id;").toList
//                  usersWallet.headOption match {
//                    case Some(wallet) =>
//                      val currentTier = wallet.getString("currentTier")
//                      decode[Set[PointBucket]](wallet.getString("currentPoints")) match {
//                        case Right(value) =>
//                          val currentProgram = programs.filter(_.programId == programId)
//                          val t = currentProgram.head.tiers.filter(_.name == currentTier).head
//                          val kkkk = t.profiles.filter(_.`type` == userType).head
//
//                          //pull the bucket
//                          //decode it
//                          //find the bucket in there that i need
//                          //update it
//                          //write it back to the database
//                          session.handle.execute("")
//
//                          value
//                        case Left(ex) => throw ex
//                      }
//                    case None => throw new Exception("cant find the user wallet")
//                  }
//                }
//                else throw new Exception("transaction already processed")
//              }
//              complete()
//            }
//          }
//        }
//    }

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

}
