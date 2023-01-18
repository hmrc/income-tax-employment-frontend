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

package forms.benefits.utilities

import forms.{AmountForm, YesNoForm}
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class UtilitiesFormsProvider {

  def employerProvidedServicesBenefitsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.employerProvidedServicesBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.employerProvidedServicesBenefitsAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.employerProvidedServicesBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def employerProvidedServicesBenefitsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.employerProvidedServices.error.no-entry.${if (isAgent) "agent" else "individual"}"
  )

  def otherServicesBenefitsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.otherServicesBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.otherServicesBenefitsAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.otherServicesBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def otherServicesBenefitsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.otherServicesBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  def professionalSubscriptionsBenefitsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.professionalSubscriptionsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.professionalSubscriptionsAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.professionalSubscriptionsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def professionalSubscriptionsBenefitsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.professionalSubscriptions.error.${if (isAgent) "agent" else "individual"}"
  )

  def telephoneBenefitsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.telephoneEmploymentBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.telephoneEmploymentBenefitsAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.telephoneEmploymentBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def telephoneBenefitsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.telephoneBenefits.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def utilitiesOrGeneralServicesBenefitsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.utilitiesOrGeneralServices.error.${if (isAgent) "agent" else "individual"}"
  )
}
