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

package support.builders.models.employment

import models.employment.HmrcEmploymentSource
import support.TaxYearUtils.taxYearEOY
import support.builders.models.employment.EmploymentFinancialDataBuilder.{aCustomerEmploymentFinancialData, aHmrcEmploymentFinancialData}

// TODO: Should only have one default method
object HmrcEmploymentSourceBuilder {

  val aHmrcEmploymentSource: HmrcEmploymentSource = HmrcEmploymentSource(
    employmentId = "employmentId",
    employerName = "maggie",
    employerRef = Some("223/AB12399"),
    payrollId = Some("12345678"),
    startDate = Some("2019-04-21"),
    cessationDate = Some(s"${taxYearEOY - 1}-03-11"),
    dateIgnored = None,
    submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"),
    hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData),
    None
  )

  val aHmrcEmploymentSourceWithCustomerAndHmrcFinancials: HmrcEmploymentSource = HmrcEmploymentSource(
    employmentId = "employmentId",
    employerName = "maggie",
    employerRef = Some("223/AB12399"),
    payrollId = Some("12345678"),
    startDate = Some("2019-04-21"),
    cessationDate = Some(s"${taxYearEOY - 1}-03-11"),
    dateIgnored = None,
    submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"),
    hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData),
    customerEmploymentFinancialData = Some(aCustomerEmploymentFinancialData)
  )
}
