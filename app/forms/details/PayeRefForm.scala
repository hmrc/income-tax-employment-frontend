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
import play.api.data.Form
import play.api.data.validation.Constraint

object PayeRefForm extends InputFilters {

  val payeRef: String = "payeRef"

  private val charRegex = "^$|^\\d{3}\\/[A-Za-z0-9 ]{1,10}$"

  private val validateFormat: Constraint[String] = validateChar(charRegex)("payeRef.errors.wrongFormat")

  def payeRefForm: Form[String] = Form(
    payeRef -> trimmedText.transform[String](filter, identity).verifying(validateFormat)
  )
}
