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

package builders.models.employment

import builders.models.employment.PayBuilder.aPay
import models.employment.EmploymentData

object EmploymentDataBuilder {

  def anEmploymentData: EmploymentData = EmploymentData(
    submittedOn = "2021-01-01",
    employmentSequenceNumber = None,
    companyDirector = Some(true),
    closeCompany = Some(true),
    directorshipCeasedDate = None,
    occPen = Some(false),
    disguisedRemuneration = None,
    pay = Some(aPay),
    deductions = None
  )
}