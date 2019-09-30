package onetoone.servicecore.directives

//Imports
import net.logstash.logback.argument.{StructuredArgument, StructuredArguments}
import onetoone.servicecore.models.error.ErrorResponse
import onetoone.servicecore.models.statuscheck.StatusCheckResponse
//Akka
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, RequestEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
//Slf4j
import net.logstash.logback.argument.StructuredArguments.value
import org.slf4j.Logger
//Scala
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}
//Circe
import io.circe.generic.AutoDerivation
//Java
import java.util.UUID
//Datastax
import com.datastax.driver.core.{Cluster, Session}

trait ExternalId {

  private val externalIdKey: String = "external-id"

  def extractExternalId: Directive1[String] =
    extractRequestContext.flatMap { ctx: RequestContext =>
      val externalId: String =
        ctx.request.headers
          .find(_.name().toLowerCase == externalIdKey)
          .map(_.value())
          .getOrElse(s"no-${externalIdKey}-found")
      provide(externalId)
    }

  def logExternalId(implicit id: String): StructuredArgument = value(externalIdKey, id)

}
