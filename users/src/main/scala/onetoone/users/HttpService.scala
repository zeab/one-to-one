package onetoone.users

//Imports
import onetoone.servicecore.models.error.ErrorResponse
import onetoone.servicecore.service.ServiceCore
import onetoone.users.http.{GetUser200, PostUser201, PostUserRequest}
//Datastax
import com.datastax.driver.core.{Row, Session}
//Circe
import io.circe.generic.AutoDerivation
//Akka
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
//Java
import java.util.UUID

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]
  val users: Route =
    pathPrefix("users") {
      get {
//        parameter("username") { username: String =>
//          session.handle
//            .execute(s"select * from users.usernames where username = '$username';").list
//            .headOption match {
//            case Some(row) => complete(StatusCodes.OK, GetUser200(row.getString("userId")))
//            case None => complete(StatusCodes.NotFound, ErrorResponse("", "Member not found"))
//          }
//        }
        complete()
      } ~
        post {
          decodeRequest {
            entity(as[PostUserRequest]) { req: PostUserRequest =>
              val alreadyCreatedUsers: List[Row] =
                session.handle.execute(s"select * from users.user_by_email where email = '${req.email}';").list
              if (alreadyCreatedUsers.isEmpty){
                val userId: String = UUID.randomUUID().toString
                session.handle.execute(s"insert into users.user_by_email (email, userId) values ('${req.email}', '$userId');").list
                session.handle.execute(s"insert into users.users (userId, walletId, email, createDateTime, lastActivityDateTime, userType) values ('$userId', 'none', '${req.email}', now(), now(), 'base');").list
                complete(StatusCodes.Created, PostUser201(userId))
              }
              else throw new Exception("That username is already taken")
            }
          }
        } ~
        put {
          //update the user's info
          complete()
        } ~
        delete {
          //remove the user from the database
          complete()
        }
    }

  def all: Route =
    logsAndMetrics {
      extractExternalId { implicit externalId: String =>
        handleExceptions(exceptionHandler) {
          handleRejections(rejectionHandler) {
            statusCheck("readiness") ~ statusCheck("liveness") ~ users
          }
        }
      }
    }

}
