package onetoone.wallets

//Scala

import io.circe.generic.AutoDerivation
import onetoone.servicecore.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.service.ServiceCore
import onetoone.wallets.http.PostWalletRequest
//Imports
//Akka
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.datastax.driver.core.Session
//Java
//Circe
//Slf4j
//Scala

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]
  val programs: List[ProgramRevisionsByProgramIdRow]

  def all: Route =
    logsAndMetrics {
      extractExternalId { implicit externalId: String =>
        handleExceptions(exceptionHandler) {
          handleRejections(rejectionHandler) {
            statusCheck("readiness") ~ statusCheck("liveness")
          }
        }
      }
    }

}
