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

package forms.lumpSum

import forms.AmountForm
import forms.lumpSums.LumpSumFormsProvider
import play.api.data.FormError
import support.UnitTest

class LumpSumFormsProviderSpec extends UnitTest {
  private val amount: String = 123.0.toString
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,001")
  private val underMinimum: Map[String, String] = Map(AmountForm.amount -> "-1")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty

  private val employerName = "employerName"

  private val underTest = new LumpSumFormsProvider()

  "TaxableLumpSumAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.taxableLumpSumAmountForm(isAgent = true, employerName).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.taxableLumpSumAmountForm(isAgent = true, employerName).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("lumpSum.taxableLumpSums.amount.error.noInput.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.taxableLumpSumAmountForm(isAgent = true, employerName).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("lumpSum.taxableLumpSums.amount.error.noInput.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.taxableLumpSumAmountForm(isAgent = true, employerName).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("lumpSum.taxableLumpSums.amount.error.invalidFormat.agent"), Seq(employerName))
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.taxableLumpSumAmountForm(isAgent = true, employerName).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("lumpSum.taxableLumpSums.amount.error.overMax.agent"), Seq())
        )
      }

      "when isAgent is true and data is underMinimum" in {
        underTest.taxableLumpSumAmountForm(isAgent = true, employerName).bind(underMinimum).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("lumpSum.taxableLumpSums.amount.error.underMin.agent"), Seq())
        )
      }
    }
  }
}
