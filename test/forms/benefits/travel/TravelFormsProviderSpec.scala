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

package forms.benefits.travel

import forms.{AmountForm, YesNoForm}
import play.api.data.FormError
import support.UnitTest

class TravelFormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty

  private val underTest = new TravelFormsProvider()

  ".travelOrEntertainmentBenefitsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.travelOrEntertainmentBenefitsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.travelOrEntertainmentBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.travelOrEntertainment.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.travelOrEntertainmentBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.travelOrEntertainment.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.travelOrEntertainmentBenefitsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.travelOrEntertainment.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.travelOrEntertainmentBenefitsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.travelOrEntertainment.error.individual"), Seq())
        )
      }
    }
  }

  ".entertainingBenefitsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.entertainingBenefitsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.entertainingBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.entertainingBenefits.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.entertainingBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.entertainingBenefits.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.entertainingBenefitsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.entertainingBenefits.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.entertainingBenefitsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.entertainingBenefits.error.individual"), Seq())
        )
      }
    }
  }

  ".travelAndSubsistenceBenefitsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.travelAndSubsistenceBenefitsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.travelAndSubsistenceBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.travelAndSubsistence.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.travelAndSubsistenceBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.travelAndSubsistence.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.travelAndSubsistenceBenefitsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.travelAndSubsistence.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.travelAndSubsistenceBenefitsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.travelAndSubsistence.error.individual"), Seq())
        )
      }
    }
  }

  ".incidentalOvernightCostEmploymentBenefitsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.incidentalOvernightCostEmploymentBenefitsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.incidentalOvernightCostEmploymentBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.incidentalOvernightCostEmploymentBenefits.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.incidentalOvernightCostEmploymentBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.incidentalOvernightCostEmploymentBenefits.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.incidentalOvernightCostEmploymentBenefitsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.incidentalOvernightCostEmploymentBenefits.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.incidentalOvernightCostEmploymentBenefitsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.incidentalOvernightCostEmploymentBenefits.error.individual"), Seq())
        )
      }
    }
  }

  ".travelOrSubsistenceBenefitsAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.travelOrSubsistenceBenefitsAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.travelOrSubsistenceBenefitsAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.travelOrSubsistenceBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.travelOrSubsistenceBenefitsAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.travelOrSubsistenceBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.travelOrSubsistenceBenefitsAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.travelOrSubsistenceBenefitsAmount.error.wrongFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.travelOrSubsistenceBenefitsAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.travelOrSubsistenceBenefitsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.travelOrSubsistenceBenefitsAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.travelOrSubsistenceBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.travelOrSubsistenceBenefitsAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.travelOrSubsistenceBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.travelOrSubsistenceBenefitsAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.travelOrSubsistenceBenefitsAmount.error.wrongFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.travelOrSubsistenceBenefitsAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.travelOrSubsistenceBenefitsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".incidentalCostsBenefitsAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.incidentalCostsBenefitsAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.incidentalCostsBenefitsAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.incidentalCostsBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.incidentalCostsBenefitsAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.incidentalCostsBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.incidentalCostsBenefitsAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.incidentalCostsBenefitsAmount.error.incorrectFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.incidentalCostsBenefitsAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.incidentalCostsBenefitsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.incidentalCostsBenefitsAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.incidentalCostsBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.incidentalCostsBenefitsAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.incidentalCostsBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.incidentalCostsBenefitsAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.incidentalCostsBenefitsAmount.error.incorrectFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.incidentalCostsBenefitsAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.incidentalCostsBenefitsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".entertainmentBenefitsAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.entertainmentBenefitsAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.entertainmentBenefitsAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.entertainmentBenefitAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.entertainmentBenefitsAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.entertainmentBenefitAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.entertainmentBenefitsAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.entertainmentBenefitAmount.error.invalidFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.entertainmentBenefitsAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.entertainmentBenefitAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.entertainmentBenefitsAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.entertainmentBenefitAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.entertainmentBenefitsAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.entertainmentBenefitAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.entertainmentBenefitsAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.entertainmentBenefitAmount.error.invalidFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.entertainmentBenefitsAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.entertainmentBenefitAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }
}
