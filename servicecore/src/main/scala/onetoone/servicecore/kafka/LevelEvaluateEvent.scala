package onetoone.servicecore.kafka

//Imports
import onetoone.servicecore.models.wallets.Tank

case class LevelEvaluateEvent(
                               timestamp: String,
                               programId: String,
                               userId: String,
                               currentLevel: Int,
                               currentTanks: Set[Tank],
                               lifetimeTanks: Set[Tank],
                               userType: String
                             )