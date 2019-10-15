package onetoone.wallets

//Scala

import akka.http.scaladsl.model.StatusCodes
import io.circe.generic.AutoDerivation
import onetoone.servicecore.models.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.models.http.wallets.Tank
import onetoone.servicecore.service.ServiceCore
import onetoone.wallets.http.{GetWallets200, PostWalletRequest}
//Imports
//Akka
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.datastax.driver.core.Session
import io.circe.parser.decode
//Java
//Circe
//Slf4j
//Scala

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]
  val programs: List[ProgramRevisionsByProgramIdRow]

  val wallets: Route =
    pathPrefix("wallets") {
      get {
        parameter("userId", "programId") { (userId, programId) =>
          val (currentLevel, currentTanks, lifetimeTanks) =
            session.executeSafe(s"select * from wallets.wallet_by_user_id where userId = '$userId' and programId = '$programId';").toList.headOption match {
              case Some(walletRow) =>
                val currentTanks: Set[Tank] =
                decode[Set[Tank]](walletRow.getString("currentTanks")) match {
                  case Right(tanks) => tanks
                  case Left(ex) => throw ex
                }
                val lifetimeTanks: Set[Tank] =
                decode[Set[Tank]](walletRow.getString("lifetimeTanks")) match {
                  case Right(tanks) => tanks
                  case Left(ex) => throw ex
                }
                val currentLevel = walletRow.getInt("currentLevel")
                (currentLevel, currentTanks, lifetimeTanks)
              case None => throw new Exception("no user found using __ and __")
            }
          complete(StatusCodes.OK, GetWallets200(currentLevel, currentTanks, lifetimeTanks))
        }
      }
    }

  def all: Route =
    logsAndMetrics {
      extractExternalId { implicit externalId: String =>
        handleExceptions(exceptionHandler) {
          handleRejections(rejectionHandler) {
            statusCheck("readiness") ~ statusCheck("liveness") ~ wallets
          }
        }
      }
    }

}
