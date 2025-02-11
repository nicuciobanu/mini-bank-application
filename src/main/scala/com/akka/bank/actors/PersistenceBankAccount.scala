package com.akka.bank.actors

import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object PersistenceBankAccount {

  import Bank.Command
  import Bank.Command._

  trait PersistenceEvent

  case class BankAccountCreated(bankAccount: BankAccount) extends PersistenceEvent

  case class BalanceUpdated(amount: Long) extends PersistenceEvent

  // state
  case class BankAccount(id: String, user: String, currency: String, balance: Long)

  // responses
  sealed trait PersistenceResponse

  case class BankAccountCreateResponse(id: String) extends PersistenceResponse

  case class BankAccountBalanceUpdatedResponse(maybeBankAccount: Either[PersistenceFailureResponse, BankAccount]) extends PersistenceResponse

  case class GetBankAccountResponse(maybeBankAccount: Either[PersistenceFailureResponse, BankAccount]) extends PersistenceResponse


  case class PersistenceFailureResponse(msg: String) extends Throwable {
    override def getMessage: String = msg
  }

  val commandHandler: (BankAccount, Command) => Effect[PersistenceEvent, BankAccount] = (state, command) => command match {
    case CreateBankAccount(user, currency, initialBalance, bank) =>
      val id = state.id
      Effect.persist(BankAccountCreated(BankAccount(id, user, currency, initialBalance)))
      .thenReply(bank)(_ => BankAccountCreateResponse(id))

    case UpdateBalance(_, _, amount, bank) =>
      val newBalance = state.balance + amount
      if(newBalance < 0)
        Effect.reply(bank)(BankAccountBalanceUpdatedResponse(Left(PersistenceFailureResponse("Balance is negative"))))
      else
        Effect.persist(BalanceUpdated(newBalance))
        .thenReply(bank)(newState => BankAccountBalanceUpdatedResponse(Right(newState.copy(balance = newBalance))))

    case GetBankAccount(_, bank) =>
      Effect.reply(bank)(GetBankAccountResponse(Right(state)))
  }

  val eventHandler: (BankAccount, PersistenceEvent) => BankAccount = (state, event) =>
    event match {
      case BankAccountCreated(bankAccount) => bankAccount
      case BalanceUpdated(amount) => state.copy(balance = amount)
    }

  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior[Command, PersistenceEvent, BankAccount](
      persistenceId = PersistenceId.ofUniqueId(id),
      emptyState = BankAccount(id, "", "", 0),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}