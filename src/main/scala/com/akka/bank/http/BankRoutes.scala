package com.akka.bank.http

import akka.http.scaladsl.server.Directives._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import com.akka.bank.actors.Bank.Command
import com.akka.bank.actors.Bank.Command._
import com.akka.bank.actors.PersistenceBankAccount.{BankAccountBalanceUpdatedResponse, BankAccountCreateResponse, GetBankAccountResponse, PersistenceResponse}
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import com.akka.bank.http.Validation.{Validator, requiredString, validateEntity, validateMinimum, validateMinimumAbs, validateRequired}
import Validation._
import akka.http.scaladsl.server.Route
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._

import scala.concurrent.Future
import scala.concurrent.duration._

case class BankAccountCreateRequest(user: String, currency: String, balance: Long) {
  def toCommand(replyTo: ActorRef[PersistenceResponse]): Command =
    CreateBankAccount(user, currency, balance, replyTo)
}

object BankAccountCreateRequest {
  implicit val validator: Validator[BankAccountCreateRequest] = new Validator[BankAccountCreateRequest] {
    override def validate(request: BankAccountCreateRequest): ValidationResult[BankAccountCreateRequest] = {
      val userValidation = validateRequired(request.user, "user")
      val currencyValidation = validateRequired(request.currency, "currency")
      val balanceValidation = validateMinimum(request.balance, 0, "balance")
        .combine(validateMinimumAbs(request.balance, 1, "balance"))

      (userValidation, currencyValidation, balanceValidation).mapN(BankAccountCreateRequest.apply)
    }
  }
}

case class FailureResponse(msg: String)

case class BankAccountUpdateRequest(currency: String, amount: Long) {
  def toCommand(id: String, replyTo: ActorRef[PersistenceResponse]): Command =
    UpdateBalance(id, currency, amount, replyTo)
}

object BankAccountUpdateRequest {
  implicit val validator: Validator[BankAccountUpdateRequest] = new Validator[BankAccountUpdateRequest] {
    override def validate(request: BankAccountUpdateRequest): ValidationResult[BankAccountUpdateRequest] = {
      val currencyValidation = validateRequired(request.currency, "currency")
      val amountValidation = validateMinimumAbs(request.amount, 1, "amount")

      (currencyValidation, amountValidation).mapN(BankAccountUpdateRequest.apply)
    }
  }
}

class BankRoutes(bank: ActorRef[Command])(implicit system: ActorSystem[_]) {

  implicit val timeout: Timeout = Timeout(5.seconds)

  def createBankAccount(request: BankAccountCreateRequest): Future[PersistenceResponse] =
    bank.ask(replyTo => request.toCommand(replyTo))

  def getBankAccount(id: String): Future[PersistenceResponse] =
    bank.ask(replyTo => GetBankAccount(id, replyTo))

  def updateBankAccount(id: String, request: BankAccountUpdateRequest): Future[PersistenceResponse] =
    bank.ask(replyTo => request.toCommand(id, replyTo))

  def validateRequest[R: Validator](request: R)(routeIfValid: Route): Route =
    validateEntity(request) match {
      case Valid(_) =>
        routeIfValid
      case Invalid(failures) =>
        complete(StatusCodes.BadRequest, FailureResponse(failures.toList.map(_.errorMessage).mkString(", ")))
    }

  val routes =
    pathPrefix("bank") {
      /*
      * POST /bank/
      * - Payload: create request as json
      * Response:
      * - Location: /bank/uuid
      * */
      pathEndOrSingleSlash {
        post {
          entity(as[BankAccountCreateRequest]) { request =>
            validateRequest(request) {
              onSuccess(createBankAccount(request)) {
                case BankAccountCreateResponse(id) =>
                  respondWithHeader(Location(s"/bank/$id")) {
                    complete(StatusCodes.Created)
                  }
              }
            }
          }
        }
      } ~
        path(Segment) { id =>
          /*
                   * GET /bank/uuid
                   * Response:
                   * - 200 OK
                   * - Json Response
                   * */
          get {
            onSuccess(getBankAccount(id)) {
              case GetBankAccountResponse(Right(bankAccount)) =>
                complete(StatusCodes.OK, bankAccount)
              case GetBankAccountResponse(Left(error)) =>
                complete(StatusCodes.NotFound, FailureResponse(error.getMessage))
            }
          } ~
            /*
               * PUT /bank/uuid
               * - Payload: (amount, currency) as JSON
               * Response:
               * - 200 OK
               *   Payload: new bank details as JSON
               * - 404 NOT FOUND
               * - 400 BAD REQUEST - if something wrong
               * */
            put {
              entity(as[BankAccountUpdateRequest]) { request =>
                validateRequest(request) {
                  onSuccess(updateBankAccount(id, request)) {
                    case BankAccountBalanceUpdatedResponse(Right(bankAccount)) =>
                      complete(StatusCodes.OK, bankAccount)
                    case BankAccountBalanceUpdatedResponse(Left(error)) =>
                      complete(StatusCodes.NotFound, FailureResponse(error.getMessage))
                  }
                }
              }
            }
        }
    }
}
