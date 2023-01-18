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

package forms.benefits.travel

import forms.{AmountForm, YesNoForm}
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class TravelFormsProvider {

  def entertainingBenefitsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.entertainingBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  def entertainmentBenefitsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.entertainmentBenefitAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.entertainmentBenefitAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.entertainmentBenefitAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def incidentalCostsBenefitsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.incidentalCostsBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.incidentalCostsBenefitsAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.incidentalCostsBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def incidentalOvernightCostEmploymentBenefitsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.incidentalOvernightCostEmploymentBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  def travelAndSubsistenceBenefitsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.travelAndSubsistence.error.${if (isAgent) "agent" else "individual"}"
  )

  def travelOrEntertainmentBenefitsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.travelOrEntertainment.error.${if (isAgent) "agent" else "individual"}"
  )

  def travelOrSubsistenceBenefitsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.travelOrSubsistenceBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.travelOrSubsistenceBenefitsAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.travelOrSubsistenceBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )
}
