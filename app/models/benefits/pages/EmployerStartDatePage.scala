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
import models.employment.DateFormData
import models.mongo.EmploymentUserData
import play.api.data.Form

case class EmployerStartDatePage(taxYear: Int, employmentId: String, employerName: String, isAgent: Boolean, form: Form[DateFormData])

object EmployerStartDatePage {

  def apply(taxYear: Int,
            employmentId: String,
            user: User,
            form: Form[DateFormData],
            employmentUserData: EmploymentUserData): EmployerStartDatePage = {
    val optStartDate = employmentUserData.employment.employmentDetails.startDate

    EmployerStartDatePage(
      taxYear = taxYear,
      employmentId = employmentId,
      employerName = employmentUserData.employment.employmentDetails.employerName,
      isAgent = user.isAgent,
      form = optStartDate.fold(form)(startDate => if (form.hasErrors) form else form.fill(DateFormData(startDate)))
    )
  }
}
