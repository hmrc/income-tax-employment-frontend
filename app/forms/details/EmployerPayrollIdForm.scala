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

import filters.InputFilters
import forms.validation.StringConstraints.{validateChar, validateSize}
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Form

object EmployerPayrollIdForm extends InputFilters {

  val payrollId: String = "payrollId"

  private val charLimit: Int = 38
  private val regex: String = "^[A-Za-z0-9.,\\\\\\-()\\/=!\"&*; <>'’+:\\?]{0,38}$"

  def employerPayrollIdForm(invalidCharactersKey: String,
                            tooManyCharactersKey: String): Form[String] = Form(
    payrollId -> trimmedText.transform[String](filter, identity).verifying(
      validateSize(charLimit)(tooManyCharactersKey)
        .andThen(validateChar(regex, outputInvalidChars = true)(invalidCharactersKey))
    )
  )
}
