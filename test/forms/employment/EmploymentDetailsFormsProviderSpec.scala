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
import play.api.data.FormError
import support.UnitTest

class EmploymentDetailsFormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val employerName = "some employer name"
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty

  private val underTest = new EmploymentDetailsFormsProvider()

  ".didYouLeaveForm" should {
    "return a form that maps data when data is correct" in {
      underTest.didYouLeaveForm(isAgent = anyBoolean, employerName = employerName).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.didYouLeaveForm(isAgent = true, employerName).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("employment.didYouLeave.error.agent"), Seq(employerName))
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.didYouLeaveForm(isAgent = true, employerName).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("employment.didYouLeave.error.agent"), Seq(employerName))
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.didYouLeaveForm(isAgent = false, employerName).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("employment.didYouLeave.error.individual"), Seq(employerName))
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.didYouLeaveForm(isAgent = false, employerName).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("employment.didYouLeave.error.individual"), Seq(employerName))
        )
      }
    }
  }

  ".employerPayAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.employerPayAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.employerPayAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employerPayAmount.error.empty.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.employerPayAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employerPayAmount.error.empty.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.employerPayAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employerPayAmount.error.wrongFormat"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.employerPayAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employerPayAmount.error.amountMaxLimit"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.employerPayAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employerPayAmount.error.empty.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.employerPayAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employerPayAmount.error.empty.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.employerPayAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employerPayAmount.error.wrongFormat"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.employerPayAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employerPayAmount.error.amountMaxLimit"), Seq())
        )
      }
    }
  }

  ".employmentTaxAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.employmentTaxAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.employmentTaxAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employment.employmentTax.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.employmentTaxAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employment.employmentTax.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.employmentTaxAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employment.employmentTax.error.format"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.employmentTaxAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employment.employmentTax.error.max"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.employmentTaxAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employment.employmentTax.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.employmentTaxAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employment.employmentTax.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.employmentTaxAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employment.employmentTax.error.format"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.employmentTaxAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("employment.employmentTax.error.max"), Seq())
        )
      }
    }
  }
}
