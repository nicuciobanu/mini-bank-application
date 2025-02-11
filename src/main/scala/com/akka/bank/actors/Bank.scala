package com.akka.bank.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import PersistenceBankAccount.{BankAccountBalanceUpdatedResponse, GetBankAccountResponse}

import java.util.UUID

object Bank {

  import PersistenceBankAccount.PersistenceResponse
  import Command._

  // events
  sealed trait BankEvent

  case class BankAccountCreated(id: String) extends BankEvent

  // state
  case class State(accounts: Map[String, ActorRef[Command]])

  // commands = messages
  sealed trait Command

  object Command {
    case class CreateBankAccount(user: String, currency: String, initialBalance: Long, replyTo: ActorRef[PersistenceResponse]) extends Command

    case class UpdateBalance(id: String, currency: String, amount: Long, replyTo: ActorRef[PersistenceResponse]) extends Command

    case class GetBankAccount(id: String, replyTo: ActorRef[PersistenceResponse]) extends Command
  }

  // command handler
  def commandHandler(context: ActorContext[Command]): (State, Command) => Effect[BankEvent, State] = (state, command) =>
    command match {
      case createCmd @ CreateBankAccount(_, _, _, _) =>
        val id = UUID.randomUUID().toString
        val newBankAccount = context.spawn(PersistenceBankAccount(id), id)
        Effect.persist(BankAccountCreated(id))
        .thenReply(newBankAccount)(_ => createCmd)

      case updateCmd @ UpdateBalance(id, _, _, replyTo) =>
        state.accounts.get(id) match {
          case Some(account) =>
            Effect.reply(account)(updateCmd)
          case None =>
            Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(Left(throw new Exception(s"No bank account with id: $id"))))
        }

      case getCmd @ GetBankAccount(id, replyTo) =>
        state.accounts.get(id) match {
          case Some(account) =>
            Effect.reply(account)(getCmd)
          case None =>
            Effect.reply(replyTo)(GetBankAccountResponse(Left(throw new Exception(s"No bank account with id: $id"))))
        }
    }

  // event handler
  def eventHandler(context: ActorContext[Command]): (State, BankEvent) => State = (state, event) =>
    event match {
      case BankAccountCreated(id) =>
        val account = context.child(id)
          .getOrElse(context.spawn(PersistenceBankAccount(id), id))
          .asInstanceOf[ActorRef[Command]]

        state.copy(state.accounts + (id -> account))
    }

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    EventSourcedBehavior[Command, BankEvent, State](
      persistenceId = PersistenceId.ofUniqueId("bank"),
      emptyState = State(Map()),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context)
    )
  }
}
