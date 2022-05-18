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

import play.api.data.FormError
import support.UnitTest

class SelectEmployerFormSpec extends UnitTest {

  private val underTest = new SelectEmployerForm()

  private val id = "1234567890"

  "employerListForm" should {
    "return a form that maps data when data is correct" in {
      val correctData = Map("value" -> id)

      underTest.employerListForm(Seq(id)).bind(correctData).errors shouldBe Seq.empty
    }

    "return a form that contains error" which {
      "when id is not in the list" in {
        val invalidData = Map("value" -> "0126583475783478578345")
        underTest.employerListForm(Seq(id)).bind(invalidData).errors shouldBe Seq(
          FormError("value", Seq("employment.unignoreEmployment.error"), Seq())
        )
      }
      "when value is empty" in {
        val invalidData = Map("value" -> "")
        underTest.employerListForm(Seq(id)).bind(invalidData).errors shouldBe Seq(
          FormError("value", Seq("employment.unignoreEmployment.error"), Seq())
        )
      }
      "when key is invalid" in {
        val invalidData = Map("key" -> "")
        underTest.employerListForm(Seq(id)).bind(invalidData).errors shouldBe Seq(
          FormError("value", Seq("employment.unignoreEmployment.error"), Seq())
        )
      }
    }
  }
}
