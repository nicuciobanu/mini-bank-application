package com.akka.bank.http

import cats.data.ValidatedNel
import cats.implicits._

object Validation {

  // fields must be present
  trait Required[A] extends (A => Boolean)

  trait Minimum[A] extends ((A, Long) => Boolean)

  trait MinimumAbs[A] extends ((A, Long) => Boolean)

  // TC instances
  implicit val requiredString: Required[String] = _.nonEmpty
  implicit val minimumInt: Minimum[Int] = _ >= _
  implicit val minimumDouble: Minimum[Long] = _ >= _
  implicit val minimumIntAbs: MinimumAbs[Int] = Math.abs(_) >= _
  implicit val minimumLongAbs: MinimumAbs[Long] = Math.abs(_) >= _

  // usage
  def required[A](value: A)(implicit req: Required[A]): Boolean = req(value)

  def minimum[A](value: A, threshold: Long)(implicit min: Minimum[A]): Boolean = min(value, threshold)

  def minimumAbs[A](value: A, threshold: Long)(implicit min: MinimumAbs[A]): Boolean = min(value, threshold)

  // Validated
  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  // validation failures
  trait ValidationFailure {
    def errorMessage: String
  }

  case class EmptyField(fieldName: String) extends ValidationFailure {
    override def errorMessage = s"$fieldName is empty"
  }

  case class NegativeValue(fieldName: String) extends ValidationFailure {
    override def errorMessage = s"$fieldName is negative"
  }

  case class BelowMinimumValue(fieldName: String, min: Long) extends ValidationFailure {
    override def errorMessage = s"$fieldName is below the minimum threshold $min"
  }

  def validateMinimum[A: Minimum](value: A, threshold: Long, fieldName: String): ValidationResult[A] = {
    if (minimum(value, threshold)) value.validNel
    else if (threshold == 0) NegativeValue(fieldName).invalidNel
    else BelowMinimumValue(fieldName, threshold).invalidNel
  }

  def validateMinimumAbs[A: MinimumAbs](value: A, threshold: Long, fieldName: String): ValidationResult[A] = {
    if (minimumAbs(value, threshold)) value.validNel
    else BelowMinimumValue(fieldName, threshold).invalidNel
  }

  def validateRequired[A: Required](value: A, fieldName: String): ValidationResult[A] =
    if (required(value)) value.validNel
    else EmptyField(fieldName).invalidNel

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  def validateEntity[A](value: A)(implicit validator: Validator[A]): ValidationResult[A] =
    validator.validate(value)
}
