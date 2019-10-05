package onetoone.transactions.http

case class PostTransactionRequest(
                                   transactionId: String,
                                   timestamp: String,
                                   accountId: String,
                                   amountInBase: Int,
                                   currencyCode:Int
                                 )
