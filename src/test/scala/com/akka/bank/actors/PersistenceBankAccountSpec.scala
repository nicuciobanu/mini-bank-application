package com.akka.bank.actors

import akka.persistence.testkit.scaladsl.UnpersistentBehavior
import com.akka.bank.actors.Bank.Command._
import com.akka.bank.actors.Bank.Command
import com.akka.bank.actors.PersistenceBankAccount._
import org.scalatest.wordspec.AnyWordSpecLike

class PersistenceBankAccountSpec extends AnyWordSpecLike {

  private def onCreateBankAccount: UnpersistentBehavior.EventSourced[Command, PersistenceEvent, BankAccount] =
    UnpersistentBehavior.fromEventSourced(
      PersistenceBankAccount.apply("1"),
      BankAccount("1", "user1", "GBP", 100000))

  "PersistenceBankAccount" should {
    "Create bank account" in {
      onCreateBankAccount { (testKit, eventProbe, _) =>
        testKit.runAsk[PersistenceResponse](reply => CreateBankAccount("user1", "GBP", 200000, reply))
          .expectReply(BankAccountCreateResponse("1"))

        eventProbe.expectPersisted(BankAccountCreated(BankAccount("1", "user1", "GBP", 200000)))

        testKit.runAsk[PersistenceResponse](reply => Command.GetBankAccount("1", reply))
          .expectReply(GetBankAccountResponse(Right(BankAccount("1", "user1", "GBP", 200000))))
      }
    }

    "Update bank account - success case" in {
      onCreateBankAccount { (testKit, eventProbe, _) =>
        testKit.runAsk[PersistenceResponse](reply => UpdateBalance("user1", "GBP", 200000, reply))
          .expectReply(BankAccountBalanceUpdatedResponse(Right(BankAccount("1", "user1", "GBP", 300000))))

        eventProbe.expectPersisted(BalanceUpdated(300000))

        testKit.runAsk[PersistenceResponse](reply => Command.GetBankAccount("1", reply))
          .expectReply(GetBankAccountResponse(Right(BankAccount("1", "user1", "GBP", 300000))))
      }
    }

    "Update bank account - fail case, when balance is negative" in {
      onCreateBankAccount { (testKit, _, _) =>
        testKit.runAsk(reply => UpdateBalance("user1", "GBP", -200000, reply))
          .expectReply(BankAccountBalanceUpdatedResponse(Left(PersistenceFailureResponse("Balance is negative"))))
      }
    }
  }
}
