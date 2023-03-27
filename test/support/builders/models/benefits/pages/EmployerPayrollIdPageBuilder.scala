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

package support.builders.models.benefits.pages

import forms.details.EmploymentDetailsFormsProvider
import models.benefits.pages.EmployerPayrollIdPage
import support.TaxYearUtils
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails

object EmployerPayrollIdPageBuilder {

  val anEmployerPayrollIdPage: EmployerPayrollIdPage = EmployerPayrollIdPage(
    taxYear = TaxYearUtils.taxYearEOY,
    employmentId = "employmentId",
    employerName = anEmploymentDetails.employerName,
    employmentEnded = anEmploymentDetails.cessationDate.isDefined,
    isAgent = false,
    form = new EmploymentDetailsFormsProvider().employerPayrollIdForm()
  )
}
