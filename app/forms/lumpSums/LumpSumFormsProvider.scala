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

package forms.lumpSums

import forms.AmountForm
import play.api.data.Form

class LumpSumFormsProvider {
  def TaxableLumpSumAmountForm(isAgent: Boolean, employerName: String): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"lumpSum.taxableLumpSums.amount.error.noInput.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"lumpSum.taxableLumpSums.amount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"lumpSum.taxableLumpSums.amount.error.overMax.${if (isAgent) "agent" else "individual"}",
    underMinAmountKey = Some(s"lumpSum.taxableLumpSums.amount.error.underMin.${if (isAgent) "agent" else "individual"}"),
    emptyFieldArguments = Seq(employerName)
  )

}
