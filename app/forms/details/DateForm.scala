/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.details

import filters.InputFilters
import forms.validation.mappings.MappingUtil.trimmedText
import models.employment.DateFormData
import play.api.data.Forms.mapping
import play.api.data.{Form, FormError}
import utils.ViewUtils._

import java.time.LocalDate
import scala.util.Try

//TODO - unit test the form validation and error message keys
object DateForm extends InputFilters {

  val startDate = "employmentStartDate"
  val leaveDate = "employmentLeaveDate"

  val day: String = "amount-day"
  val month: String = "amount-month"
  val year: String = "amount-year"

  def dateForm(): Form[DateFormData] = Form(
    mapping(
      day -> trimmedText.transform[String](filter, identity),
      month -> trimmedText.transform[String](filter, identity),
      year -> trimmedText.transform[String](filter, identity)
    )(DateFormData.apply)(DateFormData.unapply)
  )

  val april = 4
  val startDay = 6

  def sixthAprilDate(taxYear: Int): LocalDate = LocalDate.of(taxYear, april, startDay)

  def fifthAprilDate(taxYear: Int): LocalDate = LocalDate.of(taxYear, april, startDay - 1)

  object tooLongAgo {
    val day = 1
    val month = 1
    val year = 1900
  }

  val tooLongAgoDate: LocalDate = LocalDate.of(tooLongAgo.year, tooLongAgo.month, tooLongAgo.day)

  trait DateValidationRequest

  case object StartDateValidationRequest extends DateValidationRequest

  case class EndDateValidationRequest(startDate: LocalDate) extends DateValidationRequest

  case class LeaveDateValidationRequest(startDate: LocalDate) extends DateValidationRequest

  def verifyStartDate(date: DateFormData, taxYear: Int, isAgent: Boolean, key: String): Seq[FormError] = {
    verifyNewDate(date, taxYear, isAgent, key, StartDateValidationRequest, startDateValidation)
  }

  def verifyEndDate(date: DateFormData,
                    taxYear: Int,
                    isAgent: Boolean,
                    key: String,
                    startDate: LocalDate): Seq[FormError] = {
    verifyNewDate[EndDateValidationRequest](date, taxYear, isAgent, key, EndDateValidationRequest(startDate), endDateValidation)
  }

  def verifyLeaveDate(date: DateFormData, taxYear: Int, isAgent: Boolean, key: String, startDate: String): Seq[FormError] = {
    verifyNewDate[LeaveDateValidationRequest](date, taxYear, isAgent, key, LeaveDateValidationRequest(LocalDate.parse(startDate)), leaveDateValidation)
  }

  private def verifyNewDate[ValidationParameters](date: DateFormData,
                                                  taxYear: Int,
                                                  isAgent: Boolean,
                                                  key: String,
                                                  extraParameters: ValidationParameters,
                                                  f: (LocalDate, Int, String, String, ValidationParameters) => Seq[FormError]): Seq[FormError] = {
    val agentCheck = if (isAgent) "agent" else "individual"
    val emptyDatesErrors: Seq[FormError] = areDatesEmpty(date, isAgent, key)
    if (emptyDatesErrors.isEmpty) {
      val newDate: Either[Throwable, LocalDate] = Try(LocalDate.of(date.amountYear.toInt, date.amountMonth.toInt, date.amountDay.toInt)).toEither
      newDate match {
        case Right(date) => f(date, taxYear, agentCheck, key, extraParameters)
        case Left(_) => Seq(FormError("invalidFormat", s"employment.$key.error.invalidDate.$agentCheck"))
      }
    } else {
      emptyDatesErrors
    }
  }

  private def areDatesEmpty(date: DateFormData, isAgent: Boolean, key: String): Seq[FormError] = {
    val agentCheck: String = if (isAgent) "agent" else "individual"
    (date.amountDay.isEmpty, date.amountMonth.isEmpty, date.amountYear.isEmpty) match {
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


  private def startDateValidation(date: LocalDate, taxYear: Int, agentCheck: String, pageNameKey: String, s: DateValidationRequest): Seq[FormError] = {
    (date.isAfter(LocalDate.now()), date.isBefore(sixthAprilDate(taxYear)), date.isAfter(tooLongAgoDate)) match {
      case (true, _, _) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.notInPast.$agentCheck"))
      case (_, false, _) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
      case (_, _, false) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.tooLongAgo.$agentCheck"))
      case _ => Seq()
    }
  }

  private def endDateValidation(date: LocalDate,
                                taxYear: Int,
                                agentCheck: String,
                                pageNameKey: String,
                                endDateValidation: EndDateValidationRequest): Seq[FormError] = {
    (date.isBefore(sixthAprilDate(taxYear)), date.isAfter(fifthAprilDate(taxYear - 1)), date.isAfter(endDateValidation.startDate)) match {
      case (false, _, _) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
      case (_, false, _) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.tooLongAgo.$agentCheck", Seq((taxYear - 1).toString)))
      case (_, _, false) =>
        Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.beforeStartDate.$agentCheck", Seq(dateFormatter(endDateValidation.startDate))))
      case _ => Seq()
    }
  }

  def leaveDateValidation(date: LocalDate,
                          taxYear: Int,
                          agentCheck: String,
                          pageNameKey: String,
                          leaveValidation: LeaveDateValidationRequest): Seq[FormError] = {
    (date.isAfter(LocalDate.now()), date.isBefore(sixthAprilDate(taxYear)),
      date.isAfter(fifthAprilDate(taxYear - 1)), !date.isBefore(leaveValidation.startDate)) match {
      case (true, _, _, _) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.notInPast.$agentCheck"))
      case (_, false, _, _) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
      case (_, _, false, _) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.tooLongAgo.$agentCheck", Seq((taxYear - 1).toString)))
      case (_, _, _, false) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.beforeStartDate.$agentCheck",
        Seq(dateFormatter(leaveValidation.startDate))))
      case _ => Seq()
    }
  }
}
