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
import play.api.data.FormError
import support.UnitTest

class ReimbursedFormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty

  private val underTest = new ReimbursedFormsProvider()

  ".nonCashForm" should {
    "return a form that maps data when data is correct" in {
      underTest.nonCashForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.nonCashForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonCashBenefits.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.nonCashForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonCashBenefits.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.nonCashForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonCashBenefits.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.nonCashForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonCashBenefits.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".nonCashAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.nonCashAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.nonCashAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonCashBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.nonCashAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonCashBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.nonCashAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonCashBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.nonCashAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonCashBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }
    }

    "when data is wrongFormat" in {
      underTest.nonCashAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
        FormError(AmountForm.amount, Seq("benefits.nonCashBenefitsAmount.error.incorrectFormat"), Seq())
      )
    }

    "when data is overMaximum" in {
      underTest.nonCashAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
        FormError(AmountForm.amount, Seq("benefits.nonCashBenefitsAmount.error.overMaximum"), Seq())
      )
    }
  }

  ".nonTaxableCostsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.nonTaxableCostsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.nonTaxableCostsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonTaxableCosts.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.nonTaxableCostsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonTaxableCosts.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.nonTaxableCostsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonTaxableCosts.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.nonTaxableCostsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonTaxableCosts.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".nonTaxableCostsAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.nonTaxableCostsAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.nonTaxableCostsAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonTaxableCostsBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.nonTaxableCostsAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonTaxableCostsBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.nonTaxableCostsAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonTaxableCostsBenefitsAmount.error.incorrectFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.nonTaxableCostsAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonTaxableCostsBenefitsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.nonTaxableCostsAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonTaxableCostsBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.nonTaxableCostsAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonTaxableCostsBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.nonTaxableCostsAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonTaxableCostsBenefitsAmount.error.incorrectFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.nonTaxableCostsAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonTaxableCostsBenefitsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".otherBenefitsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.otherBenefitsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.otherBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.otherBenefits.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.otherBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.otherBenefits.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.otherBenefitsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.otherBenefits.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.otherBenefitsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.otherBenefits.error.individual"), Seq())
        )
      }
    }
  }

  ".otherBenefitsAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.otherBenefitsAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.otherBenefitsAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.otherBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.otherBenefitsAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.otherBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.otherBenefitsAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.otherBenefitsAmount.error.incorrectFormat.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.otherBenefitsAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.otherBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.otherBenefitsAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.otherBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.otherBenefitsAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.otherBenefitsAmount.error.incorrectFormat.individual"), Seq())
        )
      }
    }

    "return a form with error when data is overMaximum" in {
      underTest.otherBenefitsAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
        FormError(AmountForm.amount, Seq("benefits.otherBenefitsAmount.error.overMaximum"), Seq())
      )
    }
  }

  ".vouchersAndNonCashForm" should {
    "return a form that maps data when data is correct" in {
      underTest.vouchersAndNonCashForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.vouchersAndNonCashForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.reimbursedCostsVouchersAndNonCash.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.vouchersAndNonCashForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.reimbursedCostsVouchersAndNonCash.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.vouchersAndNonCashForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.reimbursedCostsVouchersAndNonCash.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.vouchersAndNonCashForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.reimbursedCostsVouchersAndNonCash.error.individual"), Seq())
        )
      }
    }
  }

  ".taxableCostsAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.taxableCostsAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.taxableCostsAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.taxableCostsBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.taxableCostsAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.taxableCostsBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.taxableCostsAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.taxableCostsBenefitsAmount.error.incorrectFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.taxableCostsAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.taxableCostsBenefitsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.taxableCostsAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.taxableCostsBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.taxableCostsAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.taxableCostsBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.taxableCostsAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.taxableCostsBenefitsAmount.error.incorrectFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.taxableCostsAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.taxableCostsBenefitsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".taxableCostsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.taxableCostsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.taxableCostsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.taxableCosts.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.taxableCostsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.taxableCosts.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.taxableCostsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.taxableCosts.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.taxableCostsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.taxableCosts.error.individual"), Seq())
        )
      }
    }
  }

  ".vouchersForm" should {
    "return a form that maps data when data is correct" in {
      underTest.vouchersForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.vouchersForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.vouchersBenefits.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.vouchersForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.vouchersBenefits.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.vouchersForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.vouchersBenefits.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.vouchersForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.vouchersBenefits.error.individual"), Seq())
        )
      }
    }
  }

  ".vouchersAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.vouchersAmountForm.bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains error" when {
      "key is wrong" in {
        underTest.vouchersAmountForm.bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.vouchersBenefitsAmount.error.noEntry"), Seq())
        )
      }

      "data is empty" in {
        underTest.vouchersAmountForm.bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.vouchersBenefitsAmount.error.noEntry"), Seq())
        )
      }

      "data is wrongFormat" in {
        underTest.vouchersAmountForm.bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.vouchersBenefitsAmount.error.incorrectFormat"), Seq())
        )
      }

      "data is overMaximum" in {
        underTest.vouchersAmountForm.bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.vouchersBenefitsAmount.error.overMaximum"), Seq())
        )
      }
    }
  }
}
