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
import play.api.i18n.Messages
import utils.ViewUtils.{dateFormatter, translatedDateFormatter}

import java.time.LocalDate
import java.time.Month.{APRIL, JANUARY}
import scala.util.Try

//TODO - unit test the form validation
object DateForm extends InputFilters {

  private val ONE = 1
  private val SIX = 6
  private val NINETEEN_HUNDRED = 1900

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

  def validateStartDate(formData: DateFormData,
                        taxYear: Int,
                        isAgent: Boolean,
                        employerName: String,
                        endDate: Option[LocalDate])
                       (implicit messages: Messages): Seq[FormError] =
    (for {
      date <- dateOrAllCommonValidation(formData, taxYear, datePageName = "employmentStartDate", isAgent, employerName)
      _ <- startDateSpecificValidation(date, isAgent, employerName, endDate).toLeft(())
    } yield None).left.toSeq

  def validateEndDate(formData: DateFormData,
                      taxYear: Int,
                      isAgent: Boolean,
                      employerName: String,
                      startDate: LocalDate)
                     (implicit messages: Messages): Seq[FormError] =
    (for {
      date <- dateOrAllCommonValidation(formData, taxYear, datePageName = "employmentEndDate", isAgent, employerName)
      _ <- endDateSpecificValidation(date, isAgent, employerName, startDate).toLeft(())
    } yield None).left.toSeq

  private def dateOrAllCommonValidation(formData: DateFormData,
                                        taxYear: Int,
                                        datePageName: String,
                                        isAgent: Boolean,
                                        employerName: String): Either[FormError, LocalDate] =
    for {
      _ <- emptyDateFieldsValidation(formData, datePageName, isAgent, employerName).toLeft(())
      date <- dateOrInvalidDateFormatValidation(formData, datePageName, isAgent, employerName)
      _ <- commonDateValidation(date, taxYear, datePageName, isAgent, employerName).toLeft(())
    } yield date

  // TODO: delete this once employmentDates page goes away
  object DeprecatedObjects {
    trait DateValidationRequest

    case class LeaveDateValidationRequest(startDate: LocalDate) extends DateValidationRequest
  }

  // TODO: delete this once employmentDates page goes away
  def leaveDateValidation(date: LocalDate,
                          taxYear: Int,
                          agentCheck: String,
                          pageNameKey: String,
                          leaveValidation: DeprecatedObjects.LeaveDateValidationRequest): Seq[FormError] = {
    val april = 4
    val startDay = 6

    def sixthAprilDate(taxYear: Int): LocalDate = LocalDate.of(taxYear, april, startDay)

    def fifthAprilDate(taxYear: Int): LocalDate = LocalDate.of(taxYear, april, startDay - 1)

    (date.isAfter(LocalDate.now()), date.isBefore(sixthAprilDate(taxYear)),
      date.isAfter(fifthAprilDate(taxYear - 1)), !date.isBefore(leaveValidation.startDate)) match {
      case (true, _, _, _) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.notInPast.$agentCheck"))
      case (_, false, _, _) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.tooRecent.$agentCheck", Seq(taxYear.toString)))
      case (_, _, false, _) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.tooLongAgo.$agentCheck", Seq((taxYear - 1).toString)))
      case (_, _, _, false) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.mustBeBeforeStartDate.$agentCheck",
        Seq(dateFormatter(leaveValidation.startDate))))
      case _ => Seq()
    }
  }

  private def commonDateValidation(date: LocalDate,
                                   taxYear: Int,
                                   datePageName: String,
                                   isAgent: Boolean,
                                   employerName: String): Option[FormError] = {

    val year = date.getYear
    val has4DigitYear = year >= 1000 && year < 10000
    val isBeforeEOY = date.isBefore(LocalDate.of(taxYear, APRIL, SIX))

    lazy val mustHave4DigitYearErrorMessage = s"employment.$datePageName.error.mustHave4DigitYear.${userType(isAgent)}"
    lazy val mustBeBeforeEndOfTaxYearErrorMessage = s"employment.$datePageName.error.mustBeBeforeEndOfTaxYear.${userType(isAgent)}"

    (has4DigitYear, isBeforeEOY) match {
      case (false, _) => Some(FormError("invalidFormat", mustHave4DigitYearErrorMessage, Seq(employerName)))
      case (_, false) => Some(FormError("invalidFormat", mustBeBeforeEndOfTaxYearErrorMessage, Seq(employerName, taxYear.toString)))
      case _ => None
    }
  }

  private def startDateSpecificValidation(startDate: LocalDate,
                                          isAgent: Boolean,
                                          employerName: String,
                                          endDate: Option[LocalDate])
                                         (implicit messages: Messages): Option[FormError] = {

    val isAfter1900 = startDate.isAfter(LocalDate.of(NINETEEN_HUNDRED, JANUARY, ONE))
    val isBeforeEndDate = startDate.isBefore(endDate.getOrElse(startDate.plusDays(1)))

    lazy val mustBeAfter1900ErrorMessage = s"employment.employmentStartDate.error.mustBeAfter1900.${userType(isAgent)}"
    lazy val mustBeBeforeEndDateErrorMessage = s"employment.employmentStartDate.error.mustBeBeforeEndDate.${userType(isAgent)}"

    (isAfter1900, isBeforeEndDate) match {
      case (false, _) => Some(FormError("invalidFormat", mustBeAfter1900ErrorMessage, Seq(employerName)))
      case (_, false) => Some(FormError("invalidFormat", mustBeBeforeEndDateErrorMessage, Seq(employerName, translatedDateFormatter(endDate.get))))
      case _ => None
    }
  }

  private def endDateSpecificValidation(endDate: LocalDate,
                                        isAgent: Boolean,
                                        employerName: String,
                                        startDate: LocalDate)
                                       (implicit messages: Messages): Option[FormError] = {

    val isAfterStartDate = endDate.isAfter(startDate)

    lazy val mustBeAfterStartDateErrorMessage = s"employment.employmentEndDate.error.mustBeBeforeStartDate.${userType(isAgent)}"

    Option.when(!isAfterStartDate)(FormError("invalidFormat", mustBeAfterStartDateErrorMessage, Seq(employerName, translatedDateFormatter(startDate))))
  }

  private def emptyDateFieldsValidation(formData: DateFormData,
                                        datePageName: String,
                                        isAgent: Boolean,
                                        employerName: String): Option[FormError] = {
    lazy val userTypeValue = userType(isAgent)
    val messageParams = Seq(employerName)

    (formData.amountDay.isEmpty, formData.amountMonth.isEmpty, formData.amountYear.isEmpty) match {
      case (true, true, true) => Some(FormError(s"emptyAll", s"employment.$datePageName.error.incompleteAll.$userTypeValue", messageParams))
      case (true, true, false) => Some(FormError("emptyDayMonth", s"employment.$datePageName.error.incompleteDayMonth.$userTypeValue", messageParams))
      case (true, false, true) => Some(FormError("emptyDayYear", s"employment.$datePageName.error.incompleteDayYear.$userTypeValue", messageParams))
      case (false, true, true) => Some(FormError("emptyMonthYear", s"employment.$datePageName.error.incompleteMonthYear.$userTypeValue", messageParams))
      case (false, false, true) => Some(FormError("emptyYear", s"employment.$datePageName.error.incompleteYear.$userTypeValue", messageParams))
      case (false, true, false) => Some(FormError("emptyMonth", s"employment.$datePageName.error.incompleteMonth.$userTypeValue", messageParams))
      case (true, false, false) => Some(FormError("emptyDay", s"employment.$datePageName.error.incompleteDay.$userTypeValue", messageParams))
      case (false, false, false) => None
    }
  }

  private def dateOrInvalidDateFormatValidation(formData: DateFormData,
                                                datePageName: String,
                                                isAgent: Boolean,
                                                employerName: String): Either[FormError, LocalDate] = {
    Try(LocalDate.of(formData.amountYear.toInt, formData.amountMonth.toInt, formData.amountDay.toInt)).toOption match {
      case None => Left(FormError("invalidFormat", s"employment.$datePageName.error.invalidDate.${userType(isAgent)}", Seq(employerName)))
      case Some(date) => Right(date)
    }
  }

  private def userType(isAgent: Boolean): String = if (isAgent) "agent" else "individual"
}
