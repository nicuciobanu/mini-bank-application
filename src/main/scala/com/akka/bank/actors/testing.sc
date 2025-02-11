//object CalculateBalance {
//
//  type Timestamp = Long
//  type IBAN = String
//
//  case class TransactionLog(instant: Timestamp, debtorIBAN: IBAN, creditorIBAN: IBAN, sum: BigDecimal)
//
//  def isInInterval(instant: Timestamp, from: Timestamp, to: Timestamp): Boolean = {
//    if(instant >= from || instant <= to) true else false
//  }
//
//  def calculateBalances(from: Timestamp, to: Timestamp, transactionLogs: Seq[TransactionLog]): Map[IBAN, BigDecimal] = {
//    val transInInterval =
//      transactionLogs
//        .filter(tr => isInInterval(tr.instant, from, to))
//        .map(tr =>
//          Map(tr.debtorIBAN -> -1 * tr.sum) ++ Map(tr.creditorIBAN -> tr.sum)
//        ).flatten.toMap.groupBy(_._1)
//        .map(tr => tr._1 -> tr._2.map(_._2))
//        .map(tr => tr._1 -> tr._2.sum)
//    transInInterval
//  }
//
//  def main(args: Array[String]): Unit = {
//    val transactionList = List(
//      TransactionLog(1001, "IBAN_01", "IBAN_02", 10.00),
//      TransactionLog(2002, "IBAN_02", "IBAN_03", 15.00),
//      TransactionLog(3003, "IBAN_03", "IBAN_01", 20.00),
//      TransactionLog(4004, "IBAN_03", "IBAN_02", 25.00),
//    )
//
//    // Should return Map(IBAN_01 -> 20.00, IBAN_02 -> 10.00, IBAN_03 -> -30.00))
//    calculateBalances(2000, 5000, transactionList)
//  }
//
//  def verifyConsistency(balanceMap: Map[String, BigDecimal]): Boolean = ???
//}

type Timestamp = Long
type IBAN = String

case class TransactionLog(instant: Timestamp, debtorIBAN: IBAN, creditorIBAN: IBAN, sum: BigDecimal)

def isInInterval(instant: Timestamp, from: Timestamp, to: Timestamp): Boolean = {
  if (instant >= from || instant <= to) true else false
}

def calculateBalances(from: Timestamp, to: Timestamp, transactionLogs: Seq[TransactionLog]): Map[IBAN, BigDecimal] = {
  val transInInterval =
    transactionLogs
      .filter(tr => isInInterval(tr.instant, from, to))
      .map(tr =>
        Map(tr.debtorIBAN -> -1 * tr.sum) ++ Map(tr.creditorIBAN -> tr.sum)
      ).flatten.toMap.groupBy(_._1)
      .map(tr => tr._1 -> tr._2.map(_._2))
      .map(tr => tr._1 -> tr._2.sum)
  transInInterval
}

val transactionList = List(
  TransactionLog(1001, "IBAN_01", "IBAN_02", 10.00),
  TransactionLog(2002, "IBAN_02", "IBAN_03", 15.00),
  TransactionLog(3003, "IBAN_03", "IBAN_01", 20.00),
  TransactionLog(4004, "IBAN_03", "IBAN_02", 25.00),

  // IBAN_01 - 10 + 20 = 10, IBAN_02 + 10 - 15 + 25 = 20, IBAN_03 + 15 - 20 - 25 =-30
)

// Should return Map(IBAN_01 -> 20.00, IBAN_02 -> 10.00, IBAN_03 -> -30.00))
val result = calculateBalances(1000, 5000, transactionList)

println(result)