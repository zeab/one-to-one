package onetoone.servicecore.directives

//Imports
import onetoone.servicecore.models.error.ErrorResponse
//Akka
import akka.http.scaladsl.model.{RequestEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
//Slf4j
import org.slf4j.Logger
//Scala
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}
//Circe
import io.circe.generic.AutoDerivation

trait Rejections extends ExternalId with Marshallers with AutoDerivation {

  val akkaLog: Logger
  implicit val materializer: ActorMaterializer
  val rejectionPrefix: String = "01"

  //TODO Update more rejections here as the need arises (unlike exceptions each should be in their own handle block)
  implicit def rejectionHandler(implicit correlationId: String): RejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case ex: MalformedRequestContentRejection =>
          extractExecutionContext { implicit ec: ExecutionContext =>
            extractRequestEntity { req: RequestEntity =>
              onComplete(req.toStrict(5.second).map(_.data).map(_.utf8String)) {
                case Success(badBody) =>
                  akkaLog.warn(ex.toString, logExternalId)
                  complete(StatusCodes.BadRequest, ErrorResponse(s"$rejectionPrefix-01", ex.toString, badBody))
                case Failure(completeException) =>
                  akkaLog.warn(ex.toString, logExternalId)
                  complete(StatusCodes.InternalServerError, ErrorResponse(s"$rejectionPrefix-00", completeException.toString))
              }
            }
          }
      }
      .handle {
        case ex: Rejection =>
          akkaLog.warn(ex.toString, logExternalId)
          complete(StatusCodes.InternalServerError, ErrorResponse(s"$rejectionPrefix-00", ex.toString))
      }
      .result()

}
