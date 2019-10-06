package onetoone.users

//Imports
import onetoone.servicecore.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.models.error.ErrorResponse
import onetoone.servicecore.models.programs.Tier
import onetoone.servicecore.models.wallets.ExpSummary
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
              session.handle.execute(s"select * from users.user_by_email where email = '${req.email}';").toList.headOption match {
                case Some(_) => throw new Exception("user already created")
                case None =>
                  val userId: String = UUID.randomUUID().toString
                  val walletId: String = UUID.randomUUID().toString
                  val accountId = ""

                  //if i have a program id create a new wallet for it else just skip the step
                  req.programId match {
                    case Some(pId) =>
                      programs.filter(_.name == pId).find(_.startDateTime == "default") match {
                        case Some(program) =>
                          program.tiers.toList.sortBy(_.level).headOption match {
                            case Some(tier) =>

                              session.executeSafe(s"insert into wallets.wallet_by_user_id (userId, walletId, programId, currentTier, currentPoints, lifetimePoints) values ('$userId', '$walletId', '${req.programId}', '${tier.name}', '${ExpSummary("default", 0).asJson.noSpaces}', '${ExpSummary("default", 0).asJson.noSpaces}')")


                            case None => throw new Exception("something else happened...?")
                          }
                        case None => throw new Exception("no default program found for this programId")
                      }
                    case None => akkaLog.debug("no program so im just not doing anything")
                  }

                  session.handle.execute(s"insert into users.user_by_user_id (userId, walletId) values ('$userId', '$walletId');").toList
                  session.handle.execute(s"insert into users.user_by_email (email, userId) values ('${req.email}', '$userId');").toList
                  complete(StatusCodes.Created, PostUser201(userId, accountId))
              }
//              if (alreadyCreatedUsers.isEmpty){
//                val userId: String = UUID.randomUUID().toString
//                val walletId: String = UUID.randomUUID().toString
//                //create a wallet first
//                val lowestTier: Tier = programs.flatMap{_.tiers}.sortBy(_.level).headOption.getOrElse(throw new Exception("no tier found"))
////                val pointBuckets: String = programs.flatMap{_.tiers}.flatMap(_.profiles.map{profile =>
////                  PointBucket(profile.pointBucket, 0)
////                }).toSet.asJson.noSpaces
//                val pointBuckets: String = ""
//                val accountId: String = ThreadLocalRandom.getRandomNumeric(24)
//                session.handle.execute(s"insert into accounts.accounts (accountId, walletId, programId, userType, name, userId) values ('$accountId', '$walletId', '${req.programId}', '${req.userType}', '${req.name}', '$userId');").toList
//                session.handle.execute(s"insert into wallets.wallet_by_wallet_id (walletId, programId, currentTier, currentPoints, lifetimePoints) values ('$walletId', '${req.programId}', '${lowestTier.name}', '$pointBuckets', '$pointBuckets');").toList
//                session.handle.execute(s"insert into wallets.wallet_by_user_id (userId, programId, walletId, currentTier, currentPoints, lifetimePoints) values ('$userId', '${req.programId}', '$walletId', '${lowestTier.name}', '$pointBuckets', '$pointBuckets');").toList
//                session.handle.execute(s"insert into wallets.last_modified (walletId, timestamp) values ('$walletId', now());").toList
//                session.handle.execute(s"insert into users.users (userId, walletId, email, createDateTime, lastActivityDateTime, userType) values ('$userId', 'none', '${req.email}', now(), now(), 'base');").toList
//                session.handle.execute(s"insert into users.users (userId, walletId, email, createDateTime, lastActivityDateTime, userType) values ('$userId', 'none', '${req.email}', now(), now(), 'base');").toList
//                session.handle.execute(s"insert into users.user_by_email (email, userId) values ('${req.email}', '$userId');").toList
//                complete(StatusCodes.Created, PostUser201(userId, accountId))
//              }
//              else throw new Exception("That username is already taken")
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
