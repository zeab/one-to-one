package onetoone.coupons

//Scala
import com.datastax.driver.core.Row
import io.circe.generic.AutoDerivation
import onetoone.servicecore.models.http.error.ErrorResponse
import onetoone.servicecore.service.ServiceCore

import scala.collection.JavaConverters._
//Imports
//Akka
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.datastax.driver.core.Session
import onetoone.servicecore.customexceptions.NoSessionException
//Java
import java.util.UUID
//Circe
//Slf4j
//Scala

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]

  val coupons: Route = ???

  def all: Route =
    logsAndMetrics {
      extractExternalId { implicit externalId: String =>
        handleExceptions(exceptionHandler) {
          handleRejections(rejectionHandler) {
            statusCheck("readiness") ~ statusCheck("liveness") ~ coupons
          }
        }
      }
    }

}
