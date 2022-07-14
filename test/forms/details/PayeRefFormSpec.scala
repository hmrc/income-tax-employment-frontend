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

package forms.details

import play.api.data.{Form, FormError}
import utils.UnitTest

class PayeRefFormSpec extends UnitTest {

  private val payeRefForm: Form[String] = PayeRefForm.payeRefForm
  private val payeRef = "payeRef"

  private lazy val validPayeRef = List("123/AA12345", "123/AA1234", "123/AA123456","123/AAAAAAAA", "123/A")
  private lazy val invalidPayeRefs = List("123AA12345",  "A11/AA123456", "123/AA1234567890", "123/", "12/AA12345")
  private lazy val testEmpty = ""


  "PayeRefFormSpec" should {
    "as an individual" should {
      "correctly validate a name" when {
        "a valid name is entered" in {
          validPayeRef.foreach { ref =>
            val testInput = Map(payeRef -> ref)
            val expected = ref
            val actual = payeRefForm.bind(testInput).value

            actual shouldBe Some(expected)
          }
        }
      }

      "invalidate a name in the incorrect format" in {
        invalidPayeRefs.foreach { ref =>
          val testInput = Map(payeRef -> ref)
          val invalidLengthTest = payeRefForm.bind(testInput)

          invalidLengthTest.errors should contain(FormError(payeRef, "payeRef.errors.wrongFormat"))
        }
      }

      "invalidate an empty name" in {
        val testInput = Map(payeRef -> testEmpty)
        val emptyTest = payeRefForm.bind(testInput)

        emptyTest.errors should contain(FormError(payeRef, "payeRef.errors.empty"))
      }
    }

    "as an agent" should {
      "correctly validate a name" when {
        "a valid name is entered" in {
          validPayeRef.foreach { ref =>
            val testInput = Map(payeRef -> ref)
            val expected = ref
            val actual = payeRefForm.bind(testInput).value

            actual shouldBe Some(expected)
          }
        }
      }

      "invalidate a name in the incorrect format" in {
        invalidPayeRefs.foreach { ref =>
          val testInput = Map(payeRef -> ref)
          val invalidLengthTest = payeRefForm.bind(testInput)
          invalidLengthTest.errors should contain(FormError(payeRef, "payeRef.errors.wrongFormat"))
        }
      }

      "invalidate an empty name" in {
        val testInput = Map(payeRef -> testEmpty)
        val emptyTest = payeRefForm.bind(testInput)
        emptyTest.errors should contain(FormError(payeRef, "payeRef.errors.empty"))
      }
    }
  }
}
