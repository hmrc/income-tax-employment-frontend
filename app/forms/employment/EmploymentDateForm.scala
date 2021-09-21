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

  def verifyNewDate(date: EmploymentDate, taxYear: Int, isAgent: Boolean, key: String): Seq[FormError] = {
    val agentCheck = if (isAgent) "agent" else "individual"
    val emptyDatesErrors: Seq[FormError] = areDatesEmpty(date, isAgent, key)
    if(emptyDatesErrors.isEmpty){
      val newDate: Either[Throwable, LocalDate] = Try(LocalDate.of(date.amountYear.toInt, date.amountMonth.toInt, date.amountDay.toInt)).toEither
      newDate match {
        case Right(date) =>

          key match {
            case `startDate` => startDateValidation(date,taxYear,agentCheck)
            case `leaveDate` => leaveDateValidation(date,taxYear,agentCheck)
          }

        case Left(_) => Seq(FormError("invalidFormat", s"employment.$key.error.invalidDate.$agentCheck"))
      }
    } else {
        emptyDatesErrors
    }
  }

  def startDateValidation(date: LocalDate, taxYear: Int, agentCheck: String): Seq[FormError] = {
    (date.isAfter(LocalDate.now()), date.isBefore(sixthAprilDate(taxYear))) match {
      case (true, _) => Seq(FormError("invalidFormat", s"employment.$startDate.error.notInPast.$agentCheck"))
      case (_, false) => Seq(FormError("invalidFormat", s"employment.$startDate.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
      case _ => Seq()
    }
  }

  def leaveDateValidation(date: LocalDate, taxYear: Int, agentCheck: String): Seq[FormError] = {
    (date.isAfter(LocalDate.now()), date.isBefore(sixthAprilDate(taxYear)), date.isAfter(fifthAprilDate(taxYear-1))) match {
      case (true, _,_) => Seq(FormError("invalidFormat", s"employment.$leaveDate.error.notInPast.$agentCheck"))
      case (_, false,_) => Seq(FormError("invalidFormat", s"employment.$leaveDate.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
      case (_, _, false) => Seq(FormError("invalidFormat", s"employment.$leaveDate.error.tooLongAgo.$agentCheck", Seq((taxYear-1).toString)))
      case _ => Seq()
    }
  }
}
