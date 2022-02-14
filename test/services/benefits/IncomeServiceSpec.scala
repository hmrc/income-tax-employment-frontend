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

import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.benefits.IncomeTaxAndCostsModelBuilder.anIncomeTaxAndCostsModel
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import config.MockEmploymentSessionService
import models.benefits.IncomeTaxAndCostsModel
import utils.UnitTest

class IncomeServiceSpec extends UnitTest with MockEmploymentSessionService {

  private val taxYear = 2021
  private val employmentId = "some-employment-id"

  private val underTest = new IncomeService(mockEmploymentSessionService, mockExecutionContext)

  "updateSectionQuestion" should {
    "update income model and set section question to true when true value passed" in {
      val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(sectionQuestion = Some(false))))
      val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel)
      val expectedBenefits = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(sectionQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "clear income model when sectionQuestion is set to false" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel)
      val expectedBenefits = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(IncomeTaxAndCostsModel.clear))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "add income model and set sectionQuestion given model not initially present" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel.copy(incomeTaxAndCostsModel = None))
      val expectedBenefits = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(IncomeTaxAndCostsModel(sectionQuestion = Some(false))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateIncomeTaxPaidByDirectorQuestion" should {
    "set incomeTaxPaidByDirectorQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirectorQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirectorQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateIncomeTaxPaidByDirectorQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set incomeTaxPaidByDirectorQuestion to false and incomeTaxPaidByDirector value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirectorQuestion = Some(false), incomeTaxPaidByDirector = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateIncomeTaxPaidByDirectorQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateIncomeTaxPaidByDirector" should {
    "set incomeTaxPaidByDirector amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirector = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateIncomeTaxPaidByDirector(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updatePaymentsOnEmployeesBehalfQuestion" should {
    "set paymentsOnEmployeesBehalfQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(paymentsOnEmployeesBehalfQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(paymentsOnEmployeesBehalfQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updatePaymentsOnEmployeesBehalfQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set paymentsOnEmployeesBehalfQuestion to false and paymentsOnEmployeesBehalf value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(paymentsOnEmployeesBehalfQuestion = Some(false), paymentsOnEmployeesBehalf = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updatePaymentsOnEmployeesBehalfQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updatePaymentsOnEmployeesBehalf" should {
    "set paymentsOnEmployeesBehalf amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(paymentsOnEmployeesBehalf = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updatePaymentsOnEmployeesBehalf(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }
}
