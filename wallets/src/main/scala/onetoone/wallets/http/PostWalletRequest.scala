package onetoone.wallets.http

case class PostWalletRequest(
                              walletId:String,
                              programId:String,
                              userId: String
                            )
