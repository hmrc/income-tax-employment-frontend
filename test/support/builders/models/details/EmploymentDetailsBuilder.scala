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

package support.builders.models.details

import models.details.EmploymentDetails
import support.TaxYearUtils.taxYearEOY

object EmploymentDetailsBuilder {

  val anEmploymentDetails: EmploymentDetails = EmploymentDetails(
    employerName = "Employer Name",
    employerRef = Some("123/12345"),
    startDate = Some(s"${taxYearEOY - 1}-11-11"),
    payrollId = Some("12345678"),
    didYouLeaveQuestion = Some(false),
    cessationDate = None,
    dateIgnored = None,
    employmentSubmittedOn = Some(s"$taxYearEOY-04-04T01:01:01Z"),
    employmentDetailsSubmittedOn = Some(s"$taxYearEOY-04-04T01:01:01Z"),
    taxablePayToDate = Some(55.99),
    totalTaxToDate = Some(3453453.00),
    currentDataIsHmrcHeld = false,
    offPayrollWorkingStatus = None
  )
}
