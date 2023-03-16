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

package forms.validation

import forms.validation.utils.ConstraintUtil._
import play.api.data.validation.{Constraint, Invalid, Valid}

object StringConstraints {

  val charRegex = """^([ A-Za-z0-9&@£$€¥#.,:;-])*$"""

  def validateChar(charRegex: String, outputInvalidChars: Boolean = false): String => Constraint[String] = msgKey => constraint[String](
    x => if (x.matches(charRegex)) Valid else {
      if (outputInvalidChars) Invalid(msgKey, x.filterNot(c => charRegex.r.pattern.matcher(c.toString).matches()).distinct) else Invalid(msgKey)
    }
  )

  val nonEmpty: String => Constraint[String] = msgKey => constraint[String](
    x => if (x.isEmpty) Invalid(msgKey) else Valid
  )

  def validateSize(maxChars: Int): String => Constraint[String] = msgKey => constraint[String](
    x => if (x.length <= maxChars) Valid else Invalid(msgKey)
  )

}
