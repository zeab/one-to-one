package onetoone.transactions.http

case class PostTransactionRequest(
                                   transactionId: String,
                                   timestamp: Long,
                                   accountId: String,
                                   amountInBase: Int,
                                   currencyCode:Int
                                 )
