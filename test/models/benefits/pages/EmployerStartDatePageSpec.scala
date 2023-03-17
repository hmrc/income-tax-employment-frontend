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

import forms.details.DateForm
import models.employment.DateFormData
import play.api.data.FormError
import support.builders.models.UserBuilder.aUser
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.{TaxYearProvider, UnitTest}

class EmployerStartDatePageSpec extends UnitTest
  with TaxYearProvider {

  private val employmentId = "employmentId"
  private val dateForm = DateForm.dateForm()

  ".apply(...)" should {
    "return page with empty form when start date is not preset" in {
      val employmentDetails = anEmploymentDetails.copy(startDate = None)
      val userData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentDetails = employmentDetails))

      EmployerStartDatePage.apply(taxYearEOY, employmentId, aUser, dateForm, userData) shouldBe EmployerStartDatePage(
        taxYear = taxYearEOY,
        employmentId = employmentId,
        employerName = anEmploymentUserData.employment.employmentDetails.employerName,
        isAgent = aUser.isAgent,
        form = dateForm
      )
    }

    "return page with pre-filled form when start date is preset" in {
      val employmentDetails = anEmploymentDetails.copy(startDate = Some(s"$taxYearEOY-01-01"))
      val userData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentDetails = employmentDetails))

      EmployerStartDatePage.apply(taxYearEOY, employmentId, aUser, dateForm, userData) shouldBe EmployerStartDatePage(
        taxYear = taxYearEOY,
        employmentId = employmentId,
        employerName = anEmploymentUserData.employment.employmentDetails.employerName,
        isAgent = aUser.isAgent,
        form = dateForm.fill(value = DateFormData(s"$taxYearEOY-01-01"))
      )
    }

    "return page with pre-filled error form when form has errors" in {
      val form = dateForm.bind(Map(DateForm.day -> "6", DateForm.month -> "4", DateForm.year -> taxYear.toString))
      val formWithErrors = form.copy(errors = Seq(FormError("some.key", "some-message")))

      EmployerStartDatePage.apply(taxYearEOY, employmentId, aUser, formWithErrors, anEmploymentUserData) shouldBe EmployerStartDatePage(
        taxYear = taxYearEOY,
        employmentId = employmentId,
        employerName = anEmploymentUserData.employment.employmentDetails.employerName,
        isAgent = aUser.isAgent,
        form = formWithErrors
      )
    }
  }
}
