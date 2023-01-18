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

package forms.studentLoans

import forms.AmountForm
import play.api.data.FormError
import support.UnitTest

class StudentLoansFormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmountData:  Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty
  private val employerName: String = "Employer"

  private val underTest = new StudentLoansFormsProvider()

  ".uglAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.uglAmountForm(employerName = employerName, isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq()
    }

    "return a form which contains agent error" which {
      "when isAgent is true and the key is wrong" in {
        underTest.uglAmountForm(employerName = employerName, isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.undergraduateLoanAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.uglAmountForm(employerName = employerName, isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.undergraduateLoanAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is in the wrong format" in {
        underTest.uglAmountForm(employerName = employerName, isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.undergraduateLoanAmount.error.invalidFormat"), Seq(employerName))
        )
      }

      "when isAgent is true and data is over the maximum amount" in {
        underTest.uglAmountForm(employerName = employerName, isAgent = true).bind(overMaximumAmountData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("common.error.amountMaxLimit"), Seq(employerName))
        )
      }
    }

    "return a form that contains an individual error" which {
      "when isAgent is false and the key is wrong" in {
        underTest.uglAmountForm(employerName = employerName, isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.undergraduateLoanAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and the data is empty" in {
        underTest.uglAmountForm(employerName = employerName, isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.undergraduateLoanAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is in the wrong format" in {
        underTest.uglAmountForm(employerName = employerName, isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.undergraduateLoanAmount.error.invalidFormat"), Seq(employerName))
        )
      }

      "when isAgent is false and data is over the maximum amount" in {
        underTest.uglAmountForm(employerName = employerName, isAgent = false).bind(overMaximumAmountData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("common.error.amountMaxLimit"), Seq(employerName))
        )
      }
    }
  }

  ".pglAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.pglAmountForm(employerName = employerName, isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq()
    }

    "return a form which contains agent error" which {
      "when isAgent is true and the key is wrong" in {
        underTest.pglAmountForm(employerName = employerName, isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.postgraduateLoanAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.pglAmountForm(employerName = employerName, isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.postgraduateLoanAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is in the wrong format" in {
        underTest.pglAmountForm(employerName = employerName, isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.postgraduateLoanAmount.error.invalidFormat"), Seq(employerName))
        )
      }

      "when isAgent is true and data is over the maximum amount" in {
        underTest.pglAmountForm(employerName = employerName, isAgent = true).bind(overMaximumAmountData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("common.error.amountMaxLimit"), Seq(employerName))
        )
      }
    }

    "return a form that contains an individual error" which {
      "when isAgent is false and the key is wrong" in {
        underTest.pglAmountForm(employerName = employerName, isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.postgraduateLoanAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and the data is empty" in {
        underTest.pglAmountForm(employerName = employerName, isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.postgraduateLoanAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is in the wrong format" in {
        underTest.pglAmountForm(employerName = employerName, isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("studentLoans.postgraduateLoanAmount.error.invalidFormat"), Seq(employerName))
        )
      }

      "when isAgent is false and data is over the maximum amount" in {
        underTest.pglAmountForm(employerName = employerName, isAgent = false).bind(overMaximumAmountData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("common.error.amountMaxLimit"), Seq(employerName))
        )
      }
    }
  }
}
