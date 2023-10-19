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

import models.employment.EmploymentDetailsViewModel
import support.TaxYearUtils.taxYearEOY
import support.builders.models.employment.PayBuilder.aPay

object EmploymentDetailsViewModelBuilder {

  val anEmploymentDetailsViewModel: EmploymentDetailsViewModel = EmploymentDetailsViewModel(
    employerName = "maggie",
    employerRef = Some("223/AB12399"),
    payrollId = Some("12345678"),
    employmentId = "employmentId",
    startDate = Some("2019-04-21"),
    didYouLeaveQuestion = Some(true),
    cessationDate = Some(s"${taxYearEOY - 1}-03-11"),
    taxablePayToDate = aPay.taxablePayToDate,
    totalTaxToDate = aPay.totalTaxToDate,
    isUsingCustomerData = false,
    offPayrollWorkingStatus = Some(false))
}
