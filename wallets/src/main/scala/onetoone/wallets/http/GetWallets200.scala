package onetoone.wallets.http

import onetoone.servicecore.models.wallets.Tank

case class GetWallets200(currentLevel: Int, currentTanks: Set[Tank], lifetimeTanks: Set[Tank])