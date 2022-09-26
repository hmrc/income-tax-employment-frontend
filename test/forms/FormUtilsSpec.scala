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

package forms

import forms.AmountForm.amountForm
import play.api.data.Form
import support.UnitTest

class FormUtilsSpec extends UnitTest {

  object Test extends FormUtils

  def theForm(): Form[BigDecimal] = {
    amountForm("nothing to see here", "this not good", "too big")
  }

  "The form" should {
    "check form is filled with cya amount" in {
      val cya: Option[BigDecimal] = Some(166.66)
      val form = Test.fillForm(theForm(), cya)
      val result = form.value
      result shouldBe Some(166.66)
    }
    "check form is not filled with cya amount" in {
      val cya: Option[BigDecimal] = None
      val form = Test.fillForm(theForm(), cya)
      val result = form.value
      result shouldBe None
    }
  }
}