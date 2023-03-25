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
import java.time.Month.APRIL
import scala.util.Try

//TODO - unit test the form validation
object DateForm extends InputFilters {

  private val FIVE = 5
  private val SIX = 6

  private val tooLongAgoDate = LocalDate.parse("1900-01-01")

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
                       (implicit messages: Messages): Seq[FormError] = {
    lazy val emptyDateFieldsErrors = emptyDateFieldsValidation(formData, datePageName = "employmentStartDate", isAgent, Some(employerName))
    lazy val invalidDateFormatErrors = invalidDateFormatValidation(formData, datePageName = "employmentStartDate", isAgent, Some(employerName))
    lazy val startDateSpecificErrors = startDateSpecificValidation(formData.toLocalDate.get, taxYear, isAgent, employerName, endDate)

    emptyDateFieldsErrors match {
      case _ :: _ => emptyDateFieldsErrors
      case _ => if (invalidDateFormatErrors.nonEmpty) invalidDateFormatErrors else startDateSpecificErrors
    }
  }

  def validateEndDate(formData: DateFormData,
                      taxYear: Int,
                      isAgent: Boolean,
                      startDate: LocalDate)
                     (implicit messages: Messages): Seq[FormError] = {
    lazy val emptyDateFieldsErrors = emptyDateFieldsValidation(formData, datePageName = "employmentEndDate", isAgent, None)
    lazy val invalidDateFormatErrors = invalidDateFormatValidation(formData, datePageName = "employmentEndDate", isAgent, None)
    lazy val endDateSpecificErrors = endDateSpecificValidation(formData.toLocalDate.get, taxYear, isAgent, startDate)

    emptyDateFieldsErrors match {
      case _ :: _ => emptyDateFieldsErrors
      case _ => if (invalidDateFormatErrors.nonEmpty) invalidDateFormatErrors else endDateSpecificErrors
    }
  }

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
      case (_, _, _, false) => Seq(FormError("invalidFormat", s"employment.$pageNameKey.error.beforeStartDate.$agentCheck",
        Seq(dateFormatter(leaveValidation.startDate))))
      case _ => Seq()
    }
  }

  private def startDateSpecificValidation(startDate: LocalDate,
                                          taxYear: Int,
                                          isAgent: Boolean,
                                          employerName: String,
                                          endDate: Option[LocalDate])
                                         (implicit messages: Messages): Seq[FormError] = {

    val isBeforeEOY = startDate.isBefore(LocalDate.of(taxYear, APRIL, SIX))
    val isAfterMinDate = startDate.isAfter(tooLongAgoDate)
    val isBeforeDate = startDate.isBefore(endDate.getOrElse(startDate.plusDays(1)))

    lazy val mustBeSameAsOrBeforeErrorMessage = s"employment.employmentStartDate.error.tooRecent.${userType(isAgent)}"
    lazy val tooLongAgoErrorMessage = s"employment.employmentStartDate.error.tooLongAgo.${userType(isAgent)}"
    lazy val mustBeBeforeErrorMessage = s"employment.employmentStartDate.error.afterEndDate.${userType(isAgent)}"

    (isBeforeEOY, isAfterMinDate, isBeforeDate) match {
      case (false, _, _) => Seq(FormError("invalidFormat", mustBeSameAsOrBeforeErrorMessage, Seq(employerName, taxYear.toString)))
      case (_, false, _) => Seq(FormError("invalidFormat", tooLongAgoErrorMessage, Seq(employerName)))
      case (_, _, false) => Seq(FormError("invalidFormat", mustBeBeforeErrorMessage, Seq(employerName, translatedDateFormatter(endDate.get))))
      case _ => Seq.empty
    }
  }

  private def endDateSpecificValidation(endDate: LocalDate,
                                        taxYear: Int,
                                        isAgent: Boolean,
                                        startDate: LocalDate)
                                       (implicit messages: Messages): Seq[FormError] = {
    val isBeforeEOY = endDate.isBefore(LocalDate.of(taxYear, APRIL, SIX))
    val isAfterStartOfTaxYear = endDate.isAfter(LocalDate.of(taxYear - 1, APRIL, FIVE))
    val isAfterStartDate = endDate.isAfter(startDate)

    lazy val mustBeEndOfYearErrorMessage = s"employment.employmentEndDate.error.tooRecent.${userType(isAgent)}"
    lazy val mustBeAfterStartOfTaxYearErrorMessage = s"employment.employmentEndDate.error.tooLongAgo.${userType(isAgent)}"
    lazy val mustBeAfterStartDateErrorMessage = s"employment.employmentEndDate.error.beforeStartDate.${userType(isAgent)}"

    (isBeforeEOY, isAfterStartOfTaxYear, isAfterStartDate) match {
      case (false, _, _) => Seq(FormError("invalidFormat", mustBeEndOfYearErrorMessage, Seq(taxYear.toString)))
      case (_, false, _) => Seq(FormError("invalidFormat", mustBeAfterStartOfTaxYearErrorMessage, Seq((taxYear - 1).toString)))
      case (_, _, false) => Seq(FormError("invalidFormat", mustBeAfterStartDateErrorMessage, Seq(translatedDateFormatter(startDate))))
      case _ => Seq.empty
    }
  }

  private def emptyDateFieldsValidation(formData: DateFormData,
                                        datePageName: String,
                                        isAgent: Boolean,
                                        employerName: Option[String]): Seq[FormError] = {
    lazy val userTypeValue = userType(isAgent)
    val messageParams = employerName.toSeq

    (formData.amountDay.isEmpty, formData.amountMonth.isEmpty, formData.amountYear.isEmpty) match {
      case (true, true, true) => Seq(FormError(s"emptyAll", s"employment.$datePageName.error.incompleteAll.$userTypeValue", messageParams))
      case (true, true, false) => Seq(FormError("emptyDayMonth", s"employment.$datePageName.error.incompleteDayMonth.$userTypeValue", messageParams))
      case (true, false, true) => Seq(FormError("emptyDayYear", s"employment.$datePageName.error.incompleteDayYear.$userTypeValue", messageParams))
      case (false, true, true) => Seq(FormError("emptyMonthYear", s"employment.$datePageName.error.incompleteMonthYear.$userTypeValue", messageParams))
      case (false, false, true) => Seq(FormError("emptyYear", s"employment.$datePageName.error.incompleteYear.$userTypeValue", messageParams))
      case (false, true, false) => Seq(FormError("emptyMonth", s"employment.$datePageName.error.incompleteMonth.$userTypeValue", messageParams))
      case (true, false, false) => Seq(FormError("emptyDay", s"employment.$datePageName.error.incompleteDay.$userTypeValue", messageParams))
      case (false, false, false) => Seq()
    }
  }

  private def invalidDateFormatValidation(formData: DateFormData,
                                          datePageName: String,
                                          isAgent: Boolean,
                                          employerName: Option[String]): Seq[FormError] = {
    val messageParams = employerName.toSeq
    Try(LocalDate.of(formData.amountYear.toInt, formData.amountMonth.toInt, formData.amountDay.toInt)).toOption match {
      case None => Seq(FormError("invalidFormat", s"employment.$datePageName.error.invalidDate.${userType(isAgent)}", messageParams))
      case _ => Seq.empty
    }
  }

  private def userType(isAgent: Boolean): String = if (isAgent) "agent" else "individual"
}
