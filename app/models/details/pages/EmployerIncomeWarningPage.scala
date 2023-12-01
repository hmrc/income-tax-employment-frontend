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

package models.details.pages

import models.User
import models.mongo.EmploymentUserData

case class EmployerIncomeWarningPage(taxYear: Int,
                                     employmentId: String,
                                     employerName: String,
                                     isAgent: Boolean,
                                     continueUrl: String,
                                     cancelUrl: String)

object EmployerIncomeWarningPage {

  def apply(taxYear: Int,
            employmentId: String,
            user: User,
            employmentUserData: EmploymentUserData,
            continueUrl: String,
            cancelUrl: String): EmployerIncomeWarningPage = {

    EmployerIncomeWarningPage(
      taxYear = taxYear,
      employmentId = employmentId,
      employerName = employmentUserData.employment.employmentDetails.employerName,
      isAgent = user.isAgent,
      continueUrl = continueUrl,
      cancelUrl = cancelUrl
    )
  }
}
