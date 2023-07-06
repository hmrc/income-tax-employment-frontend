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
import forms.benefits.assets.AssetsFormsProvider
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.benefits.AssetsModelBuilder.anAssetsModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData

class AssetTransfersBenefitsAmountPageSpec extends UnitTest {

  private val anyTaxYear = 2020
  private val anyEmploymentId = "employmentId"
  private val form = new AssetsFormsProvider().assetTransfersAmountForm(isAgent = aUser.isAgent)

  ".apply(...)" should {
    "create page model with error form when form has errors" in {
      val formWithErrors = form.bind(Map(AmountForm.amount -> ""))

      AssetTransfersBenefitsAmountPage.apply(anyTaxYear, anyEmploymentId, aUser, formWithErrors, anEmploymentUserData) shouldBe AssetTransfersBenefitsAmountPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        form = formWithErrors
      )
    }

    "create page model with empty form when assetTransfer amount is missing" in {
      val employmentBenefits = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransfer = None)))
      val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = Some(employmentBenefits)))

      AssetTransfersBenefitsAmountPage.apply(anyTaxYear, anyEmploymentId, aUser, form, employmentUserData) shouldBe AssetTransfersBenefitsAmountPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        form = form
      )
    }

    "create page model with form populated from the assetsModel assetTransfer value" in {
      val employmentBenefits = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransfer = Some(123))))
      val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = Some(employmentBenefits)))

      AssetTransfersBenefitsAmountPage.apply(anyTaxYear, anyEmploymentId, aUser, form, employmentUserData) shouldBe AssetTransfersBenefitsAmountPage(
        taxYear = anyTaxYear,
        employmentId = anyEmploymentId,
        isAgent = aUser.isAgent,
        form = form.bind(Map(AmountForm.amount -> "123"))
      )
    }
  }
}
