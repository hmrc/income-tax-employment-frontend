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
import models.employment.EmploymentDate
import play.api.data.Forms.mapping
import play.api.data.{Form, FormError}
import java.time.LocalDate

import utils.ViewUtils

import scala.util.Try

object EmploymentDateForm {

  val startDate = "employmentStartDate"
  val leaveDate = "employmentLeaveDate"

  val day: String = "amount-day"
  val month: String = "amount-month"
  val year: String = "amount-year"

  def employmentStartDateForm: Form[EmploymentDate] =
    Form(
      mapping(
        day -> trimmedText,
        month -> trimmedText,
        year -> trimmedText
      )(EmploymentDate.apply)(EmploymentDate.unapply)
    )

  val april = 4
  val startDay = 6

  def sixthAprilDate(taxYear: Int): LocalDate = LocalDate.of(taxYear, april, startDay)
  def fifthAprilDate(taxYear: Int): LocalDate = LocalDate.of(taxYear, april, startDay-1)

  object tooLongAgo {
    val day = 1
    val month = 1
    val year = 1900
  }

  val tooLongAgoDate: LocalDate = LocalDate.of(tooLongAgo.year, tooLongAgo.month, tooLongAgo.day)

  def areDatesEmpty(date: EmploymentDate, isAgent: Boolean, key: String): Seq[FormError] = {
    val agentCheck: String = if (isAgent) "agent" else "individual"
    (date.amountDay.isEmpty, date.amountMonth.isEmpty, date.amountYear.isEmpty) match{
      case (true, true, true) => Seq(FormError(s"emptyAll", s"employment.$key.error.incompleteAll.$agentCheck"))
      case (true, true, false) => Seq(FormError("emptyDayMonth", s"employment.$key.error.incompleteDayMonth.$agentCheck"))
      case (true, false, true) => Seq(FormError("emptyDayYear", s"employment.$key.error.incompleteDayYear.$agentCheck"))
      case (false, true, true) => Seq(FormError("emptyMonthYear", s"employment.$key.error.incompleteMonthYear.$agentCheck"))
      case (false, false, true) => Seq(FormError("emptyYear", s"employment.$key.error.incompleteYear.$agentCheck"))
      case (false, true, false) => Seq(FormError("emptyMonth", s"employment.$key.error.incompleteMonth.$agentCheck"))
      case (true, false, false) => Seq(FormError("emptyDay", s"employment.$key.error.incompleteDay.$agentCheck"))
      case (false, false, false) => Seq()
    }
  }

  trait DateValidationRequest

  case object StartDateValidationRequest extends DateValidationRequest
  case class LeaveDateValidationRequest(startDate: LocalDate) extends DateValidationRequest

  def verifyStartDate(date: EmploymentDate, taxYear: Int, isAgent: Boolean, key: String): Seq[FormError] ={
    verifyNewDate(date,taxYear,isAgent, key, StartDateValidationRequest, startDateValidation)
  }

  def verifyLeaveDate(date: EmploymentDate, taxYear: Int, isAgent: Boolean, key: String, startDate: String): Seq[FormError] ={
    verifyNewDate[LeaveDateValidationRequest](date,taxYear,isAgent, key, LeaveDateValidationRequest(LocalDate.parse(startDate)), leaveDateValidation)
  }

  private def verifyNewDate[ValidationParameters](date: EmploymentDate, taxYear: Int, isAgent: Boolean, key: String,
                                                  extraParameters: ValidationParameters, f: (LocalDate,Int,String,ValidationParameters) => Seq[FormError]
                                                 ): Seq[FormError] = {

    val agentCheck = if (isAgent) "agent" else "individual"
    val emptyDatesErrors: Seq[FormError] = areDatesEmpty(date, isAgent, key)
    if(emptyDatesErrors.isEmpty){
      val newDate: Either[Throwable, LocalDate] = Try(LocalDate.of(date.amountYear.toInt, date.amountMonth.toInt, date.amountDay.toInt)).toEither
      newDate match {
        case Right(date) => f(date,taxYear,agentCheck,extraParameters)
        case Left(_) => Seq(FormError("invalidFormat", s"employment.$key.error.invalidDate.$agentCheck"))
      }
    } else {
        emptyDatesErrors
    }
  }

  def startDateValidation(date: LocalDate, taxYear: Int, agentCheck: String, s: DateValidationRequest): Seq[FormError] = {

    (date.isAfter(LocalDate.now()), date.isBefore(sixthAprilDate(taxYear)), date.isAfter(tooLongAgoDate)) match {
      case (true, _, _) => Seq(FormError("invalidFormat", s"employment.$startDate.error.notInPast.$agentCheck"))
      case (_, false, _) => Seq(FormError("invalidFormat", s"employment.$startDate.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
      case (_, _, false) => Seq(FormError("invalidFormat", s"employment.$startDate.error.tooLongAgo.$agentCheck"))
      case _ => Seq()
    }
  }

  def leaveDateValidation(date: LocalDate, taxYear: Int, agentCheck: String, leaveValidation: LeaveDateValidationRequest): Seq[FormError] = {
    (date.isAfter(LocalDate.now()), date.isBefore(sixthAprilDate(taxYear)),
      date.isAfter(fifthAprilDate(taxYear-1)), !date.isBefore(leaveValidation.startDate)) match {
      case (true,_,_,_) => Seq(FormError("invalidFormat", s"employment.$leaveDate.error.notInPast.$agentCheck"))
      case (_,false,_,_) => Seq(FormError("invalidFormat", s"employment.$leaveDate.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
      case (_,_,false,_) => Seq(FormError("invalidFormat", s"employment.$leaveDate.error.tooLongAgo.$agentCheck", Seq((taxYear-1).toString)))
      case (_,_,_,false) => Seq(FormError("invalidFormat", s"employment.$leaveDate.error.beforeStartDate.$agentCheck",
        Seq(ViewUtils.dateFormatter(leaveValidation.startDate))))
      case _ => Seq()
    }
  }
}
