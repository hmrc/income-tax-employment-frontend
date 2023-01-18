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

package forms.benefits.medical

import forms.{AmountForm, YesNoForm}
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class MedicalFormsProvider {

  def beneficialLoansForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.beneficialLoans.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def beneficialLoansAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.beneficialLoansAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.beneficialLoansAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.beneficialLoansAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def childcareForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.childcare.error.${if (isAgent) "agent" else "individual"}"
  )

  def childcareAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.childcareBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.childcareBenefitsAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.childcareBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def educationalServicesForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.educationalServices.error.${if (isAgent) "agent" else "individual"}"
  )

  def educationalServicesAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.educationalServicesBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.educationalServicesBenefitsAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.educationalServicesBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def medicalDentalForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.medicalDentalBenefits.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def medicalDentalChildcareForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.medicalDentalChildcare.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def medicalOrAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.medicalOrDentalBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.medicalOrDentalBenefitsAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.medicalOrDentalBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )
}
