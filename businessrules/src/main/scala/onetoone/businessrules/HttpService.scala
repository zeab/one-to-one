package onetoone.businessrules

//Scala
import com.datastax.driver.core.Row
import io.circe.generic.AutoDerivation
import onetoone.servicecore.models.error.ErrorResponse
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

  //coupons
//  val wallets: Route =
//    pathPrefix("wallets") {
//      path("card"){
//        get{complete("1")} ~
//          post{
//            decodeRequest{
//              entity(as[PostWalletsCardRequest]){ req: PostWalletsCardRequest =>
//                session match {
//                  case Some(openSession) =>
//                    val user: List[Row] = openSession.execute(s"select * from users.user_ids where userId = '${req.userId}';").asScala.toList
//                    if(user.isEmpty) throw new Exception("UserId does not exist")
//                    else {
//                      val cardsAlreadyRegistered: List[Row] = openSession.execute(s"select * from wallets.user_id_by_card_number where cardNumber = '${req.cardNumber}';").asScala.toList
//                      if(cardsAlreadyRegistered.isEmpty) {
//                        openSession.execute(s"insert into wallets.user_id_by_card_number (cardNumber, userId) values ('${req.cardNumber}', '${req.userId}');")
//                        complete(StatusCodes.OK)
//                      }
//                      else throw new Exception("Card is already registered")
//                    }
//                  case None => throw NoSessionException()
//                }
//              }
//            }
//            complete("2")
//          } ~
//          put{complete("4")} ~
//          delete{complete("5")}
//      }~
//      get {
//        complete("3")
//      }
//    }

//  val users: Route =
//    pathPrefix("users") {
//      get {
//        parameter("username"){ username: String =>
//          session match {
//            case Some(openSession) =>
//              val user: List[Row] = openSession.execute(s"select * from users.usernames where username = '$username';").asScala.toList
//              user.headOption match {
//                case Some(row) =>
//                  complete(StatusCodes.OK, GetUser200(row.getString("userId")))
//                case None => complete(StatusCodes.NotFound, ErrorResponse("", "Member not found"))
//              }
//            case None => throw NoSessionException()
//          }
//        }
//      } ~
//        post {
//          decodeRequest {
//            entity(as[PostUserRequest]) { user: PostUserRequest =>
//              val userId: String =
//                session match {
//                  case Some(openSession) =>
//                    val allUsernameInSystem: List[Row] =
//                      openSession.execute(s"select * from users.usernames where username = '${user.username}';").asScala.toList
//                    if (allUsernameInSystem.isEmpty) {
//                      val userId: String = UUID.randomUUID().toString
//                      openSession.execute(s"insert into users.usernames (username, userId) values ('${user.username}', '$userId');")
//                      userId
//                    }
//                    else throw new Exception("That username is already taken")
//                  case None => throw NoSessionException()
//                }
//              complete(StatusCodes.Created, PostUser201(userId))
//            }
//          }
//        } ~
//        put {
//          //update the user's info
//          complete()
//        } ~
//        delete {
//          //remove the user from the database
//          complete()
//        }
//    }


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
