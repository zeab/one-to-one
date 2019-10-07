package onetoone.users

//Imports
import onetoone.servicecore.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.models.error.ErrorResponse
import onetoone.servicecore.models.programs.Level
import onetoone.servicecore.models.wallets.TankSummary
import onetoone.servicecore.service.ServiceCore
import onetoone.servicecore.util.ThreadLocalRandom
import onetoone.users.http.{GetUser200, GetUserInfo200, PostUser201, PostUserInfoRequest, PostUserRequest}
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
import io.circe.syntax._

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]
  val programs: List[ProgramRevisionsByProgramIdRow]

  val users: Route =
    pathPrefix("users") {
      path("info"){
        get{
          parameter("userId"){userId =>
            val response =
              session.executeSafe(s"select * from users.user_info_by_user_id where userId = '$userId';").toList.headOption match {
                case Some(userInfoRow) =>
                  val birthday: String = userInfoRow.getString("birthday")
                  val languagePreference: String = userInfoRow.getString("languagePreference")
                  val contactInformation: String = userInfoRow.getString("contactInformation")
                  val contactPreference: String = userInfoRow.getString("contactPreference")
                  GetUserInfo200(userId, birthday, languagePreference, contactInformation, contactPreference)
                case None => throw new Exception("cant find user info")
              }
            complete(StatusCodes.OK, response)
          }
        }~
        put{
          decodeRequest{
            entity(as[PostUserInfoRequest]){ req: PostUserInfoRequest =>
              session.executeSafe(s"insert into users.user_info_by_user_id (userId, birthday, languagePreference, contactInformation, contactPreference) values ('${req.userId}', '${req.birthday}', '${req.languagePreference}', '${req.contactInformation}', '${req.contactPreference}');")
              complete(StatusCodes.Created)
            }
          }
        }
      } ~
      get {
        complete()
      } ~
        post {
          decodeRequest {
            entity(as[PostUserRequest]) { req: PostUserRequest =>
              val userId: String = UUID.randomUUID().toString
              val walletId: String = UUID.randomUUID().toString
              val accountId: String = UUID.randomUUID().toString
              val tank: String =
                programs.find(_.startDateTime == "base").getOrElse(throw new Exception(""))
                  .levels.flatMap(_.earnProfiles.map(earnProfile => TankSummary(0, earnProfile.tank))).asJson.noSpaces
              val currentLevel: Int =
                programs.find(_.startDateTime == "base").getOrElse(throw new Exception(""))
                  .levels.toList.sortBy(_.level).headOption.getOrElse(throw new Exception(""))
                  .level
              //Maybe move to wallets...? or hook up a kafka message that fires when this happens...
              session.handle.execute(s"insert into wallets.wallet_by_user_id (userId, programId, walletId, currentLevel, currentTanks, lifetimeTanks) values ('$userId', '${req.programId}','$walletId', $currentLevel, '$tank', '$tank');").toList

              //Maybe move to accounts
              session.handle.execute(s"insert into accounts.account_by_account_id (accountId, programId, userId, userType) values ('$accountId', '${req.programId}','$userId', '${req.userType}');").toList

              //Actually deal wit the user tables
              session.handle.execute(s"insert into users.user_by_user_id (userId, walletId, userType) values ('$userId', '$walletId', '${req.userType}');").toList
              session.handle.execute(s"insert into users.user_by_email (email, userId) values ('{${req.email}}', '$userId');").toList
              complete(StatusCodes.Created, (userId, accountId))
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
