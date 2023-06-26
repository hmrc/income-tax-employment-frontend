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

package models.benefits.pages

import controllers.employment.routes
import models.employment.TaxableLumpSumViewModel
import play.api.i18n.Messages
import play.api.mvc.Call
import java.text.NumberFormat
import java.util.Locale

case class ListRows(amount: String, call: Call)

case class TaxableLumpSumListPage(rows: Seq[ListRows], taxYear: Int)

object TaxableLumpSumListPage {
  def apply(taxableLumpSumViewModel: TaxableLumpSumViewModel, taxYear: Int)(implicit messages: Messages) : TaxableLumpSumListPage = {
    TaxableLumpSumListPage(taxableLumpSumViewModel.items.map{ item =>
      ListRows(
        displayedValueForOptionalAmount(item.lumpSumAmount),
        routes.EmploymentSummaryController.show(taxYear) //todo redirect to appropriate page
      )}, taxYear
    )
  }

  def displayedValueForOptionalAmount(valueOpt: Option[BigDecimal]): String = valueOpt.map(displayedValue).getOrElse("")

  def displayedValue(value: BigDecimal): String =  formatNoZeros(value)

  def formatNoZeros(amount: BigDecimal): String = {
    NumberFormat.getCurrencyInstance(Locale.UK).format(amount)
      .replaceAll("\\.00", "")
  }
}

