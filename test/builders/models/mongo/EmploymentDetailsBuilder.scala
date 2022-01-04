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

package builders.models.mongo

import models.mongo.EmploymentDetails

object EmploymentDetailsBuilder {

  val anEmploymentDetails: EmploymentDetails = EmploymentDetails(
    "Employer Name",
    employerRef = Some("123/12345"),
    startDate = Some("2020-11-11"),
    taxablePayToDate = Some(55.99),
    totalTaxToDate = Some(3453453.00),
    employmentSubmittedOn = Some("2020-04-04T01:01:01Z"),
    employmentDetailsSubmittedOn = Some("2020-04-04T01:01:01Z"),
    currentDataIsHmrcHeld = false
  )
}
