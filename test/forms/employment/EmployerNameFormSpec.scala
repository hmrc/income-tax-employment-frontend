/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.employment.EmployerNameForm.employerName
import play.api.data.{Form, FormError}
import utils.UnitTest

class EmployerNameFormSpec extends UnitTest {

  def form(previousNames: List[String] ,isAgent: Boolean): Form[String] = {
    EmployerNameForm.employerNameForm(previousNames, isAgent)
  }

  lazy val testNameValid = "Google"
  lazy val testNameEmpty = ""
  lazy val testNameTooBig = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"
  lazy val emptyPreviousNames = List("")
  lazy val previousNames = List("Google")

  "EmployerNameFormSpec" should {

    "as an individual" should {

      "correctly validate a name" when {

        "a valid name is entered" in {
          val testInput = Map(employerName -> testNameValid)
          val expected = testNameValid
          val actual = form(emptyPreviousNames, isAgent = false).bind(testInput).value

          actual shouldBe Some(expected)

        }

      }

      "invalidate an empty name" in {
        val testInput = Map(employerName -> testNameEmpty)

        val emptyTest = form(emptyPreviousNames, isAgent = false).bind(testInput)
        emptyTest.errors should contain(FormError(employerName, "employment.employerName.error.noEntry.individual"))

      }

      "invalidate a name that is too long" in {
        val testInput = Map(employerName -> testNameTooBig)

        val invalidLengthTest = form(emptyPreviousNames, isAgent = false).bind(testInput)
        invalidLengthTest.errors should contain(FormError(employerName, "employment.employerName.error.name.limit"))

      }

      "invalidate aa name that is a duplicate" in {
        val testInput = Map(employerName -> testNameValid)

        val duplicateNameTest = form(previousNames, isAgent = true).bind(testInput)
        duplicateNameTest.errors should contain(FormError(employerName, "employment.employerName.error.name.duplicate"))
      }

    }

    "as an agent" should {

      "correctly validate a name" when {

        "a valid name is entered" in {
          val testInput = Map(employerName -> testNameValid)
          val expected = testNameValid
          val actual = form(emptyPreviousNames, isAgent = true).bind(testInput).value

          actual shouldBe Some(expected)

        }

      }

      "invalidate an empty name" in {
        val testInput = Map(employerName -> testNameEmpty)

        val emptyTest = form(emptyPreviousNames, isAgent = true).bind(testInput)
        emptyTest.errors should contain(FormError(employerName, "employment.employerName.error.noEntry.agent"))

      }

      "invalidate a name that is too long" in {
        val testInput = Map(employerName -> testNameTooBig)

        val invalidLengthTest = form(emptyPreviousNames, isAgent = true).bind(testInput)
        invalidLengthTest.errors should contain(FormError(employerName, "employment.employerName.error.name.limit"))

      }

      "invalidate aa name that is a duplicate" in {
        val testInput = Map(employerName -> testNameValid)

        val duplicateNameTest = form(previousNames, isAgent = true).bind(testInput)
        duplicateNameTest.errors should contain(FormError(employerName, "employment.employerName.error.name.duplicate"))
      }

    }

  }

}
