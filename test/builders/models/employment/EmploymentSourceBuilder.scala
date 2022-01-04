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

package builders.models.employment

import builders.models.employment.EmploymentBenefitsBuilder.anEmploymentBenefits
import builders.models.employment.EmploymentDataBuilder.anEmploymentData
import models.employment.EmploymentSource

object EmploymentSourceBuilder {

  val anEmploymentSource: EmploymentSource = EmploymentSource(
    employmentId = "employmentId",
    employerName = "maggie",
    employerRef = Some("223/AB12399"),
    payrollId = Some("12345678"),
    startDate = Some("2019-04-21"),
    cessationDate = Some("2020-03-11"),
    dateIgnored = None,
    submittedOn = Some("2020-01-04T05:01:01Z"),
    employmentData = Some(anEmploymentData),
    employmentBenefits = Some(anEmploymentBenefits)
  )
}