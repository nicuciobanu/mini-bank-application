package com.akka.bank.app

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.akka.bank.actors.Bank
import com.akka.bank.actors.Bank.Command
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.akka.bank.http.BankRoutes

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object BankApp {

  trait RootCommand
  case class RetrieveBankActor(replyTo: ActorRef[ActorRef[Command]]) extends RootCommand

  def startHttpServer(bank: ActorRef[Command])(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    val bankRoutes = new BankRoutes(bank)
    val routes = bankRoutes.routes

    val httpBidingFuture = Http().newServerAt("localhost", 8080).bind(routes)

    val logger = system.log

    httpBidingFuture.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        logger.info(s"Server online at http://${address.getHostString}:${address.getPort}")
      case Failure(ex) =>
        logger.error(s"Failed to bind HTTP server, because: $ex")
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val rootBehavior: Behavior[RootCommand] = Behaviors.setup { context =>
      val bankActor = context.spawn(Bank(), "bank")

      Behaviors.receiveMessage {
        case RetrieveBankActor(replyTo) =>
          replyTo ! bankActor
          Behaviors.same
      }
    }

    implicit val system = ActorSystem(rootBehavior, "BankSystem")
    implicit val timeout: Timeout = Timeout(5.seconds)
    implicit val ex: ExecutionContext = system.executionContext

    val bankActorFuture: Future[ActorRef[Command]] = system.ask(replyTo => RetrieveBankActor(replyTo))
    bankActorFuture.foreach(startHttpServer)
  }
}
