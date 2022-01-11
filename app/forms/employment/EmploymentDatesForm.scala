/*
 * Copyright 2022 HM Revenue & Customs
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
import models.employment.{EmploymentDate, EmploymentDates}
import play.api.data.Forms.mapping
import play.api.data.{Form, FormError}
import utils.ViewUtils

import java.time.LocalDate
import scala.util.Try

object EmploymentDatesForm {

  val startAmountDay: String = "startAmount-day"
  val startAmountMonth: String = "startAmount-month"
  val startAmountYear: String = "startAmount-year"

  val endAmountDay: String = "endAmount-day"
  val endAmountMonth: String = "endAmount-month"
  val endAmountYear: String = "endAmount-year"

  def employmentDatesForm: Form[EmploymentDates] =
    Form(
      mapping(
        startAmountDay -> trimmedText,
        startAmountMonth -> trimmedText,
        startAmountYear -> trimmedText,

        endAmountDay -> trimmedText,
        endAmountMonth -> trimmedText,
        endAmountYear -> trimmedText
      )(EmploymentDates.apply)(EmploymentDates.unapply)
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

  def areStartDatesEmpty(dates: EmploymentDates, isAgent: Boolean): Seq[FormError] = {
    val agentCheck: String = if (isAgent) "agent" else "individual"
    (dates.startAmountDay.isEmpty, dates.startAmountMonth.isEmpty, dates.startAmountYear.isEmpty) match{
      case (true, true, true) => Seq(FormError(s"startAmount-day", s"employment.employmentStartDate.error.incompleteAll.$agentCheck"))
      case (true, true, false) => Seq(FormError("startAmount-day", s"employment.employmentStartDate.error.incompleteDayMonth.$agentCheck"))
      case (true, false, true) => Seq(FormError("startAmount-day", s"employment.employmentStartDate.error.incompleteDayYear.$agentCheck"))
      case (false, true, true) => Seq(FormError("startAmount-month", s"employment.employmentStartDate.error.incompleteMonthYear.$agentCheck"))
      case (false, false, true) => Seq(FormError("startAmount-year", s"employment.employmentStartDate.error.incompleteYear.$agentCheck"))
      case (false, true, false) => Seq(FormError("startAmount-month", s"employment.employmentStartDate.error.incompleteMonth.$agentCheck"))
      case (true, false, false) => Seq(FormError("startAmount-day", s"employment.employmentStartDate.error.incompleteDay.$agentCheck"))
      case (false, false, false) => Seq()
    }
  }

  def areEndDatesEmpty(dates: EmploymentDates, isAgent: Boolean): Seq[FormError] = {
    val agentCheck: String = if (isAgent) "agent" else "individual"
    (dates.endAmountDay.isEmpty, dates.endAmountMonth.isEmpty, dates.endAmountYear.isEmpty) match{
      case (true, true, true) => Seq(FormError(s"endAmount-day", s"employment.employmentLeaveDate.error.incompleteAll.$agentCheck"))
      case (true, true, false) => Seq(FormError("endAmount-day", s"employment.employmentLeaveDate.error.incompleteDayMonth.$agentCheck"))
      case (true, false, true) => Seq(FormError("endAmount-day", s"employment.employmentLeaveDate.error.incompleteDayYear.$agentCheck"))
      case (false, true, true) => Seq(FormError("endAmount-month", s"employment.employmentLeaveDate.error.incompleteMonthYear.$agentCheck"))
      case (false, false, true) => Seq(FormError("endAmount-year", s"employment.employmentLeaveDate.error.incompleteYear.$agentCheck"))
      case (false, true, false) => Seq(FormError("endAmount-month", s"employment.employmentLeaveDate.error.incompleteMonth.$agentCheck"))
      case (true, false, false) => Seq(FormError("endAmount-day", s"employment.employmentLeaveDate.error.incompleteDay.$agentCheck"))
      case (false, false, false) => Seq()
    }
  }

  def verifyDates(dates: EmploymentDates, taxYear: Int, isAgent: Boolean): Seq[FormError] = {
    val agentCheck = if (isAgent) "agent" else "individual"

    val startDateVerify = {
      if(areStartDatesEmpty(dates, isAgent).nonEmpty){
        areStartDatesEmpty(dates, isAgent)
      } else {
        val newDate: Either[Throwable, LocalDate] =
          Try(LocalDate.of(dates.startAmountYear.toInt, dates.startAmountMonth.toInt, dates.startAmountDay.toInt)).toEither
        newDate match {
          case Right(_) => verifyStartDate(dates, taxYear, isAgent)
          case Left(_) => Seq(FormError("startAmount-day", s"employment.employmentStartDate.error.invalidDate.$agentCheck"))
        }
      }
    }
    val endDateVerify = {
      if (areEndDatesEmpty(dates, isAgent).nonEmpty) {
        areEndDatesEmpty(dates, isAgent)
      } else {
        val newDate: Either[Throwable, LocalDate] = Try(LocalDate.of(dates.endAmountYear.toInt, dates.endAmountMonth.toInt, dates.endAmountDay.toInt)).toEither
        newDate match {
          case Right(_) => verifyLeaveDate(dates, taxYear, isAgent)
          case Left(_) => Seq(FormError("endAmount-day", s"employment.employmentLeaveDate.error.invalidDate.$agentCheck"))
        }
      }
    }
    startDateVerify ++ endDateVerify match {
      case Seq() => rightOrderValidation(dates.startDateToLocalDate, dates.endDateToLocalDate, isAgent)
      case _ => startDateVerify ++ endDateVerify
    }
  }

  def verifyStartDate(dates: EmploymentDates, taxYear: Int, isAgent: Boolean): Seq[FormError] ={
    val agentCheck = if (isAgent) "agent" else "individual"
      startDateValidation(dates.startDateToLocalDate, taxYear, agentCheck)
  }

  def verifyLeaveDate(dates: EmploymentDates, taxYear: Int, isAgent: Boolean): Seq[FormError] ={
    val agentCheck = if (isAgent) "agent" else "individual"
      leaveDateValidation(dates.endDateToLocalDate, taxYear, agentCheck)
  }

  def startDateValidation(date: LocalDate, taxYear: Int, agentCheck: String): Seq[FormError] = {
    (date.isAfter(LocalDate.now()), date.isBefore(sixthAprilDate(taxYear)), date.isAfter(tooLongAgoDate)) match {
      case (true, _, _) => Seq(FormError("startAmount-day", s"employment.employmentStartDate.error.notInPast.$agentCheck"))
      case (_, false, _) => Seq(FormError("startAmount-day", s"employment.employmentStartDate.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
      case (_, _, false) => Seq(FormError("startAmount-day", s"employment.employmentStartDate.error.tooLongAgo.$agentCheck"))
      case _ => Seq()
    }
  }

  def leaveDateValidation(date: LocalDate, taxYear: Int, agentCheck: String): Seq[FormError] = {
    (date.isAfter(LocalDate.now()), date.isBefore(sixthAprilDate(taxYear)), date.isAfter(fifthAprilDate(taxYear-1))) match {
      case (true,_,_) => Seq(FormError("endAmount-day", s"employment.employmentLeaveDate.error.notInPast.$agentCheck"))
      case (_,false,_) => Seq(FormError("endAmount-day", s"employment.employmentLeaveDate.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
      case (_,_,false) => Seq(FormError("endAmount-day", s"employment.employmentLeaveDate.error.tooLongAgo.$agentCheck", Seq((taxYear-1).toString)))
      case _ => Seq()
    }
  }

  def rightOrderValidation(startDate: LocalDate, endDate: LocalDate, isAgent: Boolean): Seq[FormError] = {
    val agentCheck = if (isAgent) "agent" else "individual"
    if(startDate.isAfter(endDate)){
      Seq(FormError("endAmount-day", s"employment.employmentLeaveDate.error.beforeStartDate.$agentCheck", Seq(ViewUtils.dateFormatter(startDate))))
    }
    else{
      Seq()
    }
  }
}
