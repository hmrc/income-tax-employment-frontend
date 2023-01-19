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

package forms.expenses

import forms.{AmountForm, YesNoForm}
import play.api.data.FormError
import support.UnitTest

class ExpensesFormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty

  private val underTest = new ExpensesFormsProvider()

  ".businessTravelExpensesForm" should {
    "return a form that maps data when data is correct" in {
      underTest.businessTravelExpensesForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.businessTravelExpensesForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.businessTravelOvernightExpenses.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.businessTravelExpensesForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.businessTravelOvernightExpenses.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.businessTravelExpensesForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.businessTravelOvernightExpenses.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.businessTravelExpensesForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.businessTravelOvernightExpenses.error.individual"), Seq())
        )
      }
    }
  }

  ".claimEmploymentExpensesForm" should {
    "return a form that maps data when data is correct" in {
      underTest.claimEmploymentExpensesForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.claimEmploymentExpensesForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.claimEmploymentExpenses.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.claimEmploymentExpensesForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.claimEmploymentExpenses.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.claimEmploymentExpensesForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.claimEmploymentExpenses.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.claimEmploymentExpensesForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.claimEmploymentExpenses.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".businessTravelAndOvernightAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.businessTravelAndOvernightAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.businessTravelAndOvernightAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.businessTravelAndOvernightAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.businessTravelAndOvernightAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.businessTravelAndOvernightAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.businessTravelAndOvernightAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.businessTravelAndOvernightAmount.error.incorrectFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.businessTravelAndOvernightAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.businessTravelAndOvernightAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.businessTravelAndOvernightAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.businessTravelAndOvernightAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.businessTravelAndOvernightAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.businessTravelAndOvernightAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.businessTravelAndOvernightAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.businessTravelAndOvernightAmount.error.incorrectFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.businessTravelAndOvernightAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.businessTravelAndOvernightAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".uniformsWorkClothesToolsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.uniformsWorkClothesToolsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.uniformsWorkClothesToolsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.uniformsWorkClothesTools.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.uniformsWorkClothesToolsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.uniformsWorkClothesTools.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.uniformsWorkClothesToolsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.uniformsWorkClothesTools.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.uniformsWorkClothesToolsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.uniformsWorkClothesTools.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".uniformsWorkClothesToolsAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.uniformsWorkClothesToolsAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.uniformsWorkClothesToolsAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.uniformsWorkClothesToolsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.uniformsWorkClothesToolsAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.uniformsWorkClothesToolsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.uniformsWorkClothesToolsAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.uniformsWorkClothesToolsAmount.error.invalidFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.uniformsWorkClothesToolsAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.uniformsWorkClothesToolsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.uniformsWorkClothesToolsAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.uniformsWorkClothesToolsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.uniformsWorkClothesToolsAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.uniformsWorkClothesToolsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.uniformsWorkClothesToolsAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.uniformsWorkClothesToolsAmount.error.invalidFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.uniformsWorkClothesToolsAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.uniformsWorkClothesToolsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  "otherEquipmentAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.otherEquipmentAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.otherEquipmentAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.otherEquipmentAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.otherEquipmentAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.otherEquipmentAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.otherEquipmentAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.otherEquipmentAmount.error.invalidFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.otherEquipmentAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.otherEquipmentAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.otherEquipmentAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.otherEquipmentAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.otherEquipmentAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.otherEquipmentAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.otherEquipmentAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.otherEquipmentAmount.error.invalidFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.otherEquipmentAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.otherEquipmentAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  "professionalFeesAndSubscriptionsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.professionalFeesAndSubscriptionsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.professionalFeesAndSubscriptionsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.professionalFeesAndSubscriptions.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.professionalFeesAndSubscriptionsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.professionalFeesAndSubscriptions.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.professionalFeesAndSubscriptionsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.professionalFeesAndSubscriptions.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.professionalFeesAndSubscriptionsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.professionalFeesAndSubscriptions.error.individual"), Seq())
        )
      }
    }
  }

  "otherEquipmentForm" should {
    "return a form that maps data when data is correct" in {
      underTest.otherEquipmentForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.otherEquipmentForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.otherEquipment.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.otherEquipmentForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.otherEquipment.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.otherEquipmentForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.otherEquipment.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.otherEquipmentForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("expenses.otherEquipment.error.individual"), Seq())
        )
      }
    }
  }

  "professionalFeesAndSubscriptionsAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.professionalFeesAndSubscriptionsAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.professionalFeesAndSubscriptionsAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.professionalFeesAndSubscriptionsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.professionalFeesAndSubscriptionsAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.professionalFeesAndSubscriptionsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.professionalFeesAndSubscriptionsAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.professionalFeesAndSubscriptionsAmount.error.invalidFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.professionalFeesAndSubscriptionsAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.professionalFeesAndSubscriptionsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.professionalFeesAndSubscriptionsAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.professionalFeesAndSubscriptionsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.professionalFeesAndSubscriptionsAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.professionalFeesAndSubscriptionsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.professionalFeesAndSubscriptionsAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.professionalFeesAndSubscriptionsAmount.error.invalidFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.professionalFeesAndSubscriptionsAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("expenses.professionalFeesAndSubscriptionsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }
}
