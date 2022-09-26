/*
 * Copyright 2022 HM Revenue & Customs
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

package services.benefits

import models.benefits.AssetsModel
import support.builders.models.UserBuilder.aUser
import support.builders.models.benefits.AssetsModelBuilder.anAssetsModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import support.mocks.MockEmploymentSessionService
import support.{TaxYearProvider, UnitTest}

import scala.concurrent.ExecutionContext

class AssetsServiceSpec extends UnitTest
  with TaxYearProvider
  with MockEmploymentSessionService {

  private val employmentId = "some-employment-id"

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val underTest = new AssetsService(mockEmploymentSessionService, ec)

  "updateSectionQuestion" should {
    "update assets model and set section question to true when true value passed" in {
      val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(sectionQuestion = Some(false))))
      val employmentUserDataWithFalseSectionQuestion = anEmploymentUserDataWithBenefits(benefitsViewModel).copy(hasPriorBenefits = false)
      val expectedEmploymentUserData = anEmploymentUserData.copy(hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, anEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(aUser, taxYearEOY, employmentId, employmentUserDataWithFalseSectionQuestion, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "clear assets relocation model when sectionQuestion is set to false" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel).copy(hasPriorBenefits = false)
      val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(AssetsModel.clear))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel).copy(hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(aUser, taxYearEOY, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "add assets model and set sectionQuestion given model not initially present" in {
      val employmentUserDataWithNoAssetsModel = anEmploymentUserDataWithBenefits(aBenefitsViewModel.copy(assetsModel = None))
      val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(AssetsModel.clear))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(aUser, taxYearEOY, employmentId, employmentUserDataWithNoAssetsModel, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateAssetsQuestion" should {
    "set assetsQuestion to true when true" in {
      val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetsQuestion = Some(false))))
      val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetsQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateAssetsQuestion(aUser, taxYearEOY, employmentId, employmentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set assetsQuestion to false and assets value is cleared when false" in {
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetsQuestion = Some(false), assets = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateAssetsQuestion(aUser, taxYearEOY, employmentId, anEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateAssets" should {
    "set assets amount" in {
      val employmentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assets = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateAssets(aUser, taxYearEOY, employmentId, employmentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateAssetTransferQuestion" should {
    "set assetTransferQuestion to true when true" in {
      val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransferQuestion = Some(false))))
      val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransferQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateAssetTransferQuestion(aUser, taxYearEOY, employmentId, employmentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set assetTransferQuestion to false and assets value is cleared when false" in {
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransferQuestion = Some(false), assetTransfer = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateAssetTransferQuestion(aUser, taxYearEOY, employmentId, anEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateAssetTransfer" should {
    "set assetTransfer amount" in {
      val employmentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransfer = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateAssetTransfer(aUser, taxYearEOY, employmentId, employmentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }
}
