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

import forms.details.DateForm.{validateEndDate, validateStartDate}
import forms.{AmountForm, YesNoForm}
import models.employment.DateFormData
import play.api.data.Form
import play.api.i18n.Messages
import utils.InYearUtil.toDateWithinTaxYear
import utils.ViewUtils.{translatedDateFormatter, translatedTaxYearEndDateFormatter}

import java.time.LocalDate

class EmploymentDetailsFormsProvider {

  def didYouLeaveForm(isAgent: Boolean,
                      taxYear: Int,
                      employmentStartDate: LocalDate)
                     (implicit messages: Messages): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"employment.didYouLeave.error.${if (isAgent) "agent" else "individual"}",
    Seq(translatedDateFormatter(toDateWithinTaxYear(taxYear, employmentStartDate)), translatedTaxYearEndDateFormatter(taxYear))
  )

  def employerPayAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"employerPayAmount.error.empty.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = "employerPayAmount.error.wrongFormat",
    exceedsMaxAmountKey = "employerPayAmount.error.amountMaxLimit"
  )

  def employmentTaxAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"employment.employmentTax.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = "employment.employmentTax.error.format",
    exceedsMaxAmountKey = "employment.employmentTax.error.max"
  )

  def validatedStartDateForm(dateForm: Form[DateFormData],
                             taxYear: Int,
                             isAgent: Boolean,
                             employerName: String,
                             endDate: Option[LocalDate])
                            (implicit messages: Messages): Form[DateFormData] = {
    dateForm.copy(errors = validateStartDate(dateForm.get, taxYear, isAgent, employerName, endDate))
  }

  def validatedEndDateForm(dateForm: Form[DateFormData],
                           taxYear: Int,
                           isAgent: Boolean,
                           startDate: LocalDate)
                          (implicit messages: Messages): Form[DateFormData] = {
    dateForm.copy(errors = validateEndDate(dateForm.get, taxYear, isAgent, startDate))
  }

  def employerPayrollIdForm(): Form[String] = EmployerPayrollIdForm.employerPayrollIdForm(
    invalidCharactersKey = "employment.payrollId.error.invalidCharacters",
    tooManyCharactersKey = "employment.payrollId.error.tooManyCharacters"
  )
}
