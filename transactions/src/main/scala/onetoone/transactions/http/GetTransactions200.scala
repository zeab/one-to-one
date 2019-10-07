package onetoone.transactions.http

import com.datastax.driver.core.Row

case class GetTransactions200(transactions: List[String])
