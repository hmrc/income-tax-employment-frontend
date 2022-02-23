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

package forms.benefits.accommodation

import play.api.data.FormError
import support.UnitTest

class AccommodationRelocationBenefitsFormMapperTest extends UnitTest {

  private val emptyData = Map("value" -> "")
  private val wrongKeyData = Map("wrongKey" -> "true")

  private val underTest = new AccommodationRelocationBenefitsFormMapper()

  "yesNoForm." should {
    "return a form that maps data when data is correct" in {
      val anyBoolean = true
      val correctData = Map("value" -> "true")

      underTest.yesNoForm(isAgent = anyBoolean).bind(correctData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.yesNoForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.accommodationRelocation.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.yesNoForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.accommodationRelocation.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.yesNoForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.accommodationRelocation.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.yesNoForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.accommodationRelocation.error.individual"), Seq())
        )
      }
    }
  }
}
