package onetoone.wallets

//Scala
import com.datastax.driver.core.Row
import io.circe.generic.AutoDerivation
import onetoone.servicecore.{PointBucket, Tier}
import onetoone.servicecore.cassandra.ProgramRevisionRow
import onetoone.servicecore.customexceptions.{CardAlreadyRegisteredException, UserIdNotFoundException}
import onetoone.servicecore.service.ServiceCore
import onetoone.wallets.http.{PostPointsRequest, PostWalletRequest, PostWalletsCardRequest}
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
import io.circe.parser.decode
import io.circe.syntax._

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]
  val programs: List[ProgramRevisionRow]

  //Feels like we need to move this off into it own service... but its fine for now...
  val points: Route =
    pathPrefix("points"){
      post{
        decodeRequest{
          entity(as[PostPointsRequest]){ req: PostPointsRequest =>
            val usersWallet = session.handle.execute(s"select * from wallets.wallet_by_user_id where userId = '${req.userId}';").toList
            usersWallet.headOption match {
              case Some(wallet) =>
                val programId = wallet.getString("programId")
                val walletId = wallet.getString("walletId")
                val updatedCurrentPoints = updatePoints(wallet, req.pointsToAdd, "currentPoints")
                val updatedLifetimePoints = updatePoints(wallet, req.pointsToAdd, "lifetimePoints")
                session.handle.execute(s"update wallets.wallet_by_user_id set currentPoints = '${updatedCurrentPoints.asJson.noSpaces}', lifetimePoints = '${updatedLifetimePoints.asJson.noSpaces}' where userId = '${req.userId}' and programId = '$programId';").toList
                session.handle.execute(s"update wallets.wallet_by_wallet_id set currentPoints = '${updatedCurrentPoints.asJson.noSpaces}', lifetimePoints = '${updatedLifetimePoints.asJson.noSpaces}' where walletId = '$walletId' and programId = '$programId';").toList
                session.handle.execute(s"insert into wallets.last_modified (walletId, timestamp) values ('$walletId', now());").toList
              case None => throw new Exception("cant find the users wallet")
            }
            complete()
          }
        }
      }
    }

  val wallets: Route =
    pathPrefix("wallets") {
      post{
        decodeRequest{
          entity(as[PostWalletRequest]){ req: PostWalletRequest =>
            val lowestTier: Tier = programs.flatMap{_.tiers}.sortBy(_.level).headOption.getOrElse(throw new Exception("no tier found"))
            val pointBuckets: String = programs.flatMap{_.tiers}.flatMap(_.profiles.map{profile =>
              s"""{"${profile.pointBucket}": 0}"""
            }).mkString("[", ",", "]")
            session.handle
              .execute(s"insert into wallets.wallet_by_wallet_id (walletId, programId, currentTier, currentPoints, lifetimePoints) values ('${req.walletId}', '${req.programId}', '${lowestTier.name}', '$pointBuckets', '$pointBuckets');")
              .toList
            session.handle
              .execute(s"insert into wallets.wallet_by_user_id (programId, currentTier, currentPoints, lifetimePoints) values ('${req.userId}', '${req.programId}', '${lowestTier.name}', '$pointBuckets', '$pointBuckets');")
              .toList
            session.handle
              .execute(s"insert into wallets.last_modified (walletId, timestamp) values ('${req.walletId}', now());")
              .toList
            complete()
          }
        }
      }
      //      path("card"){
//        get{complete("1")} ~
//          post{
//            decodeRequest{
//              entity(as[PostWalletsCardRequest]){ req: PostWalletsCardRequest =>
//                session.handle
//                  .execute(s"select * from users.user_ids where userId = '${req.userId}';")
//                  .list.headOption match {
//                  case Some(_) =>
//                    session.handle
//                      .execute(s"select * from wallets.user_id_by_card_number where cardNumber = '${req.cardNumber}';")
//                      .list.headOption match {
//                      case Some(_) => throw CardAlreadyRegisteredException()
//                      case None =>
//                        session.handle
//                          .execute(s"insert into wallets.user_id_by_card_number (cardNumber, userId) values ('${req.cardNumber}', '${req.userId}');")
//                        complete(StatusCodes.OK)
//                    }
//                  case None => throw UserIdNotFoundException()
//                }
//              }
//            }
//          } ~
//          put{complete("4")} ~
//          delete{complete("5")}
//      }~
//      get {
//        complete("3")
//      }
    }

  def all: Route =
    logsAndMetrics {
      extractExternalId { implicit externalId: String =>
        handleExceptions(exceptionHandler) {
          handleRejections(rejectionHandler) {
            statusCheck("readiness") ~ statusCheck("liveness") ~ points
          }
        }
      }
    }

  private def updatePoints(wallet: Row, pointsToAdd: Set[PointBucket], cassandraKey: String): Set[PointBucket] ={
    decode[Set[PointBucket]](wallet.getString(cassandraKey)) match {
      case Right(points) =>
        points.map{bucket =>
          pointsToAdd.find(_.name == bucket.name) match {
            case Some(specificBucket) =>
              PointBucket(bucket.name, bucket.amount + specificBucket.amount)
            case None => bucket
          }
        }
      case Left(ex) => throw ex
    }


  }

}
