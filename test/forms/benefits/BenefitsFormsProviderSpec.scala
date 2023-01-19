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

package forms.benefits

import forms.YesNoForm
import play.api.data.FormError
import support.UnitTest

class BenefitsFormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val emptyData: Map[String, String] = Map.empty

  private val underTest = new BenefitsFormsProvider()

  ".receiveAnyBenefitsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.receiveAnyBenefitsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.receiveAnyBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("receiveAnyBenefits.errors.noRadioSelected.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.receiveAnyBenefitsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("receiveAnyBenefits.errors.noRadioSelected.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.receiveAnyBenefitsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("receiveAnyBenefits.errors.noRadioSelected.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.receiveAnyBenefitsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("receiveAnyBenefits.errors.noRadioSelected.individual"), Seq())
        )
      }
    }
  }
}
