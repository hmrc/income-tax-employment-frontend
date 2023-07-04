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

import forms.YesNoForm
import support.TaxYearUtils.taxYearEOY
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData

import java.time.{LocalDate, Month}

class DidYouLeaveEmployerPageSpec extends UnitTest {

  private val startOfTaxYear = LocalDate.of(taxYearEOY - 1, Month.APRIL, 6)
  private val anyEmploymentId = "employmentId"

  private val questionForm = YesNoForm.yesNoForm("", Seq.empty)

  ".apply(...)" should {
    "return page with first date set to start of tax year when start date is before start of year" in {
      val employmentStartDate = LocalDate.parse(s"${taxYearEOY - 1}-01-01")
      val employmentDetails = anEmploymentDetails.copy(startDate = Some(employmentStartDate.toString))
      val userData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentDetails = employmentDetails))

      DidYouLeaveEmployerPage.apply(taxYearEOY, anyEmploymentId, aUser, questionForm, userData) shouldBe DidYouLeaveEmployerPage(
        taxYear = taxYearEOY,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        titleFirstDate = startOfTaxYear,
        form = questionForm.fill(value = anEmploymentDetails.didYouLeaveQuestion.get)
      )
    }

    "return page with first date set to start date when start date is NOT before start of year" in {
      val startDate = LocalDate.of(taxYearEOY, Month.JUNE, 1)
      val employmentDetails = anEmploymentDetails.copy(startDate = Some(startDate.toString))
      val userData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentDetails = employmentDetails))

      DidYouLeaveEmployerPage.apply(taxYearEOY, anyEmploymentId, aUser, questionForm, userData) shouldBe DidYouLeaveEmployerPage(
        taxYear = taxYearEOY,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        titleFirstDate = startDate,
        form = questionForm.fill(value = anEmploymentDetails.didYouLeaveQuestion.get)
      )
    }

    "create page model with error form when form has errors" in {
      val formWithErrors = questionForm.bind(Map(YesNoForm.yesNo -> ""))

      DidYouLeaveEmployerPage.apply(taxYearEOY, anyEmploymentId, aUser, formWithErrors, anEmploymentUserData) shouldBe DidYouLeaveEmployerPage(
        taxYear = taxYearEOY,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        titleFirstDate = LocalDate.parse(anEmploymentDetails.startDate.get),
        form = formWithErrors
      )
    }

    "create page model with form populated from the employmentDetails didYouLeaveQuestion" in {
      val employmentDetails = anEmploymentDetails.copy(didYouLeaveQuestion = Some(false))
      val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentDetails = employmentDetails))

      DidYouLeaveEmployerPage.apply(taxYearEOY, anyEmploymentId, aUser, questionForm, employmentUserData) shouldBe DidYouLeaveEmployerPage(
        taxYear = taxYearEOY,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        titleFirstDate = LocalDate.parse(anEmploymentDetails.startDate.get),
        form = questionForm.bind(Map(YesNoForm.yesNo -> YesNoForm.no))
      )
    }

    "create page model with empty form" in {
      val employmentDetails = anEmploymentDetails.copy(didYouLeaveQuestion = None)
      val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentDetails = employmentDetails))

      DidYouLeaveEmployerPage.apply(taxYearEOY, anyEmploymentId, aUser, questionForm, employmentUserData) shouldBe DidYouLeaveEmployerPage(
        taxYear = taxYearEOY,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        titleFirstDate = LocalDate.parse(anEmploymentDetails.startDate.get),
        form = questionForm
      )
    }
  }
}
