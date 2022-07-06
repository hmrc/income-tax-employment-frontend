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

package forms.benefits.assets

import forms.{AmountForm, YesNoForm}
import play.api.data.FormError
import support.UnitTest

class AssetsFormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty

  private val underTest = new AssetsFormsProvider()

  ".assetsOrAssetTransfersForm" should {
    "return a form that maps data when data is correct" in {
      underTest.assetsOrAssetTransfersForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.assetsOrAssetTransfersForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assetsOrAssetTransfers.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.assetsOrAssetTransfersForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assetsOrAssetTransfers.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.assetsOrAssetTransfersForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assetsOrAssetTransfers.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.assetsOrAssetTransfersForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assetsOrAssetTransfers.error.individual"), Seq())
        )
      }
    }
  }

  ".assetsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.assetsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.assetsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assets.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.assetsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assets.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.assetsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assets.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.assetsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assets.error.individual"), Seq())
        )
      }
    }
  }

  ".assetsAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.assetsAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.assetsAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.assetsAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.assetsAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetsAmount.error.incorrectFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.assetsAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.assetsAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.assetsAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.assetsAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetsAmount.error.incorrectFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.assetsAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".assetTransfersForm" should {
    "return a form that maps data when data is correct" in {
      underTest.assetTransfersForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.assetTransfersForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assetTransfers.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.assetTransfersForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assetTransfers.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.assetTransfersForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assetTransfers.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.assetTransfersForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.assetTransfers.error.individual"), Seq())
        )
      }
    }
  }

  ".assetTransfersAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.assetTransfersAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.assetTransfersAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetTransfersAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.assetTransfersAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetTransfersAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.assetTransfersAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetTransfersAmount.error.incorrectFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.assetTransfersAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetTransfersAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.assetTransfersAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetTransfersAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.assetTransfersAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetTransfersAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.assetTransfersAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetTransfersAmount.error.incorrectFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.assetTransfersAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.assetTransfersAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }
}
