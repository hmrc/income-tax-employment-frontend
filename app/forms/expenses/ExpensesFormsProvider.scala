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

package forms.expenses

import forms.{AmountForm, YesNoForm}
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class ExpensesFormsProvider {

  def businessTravelExpensesForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"expenses.businessTravelOvernightExpenses.error.${if (isAgent) "agent" else "individual"}"
  )

  def claimEmploymentExpensesForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"expenses.claimEmploymentExpenses.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def businessTravelAndOvernightAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"expenses.businessTravelAndOvernightAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"expenses.businessTravelAndOvernightAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"expenses.businessTravelAndOvernightAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def uniformsWorkClothesToolsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"expenses.uniformsWorkClothesTools.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )

  def uniformsWorkClothesToolsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"expenses.uniformsWorkClothesToolsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"expenses.uniformsWorkClothesToolsAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"expenses.uniformsWorkClothesToolsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def otherEquipmentAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"expenses.otherEquipmentAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"expenses.otherEquipmentAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"expenses.otherEquipmentAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def professionalFeesAndSubscriptionsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"expenses.professionalFeesAndSubscriptions.error.${if (isAgent) "agent" else "individual"}"
  )

  def otherEquipmentForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"expenses.otherEquipment.error.${if (isAgent) "agent" else "individual"}"
  )

  def professionalFeesAndSubscriptionsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"expenses.professionalFeesAndSubscriptionsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"expenses.professionalFeesAndSubscriptionsAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"expenses.professionalFeesAndSubscriptionsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )
}
