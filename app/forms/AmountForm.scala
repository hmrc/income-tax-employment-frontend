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

package forms

import forms.validation.mappings.MappingUtil._
import play.api.data.Form

object AmountForm {

  val amount = "amount"

  def amountForm(
                  emptyFieldKey: String,
                  wrongFormatKey: String = "common.error.invalid_currency_format",
                  exceedsMaxAmountKey: String = "common.error.amountMaxLimit",
                  emptyFieldArguments: Seq[String] = Seq.empty[String]
                ): Form[BigDecimal] = Form(
    amount -> currency(
      requiredKey = emptyFieldKey,
      wrongFormatKey = wrongFormatKey,
      maxAmountKey = exceedsMaxAmountKey,
      args = emptyFieldArguments
    )
  )

}
