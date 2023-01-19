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

package forms.benefits.accommodation

import forms.{AmountForm, YesNoForm}
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class AccommodationFormsProvider {

  def accommodationRelocationForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.accommodationRelocation.error.${if (isAgent) "agent" else "individual"}"
  )

  def livingAccommodationForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.livingAccommodationBenefits.error.no-entry.${if (isAgent) "agent" else "individual"}"
  )

  def livingAccommodationAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.livingAccommodationAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.livingAccommodationAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.livingAccommodationAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def nonQualifyingRelocationForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.nonQualifyingRelocationQuestion.error.${if (isAgent) "agent" else "individual"}"
  )

  def nonQualifyingRelocationAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.nonQualifyingRelocationBenefitAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.nonQualifyingRelocationBenefitAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.nonQualifyingRelocationBenefitAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def qualifyingRelocationForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.qualifyingRelocationBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  def qualifyingRelocationAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.qualifyingRelocationBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.qualifyingRelocationBenefitsAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.qualifyingRelocationBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )
}
