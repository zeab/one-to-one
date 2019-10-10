package onetoone.servicecore.models.http.transactions

case class ReceiptLineItem(
                            item: String,
                            qty: Double,
                            itemCost: Double,
                            totalCost: Double,
                            tax: Double,
                            tenders: List[Tender],
                            discounts: List[Discount]
                          )
