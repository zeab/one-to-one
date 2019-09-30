package onetoone.servicecore.directives

//Imports
import onetoone.servicecore.service.LoggingHandles
//Akka
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

trait ExternalId extends LoggingHandles {

  def extractExternalId: Directive1[String] =
    extractRequestContext.flatMap { ctx: RequestContext =>
      val externalId: String =
        ctx.request.headers
          .find(_.name().toLowerCase == "external-id")
          .map(_.value())
          .getOrElse(s"no-external-id-found")
      provide(externalId)
    }

}
