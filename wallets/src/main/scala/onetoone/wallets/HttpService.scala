package onetoone.wallets

//Scala
import io.circe.generic.AutoDerivation
import onetoone.servicecore.customexceptions.{CardAlreadyRegisteredException, UserIdNotFoundException}
import onetoone.servicecore.service.ServiceCore
import onetoone.wallets.http.PostWalletsCardRequest
//Imports
//Akka
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.datastax.driver.core.Session
//Java
//Circe
//Slf4j
//Scala

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]

  //coupons
  val wallets: Route =
    pathPrefix("wallets") {
      path("card"){
        get{complete("1")} ~
          post{
            decodeRequest{
              entity(as[PostWalletsCardRequest]){ req: PostWalletsCardRequest =>
                session.handle
                  .execute(s"select * from users.user_ids where userId = '${req.userId}';")
                  .list.headOption match {
                  case Some(_) =>
                    session.handle
                      .execute(s"select * from wallets.user_id_by_card_number where cardNumber = '${req.cardNumber}';")
                      .list.headOption match {
                      case Some(_) => throw CardAlreadyRegisteredException()
                      case None =>
                        session.handle
                          .execute(s"insert into wallets.user_id_by_card_number (cardNumber, userId) values ('${req.cardNumber}', '${req.userId}');")
                        complete(StatusCodes.OK)
                    }
                  case None => throw UserIdNotFoundException()
                }
              }
            }
          } ~
          put{complete("4")} ~
          delete{complete("5")}
      }~
      get {
        complete("3")
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
