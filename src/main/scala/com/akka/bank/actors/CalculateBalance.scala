package com.akka.bank.actors

object CalculateBalance {

  type Timestamp = Long
  type IBAN = String

  case class TransactionLog(instant: Timestamp, debtorIBAN: IBAN, creditorIBAN: IBAN, sum: BigDecimal)

  def isInInterval(instant: Timestamp, from: Timestamp, to: Timestamp): Boolean = {
    if (instant >= from || instant <= to) true else false
  }

//  def calculateBalances(from: Timestamp, to: Timestamp, transactionLogs: Seq[TransactionLog]): Map[IBAN, BigDecimal] =
//    transactionLogs.collect { case TransactionLog(instant, debtorIBAN, creditorIBAN, sum) if isInInterval(instant, from, to) => (debtorIBAN -> sum) }.toMap

  def calculateBalances(from: Timestamp, to: Timestamp, transactionLogs: Seq[TransactionLog]): Map[IBAN, BigDecimal] = {
    val transInInterval =
      transactionLogs
        .filter(tr => isInInterval(tr.instant, from, to))
        .map( tr =>
          Map(tr.debtorIBAN -> -1 * tr.sum) ++ Map(tr.creditorIBAN -> tr.sum)
        ).flatten.toMap.groupBy(_._1)
        .map(tr => tr._1 -> tr._2.map(_._2))
        .map(tr => tr._1 -> tr._2.sum)
    transInInterval
  }


  def combine[A, B, C](f: A => B, g: B => C): A => C = {
    a: A => g(f(a))
  }


  def main(args: Array[String]): Unit = {
    val transactionList = List(
      TransactionLog(1001, "IBAN_01", "IBAN_02", 10.00),
      TransactionLog(2002, "IBAN_02", "IBAN_03", 15.00),
      TransactionLog(3003, "IBAN_03", "IBAN_01", 20.00),
      TransactionLog(4004, "IBAN_03", "IBAN_02", 25.00),
    )

    // Should return Map(IBAN_01 -> 20.00, IBAN_02 -> 10.00, IBAN_03 -> -30.00))
    calculateBalances(2000, 5000, transactionList)
  }
}
