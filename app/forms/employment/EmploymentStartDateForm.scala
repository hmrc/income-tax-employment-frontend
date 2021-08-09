/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package forms.employment

import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import models.employment.EmploymentDate
import play.api.data.{Form, FormError}
import play.api.data.Forms.mapping
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints._
import play.api.mvc.Results.BadRequest

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Try

object EmploymentStartDateForm {

  val startDateDay: String = "amount-day"
  val startDateMonth: String = "amount-month"
  val startDateYear: String = "amount-year"

  def dayEmpty: Constraint[String] = nonEmpty("employment.employmentStartDate.error.incompleteDay")
  def monthEmpty: Constraint[String] = nonEmpty("employment.employmentStartDate.error.incompleteMonth")
  def yearEmpty: Constraint[String] = nonEmpty("employment.employmentStartDate.error.incompleteYear")
  def isDayNumeric: Constraint[String] = pattern("\\d+".r, error = "employment.employmentStartDate.error.incorrectDay" )
  def isMonthNumeric: Constraint[String] = pattern("\\d+".r, error = "employment.employmentStartDate.error.incorrectMonth" )
  def isYearNumeric: Constraint[String] = pattern("\\d+".r, error = "employment.employmentStartDate.error.incorrectYear" )

  def employmentStartDateForm: Form[EmploymentDate] = Form(
    mapping(
      startDateDay -> trimmedText.verifying(dayEmpty andThen isDayNumeric) ,
      startDateMonth -> trimmedText.verifying(monthEmpty andThen isMonthNumeric),
      startDateYear -> trimmedText.verifying(yearEmpty andThen isYearNumeric)
    )(EmploymentDate.apply)(EmploymentDate.unapply)
  )

  def fifthAprilDate(taxYear: Int): LocalDate = LocalDate.of(taxYear, 4, 5)

  def verifyNewDate(date: EmploymentDate, taxYear: Int): Either[FormError, LocalDate] = {
    val newDate: Either[Throwable, LocalDate] = Try(LocalDate.of(date.amountYear.toInt, date.amountMonth.toInt, date.amountDay.toInt)).toEither
    newDate match {
      case Right(date) =>
        (date.isAfter(LocalDate.now()), date.isBefore(fifthAprilDate(taxYear))) match {
          case (true, _) => Left(FormError("amount-day", "employment.employmentStartDate.error.notInPast"))
          case (_, false) => Left(FormError("amount-day", "employment.employmentStartDate.error.tooRecent", Seq(taxYear.toString)))
          case _ => Right(date)
        }
      case Left(_) =>
        Left(FormError("amount-day", "employment.employmentStartDate.error.incorrect"))
    }
  }

//      .verifying("employment.employmentStartDate.error.totallyEmpty", data => {
//        (data.amountYear.nonEmpty, data.amountMonth.nonEmpty, data.amountDay.nonEmpty) match {
//          case (false, false, false) => false
//          case _ => true
//        }
//      })

////      .verifying("employment.employmentStartDate.error.incompleteDay", _.amountDay.nonEmpty)
//      .verifying("employment.employmentStartDate.error.incompleteDay", data => {
//        (data.amountYear.nonEmpty, data.amountMonth.nonEmpty, data.amountDay.nonEmpty) match {
//          case (false, false, false) => true
//          case (_, _, false) => false
//          case _ => true
//        }})
////      .verifying("employment.employmentStartDate.error.incompleteMonth", _.amountMonth.nonEmpty)
//      .verifying("employment.employmentStartDate.error.incompleteMonth",  data => {
//        (data.amountYear.nonEmpty, data.amountMonth.nonEmpty, data.amountDay.nonEmpty) match {
//          case (false, false, false) => true
//          case (_, false, _) => false
//          case _ => true
//        }})
////      .verifying("employment.employmentStartDate.error.incompleteYear", _.amountYear.nonEmpty)
//      .verifying("employment.employmentStartDate.error.incompleteYear",  data => {
//        (data.amountYear.nonEmpty, data.amountMonth.nonEmpty, data.amountDay.nonEmpty) match {
//          case (false, false, false) => true
//          case (false, _, _) => false
//          case _ => true
//        }})
//  )
}
