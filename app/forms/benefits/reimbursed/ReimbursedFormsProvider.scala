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

package forms.benefits.reimbursed

import forms.{AmountForm, YesNoForm}
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class ReimbursedFormsProvider {

  def nonCashForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.nonCashBenefits.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def nonCashAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.nonCashBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.nonCashBenefitsAmount.error.incorrectFormat",
    exceedsMaxAmountKey = s"benefits.nonCashBenefitsAmount.error.overMaximum"
  )

  def nonTaxableCostsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.nonTaxableCosts.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def nonTaxableCostsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.nonTaxableCostsBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.nonTaxableCostsBenefitsAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.nonTaxableCostsBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def otherBenefitsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.otherBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  def otherBenefitsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.otherBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.otherBenefitsAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = "benefits.otherBenefitsAmount.error.overMaximum"
  )

  def vouchersAndNonCashForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.reimbursedCostsVouchersAndNonCash.error.${if (isAgent) "agent" else "individual"}"
  )

  def taxableCostsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.taxableCosts.error.${if (isAgent) "agent" else "individual"}"
  )

  def taxableCostsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.taxableCostsBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.taxableCostsBenefitsAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.taxableCostsBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def vouchersForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.vouchersBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  def vouchersAmountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "benefits.vouchersBenefitsAmount.error.noEntry",
    wrongFormatKey = "benefits.vouchersBenefitsAmount.error.incorrectFormat",
    exceedsMaxAmountKey = "benefits.vouchersBenefitsAmount.error.overMaximum"
  )
}
