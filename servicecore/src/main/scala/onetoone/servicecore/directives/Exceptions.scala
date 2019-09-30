package onetoone.servicecore.directives

//Imports
import io.circe.generic.AutoDerivation
import onetoone.servicecore.customexceptions.{CardAlreadyRegisteredException, NoSessionException, UserIdNotFoundException}
import onetoone.servicecore.models.error.ErrorResponse
//Akka
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
//Slf4j
import org.slf4j.Logger

trait Exceptions extends ExternalId with Marshallers with AutoDerivation {

  val akkaLog: Logger
  val exceptionPrefix: String = "00"

  //TODO Update more exceptions here as the need arises
  implicit def exceptionHandler(implicit externalId: String): ExceptionHandler =
    ExceptionHandler {
      case ex: CardAlreadyRegisteredException =>
        akkaLog.warn(ex.toString, logExternalId)
        complete(StatusCodes.NotAcceptable, ErrorResponse(s"$exceptionPrefix-04", ex.toString))
      case ex: UserIdNotFoundException =>
        akkaLog.warn(ex.toString, logExternalId)
        complete(StatusCodes.NotFound, ErrorResponse(s"$exceptionPrefix-03", ex.toString))
      case ex: NoSessionException =>
        akkaLog.warn(ex.toString, logExternalId)
        complete(StatusCodes.InternalServerError, ErrorResponse(s"$exceptionPrefix-02", ex.toString))
      case ex: RuntimeException =>
        akkaLog.warn(ex.toString, logExternalId)
        complete(StatusCodes.InternalServerError, ErrorResponse(s"$exceptionPrefix-01", ex.toString))
      case ex: Throwable =>
        akkaLog.warn(ex.toString, logExternalId)
        complete(StatusCodes.InternalServerError, ErrorResponse(s"$exceptionPrefix-00", ex.toString, ex.getClass.toString))
    }

}
