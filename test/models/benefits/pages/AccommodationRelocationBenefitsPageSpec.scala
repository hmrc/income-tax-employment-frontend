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
import forms.benefits.accommodation.AccommodationFormsProvider
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData

class AccommodationRelocationBenefitsPageSpec extends UnitTest {

  private val anyTaxYear = 2020
  private val anyEmploymentId = "employmentId"
  private val anyEmploymentUserData = anEmploymentUserData
  private val user = aUser
  private val form = new AccommodationFormsProvider().accommodationRelocationForm(isAgent = user.isAgent)

  ".apply(...)" should {
    "create page model with error form when form has errors" in {
      val formWithErrors = form.bind(Map(YesNoForm.yesNo -> ""))

      AccommodationRelocationBenefitsPage.apply(anyTaxYear, anyEmploymentId, user, formWithErrors, anyEmploymentUserData) shouldBe AccommodationRelocationBenefitsPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = user.isAgent,
        form = formWithErrors
      )
    }

    "create page model with form populated from the accommodationRelocationModel sectionQuestion" in {
      val employmentBenefits = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(sectionQuestion = Some(false))))
      val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(employmentBenefits)))

      AccommodationRelocationBenefitsPage.apply(anyTaxYear, anyEmploymentId, user, form, employmentUserData) shouldBe AccommodationRelocationBenefitsPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = user.isAgent,
        form = form.bind(Map(YesNoForm.yesNo -> YesNoForm.no))
      )
    }
  }
}
