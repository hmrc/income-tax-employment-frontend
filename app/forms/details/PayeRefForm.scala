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

import filters.InputFilters
import forms.validation.StringConstraints.validateChar
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Form
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints.nonEmpty

object PayeRefForm extends InputFilters {

  private val payeRef: String = "payeRef"
  private val charRegex = "^\\d{3}\\/[A-Za-z0-9 ]{1,10}$"

  private val notEmpty: Constraint[String] = nonEmpty(errorMessage = "payeRef.errors.empty")

  private val validateFormat: Constraint[String] = validateChar(charRegex)("payeRef.errors.wrongFormat")

  def payeRefForm: Form[String] = Form(
    payeRef -> trimmedText.transform[String](filter, identity).verifying(
      notEmpty andThen validateFormat
    )
  )
}
