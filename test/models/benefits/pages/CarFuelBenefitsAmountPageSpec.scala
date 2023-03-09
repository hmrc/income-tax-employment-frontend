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
import forms.benefits.fuel.FuelFormsProvider
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData

class CarFuelBenefitsAmountPageSpec extends UnitTest {

  private val anyTaxYear = 2020
  private val anyEmploymentId = "employmentId"
  private val amountForm = new FuelFormsProvider().carFuelAmountForm(isAgent = aUser.isAgent)

  ".apply(...)" should {
    "create page model with error form when form has errors" in {
      val formWithErrors = amountForm.bind(Map(AmountForm.amount -> ""))

      CarFuelBenefitsAmountPage.apply(anyTaxYear, anyEmploymentId, aUser, formWithErrors, anEmploymentUserData) shouldBe CarFuelBenefitsAmountPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        form = formWithErrors
      )
    }

    "create page model with empty form when carFuel amount is missing" in {
      val employmentBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carFuel = None)))
      val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(employmentBenefits)))

      CarFuelBenefitsAmountPage.apply(anyTaxYear, anyEmploymentId, aUser, amountForm, employmentUserData) shouldBe CarFuelBenefitsAmountPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        form = amountForm
      )
    }

    "create page model with form populated from the assetsModel assetTransfer value" in {
      val employmentBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carFuel = Some(123))))
      val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(employmentBenefits)))

      CarFuelBenefitsAmountPage.apply(anyTaxYear, anyEmploymentId, aUser, amountForm, employmentUserData) shouldBe CarFuelBenefitsAmountPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        form = amountForm.bind(Map(AmountForm.amount -> "123"))
      )
    }
  }
}
