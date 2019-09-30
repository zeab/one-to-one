package onetoone.servicecore.directives

//Imports
import onetoone.servicecore.AppConf
//Akka
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
//Slf4j
import net.logstash.logback.argument.StructuredArguments._
import org.slf4j.Logger

trait LoggingAndMetrics extends ExternalId {

  val akkaLog: Logger
  val udpLog: Logger

  def logsAndMetrics: Directive[Unit] =
    extractActorSystem.flatMap { system: ActorSystem =>
      extractRequestContext.flatMap { ctx: RequestContext =>
        val startTime: Long = System.currentTimeMillis()
        extractExternalId.flatMap { implicit externalId: String =>
          akkaLog.debug(s"${ctx.request.method.value} ${ctx.request.uri.path} - received", logExternalId)
          mapResponse { resp: HttpResponse =>
            val completionInMs: Long = System.currentTimeMillis() - startTime
            //They must be cast to Any because it throws an ambiguous reference in the logger because it cant tell if its an Object or a Any*
            akkaLog.debug(s"${ctx.request.method.value} ${ctx.request.uri.path} - completion", logFlatten(logExternalId, value("completionInMs", completionInMs)))
            udpLog.info(s"service.response.duration:$completionInMs|g|#namespace:${AppConf.envName},path:${ctx.request.uri.path},service:${system.name},method:${ctx.request.method.value}")
            resp
          }
        }
      }
    }

}
