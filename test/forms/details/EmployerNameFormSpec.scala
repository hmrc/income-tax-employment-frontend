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

import forms.details.EmployerNameForm.employerName
import play.api.data.{Form, FormError}
import support.UnitTest

class EmployerNameFormSpec extends UnitTest {

  private def underTest(isAgent: Boolean): Form[String] = EmployerNameForm.employerNameForm(isAgent)

  private lazy val testNameValid = "Google\\- _&`():.'^,"
  private lazy val testNameEmpty = ""
  private lazy val testNameTooBig = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"
  private lazy val testNameInvalidCharacters = "~~~"

  "EmployerNameFormSpec" should {
    "as an individual" should {
      "correctly validate a name" when {
        "a valid name is entered" in {
          val testInput = Map(employerName -> testNameValid)
          val expected = testNameValid
          val actual = underTest(isAgent = false).bind(testInput).value

          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty name" in {
        val testInput = Map(employerName -> testNameEmpty)
        val emptyTest = underTest(isAgent = false).bind(testInput)

        emptyTest.errors should contain(FormError(employerName, "employment.employerName.error.noEntry.individual"))
      }

      "invalidate a name that is too long" in {
        val testInput = Map(employerName -> testNameTooBig)
        val invalidLengthTest = underTest(isAgent = false).bind(testInput)

        invalidLengthTest.errors should contain(FormError(employerName, "employment.employerName.error.name.limit"))
      }

      "invalidate a name with invalid characters" in {
        val testInput = Map(employerName -> testNameInvalidCharacters)
        val invalidLengthTest = underTest(isAgent = false).bind(testInput)

        invalidLengthTest.errors should contain(FormError(employerName, "employment.employerName.error.name.wrongFormat.individual"))
      }
    }

    "as an agent" should {
      "correctly validate a name" when {
        "a valid name is entered" in {
          val testInput = Map(employerName -> testNameValid)
          val expected = testNameValid
          val actual = underTest(isAgent = true).bind(testInput).value

          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty name" in {
        val testInput = Map(employerName -> testNameEmpty)
        val emptyTest = underTest(isAgent = true).bind(testInput)

        emptyTest.errors should contain(FormError(employerName, "employment.employerName.error.noEntry.agent"))
      }

      "invalidate a name that is too long" in {
        val testInput = Map(employerName -> testNameTooBig)
        val invalidLengthTest = underTest(isAgent = true).bind(testInput)

        invalidLengthTest.errors should contain(FormError(employerName, "employment.employerName.error.name.limit"))
      }

      "invalidate a name with invalid characters" in {
        val testInput = Map(employerName -> testNameInvalidCharacters)
        val invalidLengthTest = underTest(isAgent = true).bind(testInput)

        invalidLengthTest.errors should contain(FormError(employerName, "employment.employerName.error.name.wrongFormat.agent"))
      }
    }
  }
}
