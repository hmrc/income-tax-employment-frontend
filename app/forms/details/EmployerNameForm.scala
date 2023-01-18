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
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints.nonEmpty

object EmployerNameForm extends InputFilters {

  val employerName: String = "name"
  val charLimit: Int = 74
  val regex: String = "^[0-9a-zA-Z\\\\{À-˿’}\\- _&`():.'^,]{1,74}$"

  def notEmpty(isAgent: Boolean): Constraint[String] =
    nonEmpty(s"employment.employerName.error.noEntry.${if (isAgent) "agent" else "individual"}")

  val NotCharLimit: Constraint[String] = validateSize(charLimit)("employment.employerName.error.name.limit")

  def validateFormat(isAgent: Boolean): Constraint[String] =
    validateChar(regex)(s"employment.employerName.error.name.wrongFormat.${if (isAgent) "agent" else "individual"}")

  def employerNameForm(isAgent: Boolean): Form[String] = Form(
    employerName -> trimmedText.transform[String](filter, identity).verifying(
      notEmpty(isAgent) andThen NotCharLimit andThen validateFormat(isAgent)
    )
  )

}
