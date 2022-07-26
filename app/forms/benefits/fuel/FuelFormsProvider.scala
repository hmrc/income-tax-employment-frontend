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

package forms.benefits.fuel

import forms.{AmountForm, YesNoForm}
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class FuelFormsProvider {

  def carVanFuelForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.carVanFuel.error.${if (isAgent) "agent" else "individual"}"
  )

  def carFuelAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.carFuelAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.carFuelAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.carFuelAmount.error.tooMuch.${if (isAgent) "agent" else "individual"}"
  )

  def companyCarForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"CompanyCarBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  def companyCarAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.companyCarBenefitsAmount.error.no-entry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.companyCarBenefitsAmount.error.incorrect-format.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.companyCarBenefitsAmount.error.max-length.${if (isAgent) "agent" else "individual"}"
  )

  def companyCarFuelForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.companyCarFuelBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  def companyVanForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.companyVanBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  def companyVanAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.companyVanAmountBenefits.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.companyVanAmountBenefits.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.companyVanAmountBenefits.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def companyVanFuelForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.companyVanFuelBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  def companyVanFuelAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.companyVanFuelAmountBenefits.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.companyVanFuelAmountBenefits.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.companyVanFuelAmountBenefits.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def receiveOwnCarMileageForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.receiveOwnCarMileageBenefit.error.${if (isAgent) "agent" else "individual"}"
  )

  def mileageAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.mileageBenefitAmount.error.empty.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.mileageBenefitAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.mileageBenefitAmount.error.amountMaxLimit.${if (isAgent) "agent" else "individual"}"
  )
}
