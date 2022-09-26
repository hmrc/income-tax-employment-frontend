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

import models.benefits.BenefitsViewModel
import support.builders.models.UserBuilder.aUser
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import support.mocks.MockEmploymentSessionService
import support.{TaxYearProvider, UnitTest}

import scala.concurrent.ExecutionContext

class BenefitsServiceSpec extends UnitTest
  with TaxYearProvider
  with MockEmploymentSessionService {

  private val employmentId = "some-employment-id"

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val underTest = new BenefitsService(mockEmploymentSessionService, ec)

  "updateIsBenefitsReceived" should {
    "update benefits model when questionValue is true" when {
      "and previous benefits exist" in {
        val benefitsViewModel = aBenefitsViewModel.copy(isBenefitsReceived = false)
        val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel)
        val expectedEmploymentUserData = anEmploymentUserData

        mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updateIsBenefitsReceived(aUser, taxYearEOY, employmentId, employmentUserData, questionValue = true)) shouldBe
          Right(expectedEmploymentUserData)
      }

      "and no previous benefits exist" in {
        val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None))
        val expectedBenefits = BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true)
        val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

        mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updateIsBenefitsReceived(aUser, taxYearEOY, employmentId, employmentUserData, questionValue = true)) shouldBe
          Right(expectedEmploymentUserData)
      }
    }

    "update benefits model when questionValue is false" when {
      "and previous benefits exist" in {
        val benefitsViewModel = aBenefitsViewModel.copy(isBenefitsReceived = false)
        val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel)
        val expectedBenefits = BenefitsViewModel.clear(isUsingCustomerData = true)
        val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

        mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updateIsBenefitsReceived(aUser, taxYearEOY, employmentId, employmentUserData, questionValue = false)) shouldBe
          Right(expectedEmploymentUserData)
      }

      "and no previous benefits exist" in {
        val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None))
        val expectedBenefits = BenefitsViewModel.clear(isUsingCustomerData = true)
        val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

        mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updateIsBenefitsReceived(aUser, taxYearEOY, employmentId, employmentUserData, questionValue = false)) shouldBe
          Right(expectedEmploymentUserData)
      }
    }
  }
}
