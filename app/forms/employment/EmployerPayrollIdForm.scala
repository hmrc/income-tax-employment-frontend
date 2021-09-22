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

import forms.validation.StringConstraints.{validateChar, validateMinSize, validateSize}
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Form
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints.nonEmpty

object EmployerPayrollIdForm {

  val payrollId: String = "payrollId"
  val minCharLimit: Int = 3
  val charLimit: Int = 8
  val regex: String = "^[A-Za-z0-9.,\\-()/=!\"%&*; <>'+:\\?]{0,38}$"

  def notEmpty(isAgent: Boolean): Constraint[String] =
    nonEmpty(if(isAgent) "employment.payrollId.error.noEntry.agent" else "employment.payrollId.error.noEntry.individual")

  def notCharLimit(isAgent: Boolean): Constraint[String] =
    validateSize(charLimit)(if(isAgent) "employment.payrollId.error.tooMany.agent" else "employment.payrollId.error.tooMany.individual")

  def notMinCharLimit(isAgent: Boolean): Constraint[String] =
    validateMinSize(minCharLimit)(if(isAgent) "employment.payrollId.error.notEnough.agent" else "employment.payrollId.error.notEnough.individual")

  def validateFormat(isAgent: Boolean): Constraint[String] =
    validateChar(regex)(if(isAgent) "employment.payrollId.error.incorrect.agent" else "employment.payrollId.error.incorrect.individual")

  def employerPayrollIdForm(isAgent: Boolean): Form[String] = Form(
    payrollId -> trimmedText.verifying(
      notEmpty(isAgent) andThen notMinCharLimit(isAgent) andThen notCharLimit(isAgent) andThen validateFormat(isAgent)
    )
  )

}
