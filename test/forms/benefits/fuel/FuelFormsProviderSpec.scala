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

package forms.benefits.fuel

import forms.{AmountForm, YesNoForm}
import play.api.data.FormError
import support.UnitTest

class FuelFormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty

  private val underTest = new FuelFormsProvider()

  ".carVanFuelForm" should {
    "return a form that maps data when data is correct" in {
      underTest.carVanFuelForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.carVanFuelForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.carVanFuel.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.carVanFuelForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.carVanFuel.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.carVanFuelForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.carVanFuel.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.carVanFuelForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.carVanFuel.error.individual"), Seq())
        )
      }
    }
  }

  ".carFuelAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.carFuelAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.carFuelAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.carFuelAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.carFuelAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.carFuelAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.carFuelAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.carFuelAmount.error.incorrectFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.carFuelAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.carFuelAmount.error.tooMuch.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.carFuelAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.carFuelAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.carFuelAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.carFuelAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.carFuelAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.carFuelAmount.error.incorrectFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.carFuelAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.carFuelAmount.error.tooMuch.individual"), Seq())
        )
      }
    }
  }

  ".companyCarForm" should {
    "return a form that maps data when data is correct" in {
      underTest.companyCarForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.companyCarForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("CompanyCarBenefits.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.companyCarForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("CompanyCarBenefits.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.companyCarForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("CompanyCarBenefits.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.companyCarForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("CompanyCarBenefits.error.individual"), Seq())
        )
      }
    }
  }

  ".companyCarAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.companyCarAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.companyCarAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyCarBenefitsAmount.error.no-entry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.companyCarAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyCarBenefitsAmount.error.no-entry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.companyCarAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyCarBenefitsAmount.error.incorrect-format.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.companyCarAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyCarBenefitsAmount.error.max-length.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.companyCarAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyCarBenefitsAmount.error.no-entry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.companyCarAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyCarBenefitsAmount.error.no-entry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.companyCarAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyCarBenefitsAmount.error.incorrect-format.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.companyCarAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyCarBenefitsAmount.error.max-length.individual"), Seq())
        )
      }
    }
  }

  ".companyCarFuelForm" should {
    "return a form that maps data when data is correct" in {
      underTest.companyCarFuelForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.companyCarFuelForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyCarFuelBenefits.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.companyCarFuelForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyCarFuelBenefits.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.companyCarFuelForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyCarFuelBenefits.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.companyCarFuelForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyCarFuelBenefits.error.individual"), Seq())
        )
      }
    }
  }

  ".companyVanForm" should {
    "return a form that maps data when data is correct" in {
      underTest.companyVanForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.companyVanForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyVanBenefits.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.companyVanForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyVanBenefits.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.companyVanForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyVanBenefits.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.companyVanForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyVanBenefits.error.individual"), Seq())
        )
      }
    }
  }

  ".companyVanAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.companyVanAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.companyVanAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanAmountBenefits.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.companyVanAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanAmountBenefits.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.companyVanAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanAmountBenefits.error.wrongFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.companyVanAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanAmountBenefits.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.companyVanAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanAmountBenefits.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.companyVanAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanAmountBenefits.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.companyVanAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanAmountBenefits.error.wrongFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.companyVanAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanAmountBenefits.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".companyVanFuelForm" should {
    "return a form that maps data when data is correct" in {
      underTest.companyVanFuelForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.companyVanFuelForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyVanFuelBenefits.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.companyVanFuelForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyVanFuelBenefits.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.companyVanFuelForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyVanFuelBenefits.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.companyVanFuelForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.companyVanFuelBenefits.error.individual"), Seq())
        )
      }
    }
  }

  ".companyVanFuelAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.companyVanFuelAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.companyVanFuelAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanFuelAmountBenefits.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.companyVanFuelAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanFuelAmountBenefits.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.companyVanFuelAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanFuelAmountBenefits.error.wrongFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.companyVanFuelAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanFuelAmountBenefits.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.companyVanFuelAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanFuelAmountBenefits.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.companyVanFuelAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanFuelAmountBenefits.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.companyVanFuelAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanFuelAmountBenefits.error.wrongFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.companyVanFuelAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.companyVanFuelAmountBenefits.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".receiveOwnCarMileageForm" should {
    "return a form that maps data when data is correct" in {
      underTest.receiveOwnCarMileageForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.receiveOwnCarMileageForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.receiveOwnCarMileageBenefit.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.receiveOwnCarMileageForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.receiveOwnCarMileageBenefit.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.receiveOwnCarMileageForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.receiveOwnCarMileageBenefit.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.receiveOwnCarMileageForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.receiveOwnCarMileageBenefit.error.individual"), Seq())
        )
      }
    }
  }

  ".mileageAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.mileageAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.mileageAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.mileageBenefitAmount.error.empty.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.mileageAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.mileageBenefitAmount.error.empty.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.mileageAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.mileageBenefitAmount.error.wrongFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.mileageAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.mileageBenefitAmount.error.amountMaxLimit.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.mileageAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.mileageBenefitAmount.error.empty.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.mileageAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.mileageBenefitAmount.error.empty.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.mileageAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.mileageBenefitAmount.error.wrongFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.mileageAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.mileageBenefitAmount.error.amountMaxLimit.individual"), Seq())
        )
      }
    }
  }
}
