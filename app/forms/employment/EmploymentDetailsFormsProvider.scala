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

package forms.employment

import forms.{AmountForm, YesNoForm}
import play.api.data.Form

class EmploymentDetailsFormsProvider {

  def didYouLeaveForm(isAgent: Boolean, employerName: String): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"employment.didYouLeave.error.${if (isAgent) "agent" else "individual"}", Seq(employerName)
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
}
