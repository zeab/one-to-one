package onetoone.servicecore.models.http.transactions

case class Receipt(
                    transactionId: String,
                    transactionTimestamp: Long,
                    transactionLocation: String,
                    receiptLineItems: List[ReceiptLineItem],
                    grandTotal: Double
                  )
