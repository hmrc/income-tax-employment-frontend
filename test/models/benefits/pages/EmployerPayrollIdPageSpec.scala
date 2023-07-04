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

import forms.details.EmploymentDetailsFormsProvider
import support.builders.models.UserBuilder.aUser
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.{ControllerUnitTest, TaxYearProvider}

class EmployerPayrollIdPageSpec extends ControllerUnitTest
  with TaxYearProvider {

  private val employmentId = "employmentId"
  private val payrollIdForm = new EmploymentDetailsFormsProvider().employerPayrollIdForm()

  ".apply(...)" should {
    "return page with empty form when payrollId is not preset" in {
      val employmentDetails = anEmploymentDetails.copy(payrollId = None)
      val userData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentDetails = employmentDetails))

      EmployerPayrollIdPage.apply(taxYearEOY, employmentId, aUser, payrollIdForm, userData) shouldBe EmployerPayrollIdPage(
        taxYear = taxYearEOY,
        employmentId = employmentId,
        employerName = userData.employment.employmentDetails.employerName,
        employmentEnded = userData.employment.employmentDetails.cessationDate.isDefined,
        isAgent = aUser.isAgent,
        form = payrollIdForm
      )
    }

    "return page with pre-filled form when payrollId is preset" in {
      val employmentDetails = anEmploymentDetails.copy(payrollId = Some("some-payroll-id"))
      val userData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentDetails = employmentDetails))

      EmployerPayrollIdPage.apply(taxYearEOY, employmentId, aUser, payrollIdForm, userData) shouldBe EmployerPayrollIdPage(
        taxYear = taxYearEOY,
        employmentId = employmentId,
        employerName = userData.employment.employmentDetails.employerName,
        employmentEnded = userData.employment.employmentDetails.cessationDate.isDefined,
        isAgent = aUser.isAgent,
        form = payrollIdForm.fill(value = "some-payroll-id")
      )
    }

    "return page with pre-filled error form when form has errors" in {
      val employmentDetails = anEmploymentDetails.copy(payrollId = Some("invalid-input-$"))
      val userData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentDetails = employmentDetails))

      EmployerPayrollIdPage.apply(taxYearEOY, employmentId, aUser, payrollIdForm, userData) shouldBe EmployerPayrollIdPage(
        taxYear = taxYearEOY,
        employmentId = employmentId,
        employerName = userData.employment.employmentDetails.employerName,
        employmentEnded = userData.employment.employmentDetails.cessationDate.isDefined,
        isAgent = aUser.isAgent,
        form = payrollIdForm.fill(value = "invalid-input-$")
      )
    }
  }
}
