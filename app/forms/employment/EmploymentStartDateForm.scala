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
import scala.util.Try

object EmploymentStartDateForm {

  val startDateDay: String = "amount-day"
  val startDateMonth: String = "amount-month"
  val startDateYear: String = "amount-year"

  def employmentStartDateForm: Form[EmploymentDate] =
    Form(
      mapping(
        startDateDay -> trimmedText,
        startDateMonth -> trimmedText,
        startDateYear -> trimmedText
      )(EmploymentDate.apply)(EmploymentDate.unapply)
    )

  def fifthAprilDate(taxYear: Int): LocalDate = LocalDate.of(taxYear, 4, 5)

  def areDatesEmpty(date: EmploymentDate, isAgent: Boolean): Seq[FormError] = {
    val agentCheck: String = if (isAgent) "agent" else "individual"
    (date.amountDay.isEmpty, date.amountMonth.isEmpty, date.amountYear.isEmpty) match{
      case (true, true, true) => Seq(FormError(s"emptyAll", s"employment.employmentStartDate.error.incompleteAll.$agentCheck"))
      case (true, true, false) => Seq(FormError("emptyDayMonth", s"employment.employmentStartDate.error.incompleteDayMonth.$agentCheck"))
      case (true, false, true) => Seq(FormError("emptyDayYear", s"employment.employmentStartDate.error.incompleteDayYear.$agentCheck"))
      case (false, true, true) => Seq(FormError("emptyMonthYear", s"employment.employmentStartDate.error.incompleteMonthYear.$agentCheck"))
      case (false, false, true) => Seq(FormError("emptyYear", s"employment.employmentStartDate.error.incompleteYear.$agentCheck"))
      case (false, true, false) => Seq(FormError("emptyMonth", s"employment.employmentStartDate.error.incompleteMonth.$agentCheck"))
      case (true, false, false) => Seq(FormError("emptyDay", s"employment.employmentStartDate.error.incompleteDay.$agentCheck"))
      case (false, false, false) => Seq()
    }
  }

  def verifyNewDate(date: EmploymentDate, taxYear: Int, isAgent: Boolean): Seq[FormError] = {
    val agentCheck = if (isAgent) "agent" else "individual"
    val emptyDatesErrors: Seq[FormError] = areDatesEmpty(date, isAgent)
    if(emptyDatesErrors.isEmpty){
      val newDate: Either[Throwable, LocalDate] = Try(LocalDate.of(date.amountYear.toInt, date.amountMonth.toInt, date.amountDay.toInt)).toEither
      newDate match {
        case Right(date) =>
          (date.isAfter(LocalDate.now()), date.isBefore(fifthAprilDate(taxYear))) match {
            case (true, _) => Seq(FormError("invalidFormat", s"employment.employmentStartDate.error.notInPast.$agentCheck"))
            case (_, false) => Seq(FormError("invalidFormat", s"employment.employmentStartDate.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
            case _ => Seq()
          }
        case Left(_) => Seq(FormError("invalidFormat", s"employment.employmentStartDate.error.invalidDate.$agentCheck"))
      }
    } else {
        emptyDatesErrors
    }
  }
}
