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

package models.benefits.pages

import models.User
import models.mongo.EmploymentUserData
import play.api.data.Form

case class EmployerPayrollIdPage(taxYear: Int, employmentId: String, employerName: String, employmentEnded: Boolean, isAgent: Boolean, form: Form[String])

object EmployerPayrollIdPage {

  def apply(taxYear: Int,
            employmentId: String,
            user: User,
            form: Form[String],
            employmentUserData: EmploymentUserData): EmployerPayrollIdPage = {
    val optPayrollId = employmentUserData.employment.employmentDetails.payrollId

    EmployerPayrollIdPage(
      taxYear = taxYear,
      employmentId = employmentId,
      employerName = employmentUserData.employment.employmentDetails.employerName,
      employmentEnded = employmentUserData.employment.employmentDetails.cessationDate.isDefined,
      isAgent = user.isAgent,
      form = optPayrollId.fold(form)(payrollId => if (form.hasErrors) form else form.fill(payrollId))
    )
  }
}
