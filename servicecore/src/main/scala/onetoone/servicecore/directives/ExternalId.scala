package onetoone.servicecore.directives

//Imports
import net.logstash.logback.argument.StructuredArguments.value
import net.logstash.logback.argument.StructuredArgument
//Akka
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

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
