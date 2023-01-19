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

package forms.benefits.accommodation

import forms.{AmountForm, YesNoForm}
import play.api.data.FormError
import support.UnitTest

class AccommodationFormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty

  private val underTest = new AccommodationFormsProvider()

  ".accommodationRelocationForm" should {
    "return a form that maps data when data is correct" in {
      underTest.accommodationRelocationForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.accommodationRelocationForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.accommodationRelocation.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.accommodationRelocationForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.accommodationRelocation.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.accommodationRelocationForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.accommodationRelocation.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.accommodationRelocationForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.accommodationRelocation.error.individual"), Seq())
        )
      }
    }
  }

  ".livingAccommodationForm" should {
    "return a form that maps data when data is correct" in {
      underTest.livingAccommodationForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.livingAccommodationForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.livingAccommodationBenefits.error.no-entry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.livingAccommodationForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.livingAccommodationBenefits.error.no-entry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.livingAccommodationForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.livingAccommodationBenefits.error.no-entry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.livingAccommodationForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.livingAccommodationBenefits.error.no-entry.individual"), Seq())
        )
      }
    }
  }

  ".livingAccommodationAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.livingAccommodationAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.livingAccommodationAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.livingAccommodationAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.livingAccommodationAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.livingAccommodationAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.livingAccommodationAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.livingAccommodationAmount.error.wrongFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.livingAccommodationAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.livingAccommodationAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.livingAccommodationAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.livingAccommodationAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.livingAccommodationAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.livingAccommodationAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.livingAccommodationAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.livingAccommodationAmount.error.wrongFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.livingAccommodationAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.livingAccommodationAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".nonQualifyingRelocationForm" should {
    "return a form that maps data when data is correct" in {
      underTest.nonQualifyingRelocationForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.nonQualifyingRelocationForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonQualifyingRelocationQuestion.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.nonQualifyingRelocationForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonQualifyingRelocationQuestion.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.nonQualifyingRelocationForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonQualifyingRelocationQuestion.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.nonQualifyingRelocationForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.nonQualifyingRelocationQuestion.error.individual"), Seq())
        )
      }
    }
  }

  ".nonQualifyingRelocationAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.nonQualifyingRelocationAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.nonQualifyingRelocationAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonQualifyingRelocationBenefitAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.nonQualifyingRelocationAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonQualifyingRelocationBenefitAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.nonQualifyingRelocationAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonQualifyingRelocationBenefitAmount.error.invalidFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.nonQualifyingRelocationAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonQualifyingRelocationBenefitAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.nonQualifyingRelocationAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonQualifyingRelocationBenefitAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.nonQualifyingRelocationAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonQualifyingRelocationBenefitAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.nonQualifyingRelocationAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonQualifyingRelocationBenefitAmount.error.invalidFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.nonQualifyingRelocationAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.nonQualifyingRelocationBenefitAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".qualifyingRelocationForm" should {
    "return a form that maps data when data is correct" in {
      underTest.qualifyingRelocationForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.qualifyingRelocationForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.qualifyingRelocationBenefits.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.qualifyingRelocationForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.qualifyingRelocationBenefits.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.qualifyingRelocationForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.qualifyingRelocationBenefits.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.qualifyingRelocationForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.qualifyingRelocationBenefits.error.individual"), Seq())
        )
      }
    }
  }

  ".qualifyingRelocationAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.qualifyingRelocationAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.qualifyingRelocationAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.qualifyingRelocationBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.qualifyingRelocationAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.qualifyingRelocationBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.qualifyingRelocationAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.qualifyingRelocationBenefitsAmount.error.wrongFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.qualifyingRelocationAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.qualifyingRelocationBenefitsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.qualifyingRelocationAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.qualifyingRelocationBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.qualifyingRelocationAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.qualifyingRelocationBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.qualifyingRelocationAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.qualifyingRelocationBenefitsAmount.error.wrongFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.qualifyingRelocationAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.qualifyingRelocationBenefitsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }
}
