package onetoone.transactions.http

case class PostTransactionRequest(
                                   transactionId: String,
                                   timestamp: String,
                                   cardNumber: String,
                                   amountInPennies: Int
                                 )
