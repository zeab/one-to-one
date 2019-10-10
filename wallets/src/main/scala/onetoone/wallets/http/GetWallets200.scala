package onetoone.wallets.http

import onetoone.servicecore.models.http.wallets.Tank

case class GetWallets200(currentLevel: Int, currentTanks: Set[Tank], lifetimeTanks: Set[Tank])