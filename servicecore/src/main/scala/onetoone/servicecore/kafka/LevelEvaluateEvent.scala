package onetoone.servicecore.kafka

//Imports
import onetoone.servicecore.models.http.wallets.Tank

case class LevelEvaluateEvent(
                               timestamp: Long,
                               programId: String,
                               userId: String,
                               currentLevel: Int,
                               currentTanks: Set[Tank],
                               lifetimeTanks: Set[Tank],
                               userType: String
                             )
