package onetoone.servicecore.directives

//Imports
import onetoone.servicecore.customexceptions.{CardAlreadyRegisteredException, NoSessionException, UserIdNotFoundException}
import onetoone.servicecore.models.error.ErrorResponse

import scala.util.{Failure, Success, Try}
//Akka
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
//Slf4j
import org.slf4j.Logger
//Circe
import io.circe.generic.AutoDerivation

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
        akkaLog.warn(ex.toString, logFlatten(logExceptionClass(ex.getClass.toString), logExternalId): _*)
        val exCause: String = Try(ex.getCause.toString) match {
          case Success(exCause) => exCause
          case Failure(_) => ""
        }
        complete(StatusCodes.InternalServerError, ErrorResponse(s"$exceptionPrefix-00", ex.toString, exCause))
    }

}
