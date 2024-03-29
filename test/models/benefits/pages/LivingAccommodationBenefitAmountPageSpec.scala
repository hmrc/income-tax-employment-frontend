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

import forms.AmountForm
import forms.benefits.accommodation.AccommodationFormsProvider
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData

class LivingAccommodationBenefitAmountPageSpec extends UnitTest {

  private val anyTaxYear = 2020
  private val anyEmploymentId = "employmentId"
  private val anyEmploymentUserData = anEmploymentUserData
  private val form = new AccommodationFormsProvider().livingAccommodationAmountForm(isAgent = aUser.isAgent)

  ".apply(...)" should {
    "create page model with error form when form has errors" in {
      val formWithErrors = form.bind(Map(AmountForm.amount -> ""))

      LivingAccommodationBenefitAmountPage.apply(anyTaxYear, anyEmploymentId, aUser, formWithErrors, anyEmploymentUserData) shouldBe LivingAccommodationBenefitAmountPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        form = formWithErrors
      )
    }

    "create page model with empty form when accommodation amount is missing" in {
      val employmentBenefits = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None)))
      val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = Some(employmentBenefits)))

      LivingAccommodationBenefitAmountPage.apply(anyTaxYear, anyEmploymentId, aUser, form, employmentUserData) shouldBe LivingAccommodationBenefitAmountPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        form = form
      )
    }

    "create page model with form populated from the accommodationRelocationModel accommodation value" in {
      val employmentBenefits = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = Some(123))))
      val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = Some(employmentBenefits)))

      LivingAccommodationBenefitAmountPage.apply(anyTaxYear, anyEmploymentId, aUser, form, employmentUserData) shouldBe LivingAccommodationBenefitAmountPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        form = form.bind(Map(AmountForm.amount -> "123"))
      )
    }
  }
}
