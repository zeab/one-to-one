package onetoone.transactions

//Imports

import akka.http.scaladsl.model.StatusCodes
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
      get {
        complete()
      } ~
        post {
          decodeRequest {
            entity(as[PostTransactionRequest]) { req: PostTransactionRequest =>
              session.executeSafe(s"select * from accounts.account_by_account_id where accountId = '${req.accountId}';").toList.headOption match {
                case Some(accountRow) =>
                  val userId: String = accountRow.getString("userId")
                  val programId: String = accountRow.getString("programId")
                  val userType: String = accountRow.getString("userType")
                  session.executeSafe(s"select * from wallets.wallet_by_user_id where userId = '$userId' and programId = '$programId';").toList.headOption match {
                    case Some(walletRow) =>
                      val currentLevel: Int = walletRow.getInt("currentLevel")
                      val currentTanks: Set[TankSummary] = decode[Set[TankSummary]](walletRow.getString("currentTanks")) match {
                        case Right(tank) => tank
                        case Left(ex) => throw ex
                      }
                      val lifetimeTanks: Set[TankSummary] = decode[Set[TankSummary]](walletRow.getString("lifetimeTanks")) match {
                        case Right(tank) => tank
                        case Left(ex) => throw ex
                      }
                      val currentEarnProfiles: Set[EarnProfile] =
                        programs.find(program => program.startDateTime == "base" && program.programId == programId).getOrElse(throw new Exception("soeee")).levels.find(_.level == currentLevel).getOrElse(throw new Exception("asddd")).earnProfiles.filter(_.userType == userType)

                      def calculatePoints(earnProfiles: Set[EarnProfile], tanks: Set[TankSummary]): String = {
                        earnProfiles.flatMap { profile: EarnProfile =>
                          tanks.map { tank: TankSummary =>
                            if (tank.name == profile.tank) TankSummary(tank.points + (req.amountInBase * profile.earnRate).toInt, tank.name)
                            else tank
                          }
                        }.asJson.noSpaces
                      }

                      val updatedCurrentTanks: String = calculatePoints(currentEarnProfiles, currentTanks)
                      val updatedLifetimeTanks: String = calculatePoints(currentEarnProfiles, lifetimeTanks)
                      session.executeSafe(s"UPDATE wallets.wallet_by_user_id SET currentTanks = '${updatedCurrentTanks}' , lifetimeTanks = '$updatedLifetimeTanks' WHERE userId = '$userId' and programId = '$programId';")
                      complete(StatusCodes.OK)
                    case None => throw new Exception("cant find wallet")
                  }
                case None => throw new Exception("no account found")
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

}
