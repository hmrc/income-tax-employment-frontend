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

package forms.testUtils

import forms.validation.StringConstraints
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.validation.{Constraints, Invalid, Valid}

class StringConstraintsSpec extends Constraints with AnyWordSpecLike with Matchers {

  val maxLength = 2
  val errMsgMaxLength = "Too Long"
  val errMsgNonEmpty = "it is empty"
  val errMsgInvalidChar = "there are invalid chars"
  val errMsgNoLeadingSpace = "there are leading spaces"
  val errMsgInvalidInt = "contains non numerical chars"

  "The StringConstraints.nonEmpty method" when {

    "supplied with empty value" should {

      "return invalid" in {
        StringConstraints.nonEmpty(errMsgNonEmpty)("") shouldBe Invalid(errMsgNonEmpty)
      }

    }

    "supplied with some value" should {

      "return valid" in {
        StringConstraints.nonEmpty(errMsgNonEmpty)("someValue") shouldBe Valid
      }

    }
  }

  "The StringConstraints.validateChar method" when {

    "supplied with a valid string" should {

      "return valid" in {
        val lowerCaseAlphabet = ('a' to 'z').mkString
        val upperCaseAlphabet = lowerCaseAlphabet.toUpperCase()
        val oneToNine = (1 to 9).mkString
        val otherChar = "&@£$€¥#.,:;-"
        val space = ""

        StringConstraints.validateChar(errMsgInvalidChar)(lowerCaseAlphabet + upperCaseAlphabet + space + oneToNine + otherChar + space) shouldBe Valid
      }
    }

    "supplied with a string which contains invalid characters" should {

      "return invalid" in {
        StringConstraints.validateChar(errMsgInvalidChar)("!()+{}?^~") shouldBe Invalid(errMsgInvalidChar)
      }

    }
  }

}
