package onetoone.transactions

//Imports

import akka.http.scaladsl.model.StatusCodes
import onetoone.servicecore.cassandra.ProgramRevisionsByProgramIdRow
import onetoone.servicecore.kafka.LevelEvaluateEvent
import onetoone.servicecore.models.programs.EarnProfile
import onetoone.servicecore.models.wallets.Tank
import onetoone.servicecore.service.ServiceCore
import onetoone.transactions.http.{GetTransactions200, PostTransactionRequest}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
//Scala
//Akka
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
//Datastax
import com.datastax.driver.core.Session
//Java
//Circe
import io.circe.generic.AutoDerivation
import io.circe.parser.decode
import io.circe.syntax._

trait HttpService extends ServiceCore with AutoDerivation {

  val session: Option[Session]
  val programs: List[ProgramRevisionsByProgramIdRow]
  val producer: Option[KafkaProducer[String, String]]

  val transactions: Route =
    pathPrefix("transactions") {
      get {
        parameter("userId", "afterDateTime"){ (userId, afterDateTime) =>
          val transactions = session.executeSafe(s"select * from transactions.transaction_by_user_id where userId = '$userId';").toList.map(_.toString)
          complete(StatusCodes.OK, GetTransactions200(transactions))
        }
      } ~
        post {
          decodeRequest {
            entity(as[PostTransactionRequest]) { req: PostTransactionRequest =>
              session.executeSafe(s"select * from transactions.transaction_by_transaction_id where transactionId = '${req.transactionId}';").toList.headOption match {
                case Some(_) => throw new Exception("already processed transaction")
                case None =>
                  session.executeSafe(s"select * from accounts.account_by_account_id where accountId = '${req.accountId}';").toList.headOption match {
                    case Some(accountRow) =>
                      val userId: String = accountRow.getString("userId")
                      val programId: String = accountRow.getString("programId")
                      val userType: String = accountRow.getString("userType")
                      session.executeSafe(s"select * from wallets.wallet_by_user_id where userId = '$userId' and programId = '$programId';").toList.headOption match {
                        case Some(walletRow) =>
                          val currentLevel: Int = walletRow.getInt("currentLevel")
                          val currentTanks: Set[Tank] = decode[Set[Tank]](walletRow.getString("currentTanks")) match {
                            case Right(tank) => tank
                            case Left(ex) => throw ex
                          }
                          val lifetimeTanks: Set[Tank] = decode[Set[Tank]](walletRow.getString("lifetimeTanks")) match {
                            case Right(tank) => tank
                            case Left(ex) => throw ex
                          }

                          //use the program and look for the date of the transaction to process the request
                          val currentEarnProfiles: Set[EarnProfile] =
                            getCurrentProgramWithValidDateTime(programId, req.timestamp, req.timestamp, programs)
                              .levels.find(_.level == currentLevel).getOrElse(throw new Exception("cant find level..."))
                              .earnProfiles.filter(_.userType == userType)

                          def calculatePoints(earnProfiles: Set[EarnProfile], tanks: Set[Tank]): Set[Tank] = {
                            earnProfiles.flatMap { profile: EarnProfile =>
                              if (tanks.exists(_.name == profile.tank))
                                tanks.map { tank: Tank =>
                                  if (tank.name == profile.tank) Tank(tank.points + (req.amountInBase * profile.earnRate).toInt, tank.name)
                                  else tank
                                }
                              else tanks ++ Set(Tank((req.amountInBase * profile.earnRate).toInt, profile.tank))
                            }
                          }

                          val updatedCurrentTanks: Set[Tank] = calculatePoints(currentEarnProfiles, currentTanks)
                          val updatedLifetimeTanks: Set[Tank] = calculatePoints(currentEarnProfiles, lifetimeTanks)
                          val timestamp = System.currentTimeMillis()
                          session.executeSafe(s"UPDATE wallets.wallet_by_user_id SET currentTanks = '${updatedCurrentTanks.asJson.noSpaces}' , lifetimeTanks = '${updatedLifetimeTanks.asJson.noSpaces}' WHERE userId = '$userId' and programId = '$programId';")
                          session.executeSafe(s"insert into transactions.transaction_by_transaction_id (transactionId, userId, timestamp, transaction) values ('${req.transactionId}', '$userId', $timestamp, '${req.asJson.noSpaces}');")
                          session.executeSafe(s"insert into transactions.transaction_by_user_id (userId, transactionId, timestamp, transaction) values ('$userId', '${req.transactionId}', $timestamp, '${req.asJson.noSpaces}');")
                          val levelEvaluateEvent = LevelEvaluateEvent(req.timestamp, programId, userId, currentLevel, updatedCurrentTanks, updatedLifetimeTanks, userType).asJson.noSpaces
                          producer.handle.send(new ProducerRecord[String, String]("level-evaluation", levelEvaluateEvent))
                          producer.handle.flush()
                          complete(StatusCodes.OK)
                        case None => throw new Exception("cant find wallet")
                      }
                    case None => throw new Exception("no account found")
                  }
              }
            }
          }
        }
    }

  def all: Route =
    logsAndMetrics {
      extractExternalId { implicit externalId: String =>
        handleExceptions(exceptionHandler) {
          handleRejections(rejectionHandler) {
            statusCheck("readiness") ~ statusCheck("liveness") ~ transactions
          }
        }
      }
    }

}
