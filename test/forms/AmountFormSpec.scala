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

import forms.AmountForm._
import play.api.data.{Form, FormError}
import utils.UnitTest

class AmountFormSpec extends UnitTest {

  def theForm(): Form[BigDecimal] = {
    amountForm("nothing to see here", "this not good", "too big")
  }

  val testCurrencyValid = 1000
  val testCurrencyWithSpaces = "100 0. 00"
  val testCurrencyEmpty = ""
  val testCurrencyInvalidInt = "!"
  val testCurrencyInvalidFormat = 12345.123
  val testCurrencyTooBig = "100000000000.00"

  "The AmountForm" should {

    "correctly validate a currency" when {

      "a valid currency is entered" in {
        val testInput = Map(amount -> testCurrencyValid.toString)
        val expected = testCurrencyValid
        val actual = theForm().bind(testInput).value
        actual shouldBe Some(expected)
      }
    }

    "correctly validate a currency with spaces" when {

      "a valid currency is entered" in {
        val testInput = Map(amount -> testCurrencyWithSpaces.toString)
        val expected = testCurrencyValid
        val actual = theForm().bind(testInput).value
        actual shouldBe Some(expected)
      }
    }

    "invalidate an empty currency" in {
      val testInput = Map(amount -> testCurrencyEmpty)
      val emptyTest = theForm().bind(testInput)
      emptyTest.errors should contain(FormError(amount, "nothing to see here"))
    }

    "invalidate currency that includes invalid characters" in {
      val testInput = Map(amount -> testCurrencyInvalidInt)
      val invalidCharTest = theForm().bind(testInput)
      invalidCharTest.errors should contain(FormError(amount, "this not good"))
    }

    "invalidate a currency that has incorrect formatting" in {
      val testInput = Map(amount -> testCurrencyInvalidFormat.toString)
      val invalidFormatTest = theForm().bind(testInput)
      invalidFormatTest.errors should contain(FormError(amount, "this not good"))
    }

    "invalidate a currency that is too big" in {
      val testInput = Map(amount -> testCurrencyTooBig)
      val bigCurrencyTest = theForm().bind(testInput)
      bigCurrencyTest.errors should contain(FormError(amount, "too big"))
    }
  }
}
