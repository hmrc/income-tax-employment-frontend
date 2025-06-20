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

package models.otheremployment.pages

import controllers.lumpSum.routes
import models.otheremployment.session.OtherEmploymentIncomeCYAModel
import play.api.mvc.Call

import java.text.NumberFormat
import java.util.Locale

case class ListRows(amount: String, call: Call)

case class TaxableLumpSumListPage(rows: Seq[ListRows], taxYear: Int, employmentId: String)

object TaxableLumpSumListPage {
  def apply(otherEmploymentIncomeCYAModel: OtherEmploymentIncomeCYAModel, taxYear: Int, employmentId: String):
  TaxableLumpSumListPage = {
    TaxableLumpSumListPage(otherEmploymentIncomeCYAModel.taxableLumpSums.zipWithIndex.map { item =>
      ListRows(
        displayedValue(item._1.amount),
        routes.TaxableLumpSumAmountController.show(taxYear, employmentId, Some(item._2))
      )
    }, taxYear, employmentId
    )
  }

  def displayedValue(value: BigDecimal): String = formatNoZeros(value)

  def formatNoZeros(amount: BigDecimal): String = {
    NumberFormat.getCurrencyInstance(Locale.UK).format(amount)
      .replaceAll("\\.00", "")
  }
}

