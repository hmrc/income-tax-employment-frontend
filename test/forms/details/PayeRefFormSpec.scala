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

package forms.details

import play.api.data.{Form, FormError}
import support.UnitTest

class PayeRefFormSpec extends UnitTest {

  private lazy val validPayeRef = List("123/AA12345", "123/AA1234", "123/AA123456", "123/AAAAAAAA", "123/A")
  private lazy val invalidPayeRefs = List("123AA12345", "A11/AA123456", "123/AA1234567890", "123/", "12/AA12345")

  private val underTest: Form[String] = PayeRefForm.payeRefForm

  ".payeRefForm" should {
    "allow empty form" in {
      val emptyFormData = Map[String, String]().empty

      underTest.bind(emptyFormData).errors shouldBe Seq.empty
    }

    "allow empty field" in {
      val emptyFieldFormData = Map(PayeRefForm.payeRef -> "")

      underTest.bind(emptyFieldFormData).errors shouldBe Seq.empty
    }

    "allow valid PAYE reference" in {
      validPayeRef.foreach { payeReference =>
        val formData = Map(PayeRefForm.payeRef -> payeReference)

        underTest.bind(formData).errors shouldBe Seq.empty
      }
    }

    "contain error when PAYE reference is invalid" in {
      invalidPayeRefs.foreach { payeReference =>
        val formData = Map(PayeRefForm.payeRef -> payeReference)

        underTest.bind(formData).errors should contain(FormError(PayeRefForm.payeRef, "payeRef.errors.wrongFormat"))
      }
    }
  }
}
