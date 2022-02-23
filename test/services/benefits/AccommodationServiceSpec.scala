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

import builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import config.MockEmploymentSessionService
import models.benefits.AccommodationRelocationModel
import utils.UnitTest

class AccommodationServiceSpec extends UnitTest with MockEmploymentSessionService {

  private val taxYear = 2021
  private val employmentId = "some-employment-id"

  private val underTest = new AccommodationService(mockEmploymentSessionService, mockExecutionContext)

  "updateSectionQuestion" should {
    "update accommodation relocation model and set section question to true when true value passed" in {
      val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(sectionQuestion = Some(false))))
      val employmentUserDataWithFalseSectionQuestion = anEmploymentUserDataWithBenefits(benefitsViewModel).copy(hasPriorBenefits = false)
      val expectedEmploymentUserData = anEmploymentUserData.copy(hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, anEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserDataWithFalseSectionQuestion, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "clear accommodation relocation model when sectionQuestion is set to false" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel).copy(hasPriorBenefits = false)
      val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel.clear))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel).copy(hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "add accommodation relocation model and set sectionQuestion given accommodation relocation model not initially present" in {
      val employmentUserDataWithNoAccommodationRelocationModel = anEmploymentUserDataWithBenefits(aBenefitsViewModel.copy(accommodationRelocationModel = None))
      val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel.clear))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserDataWithNoAccommodationRelocationModel, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateAccommodationQuestion" should {
    "set accommodationQuestion to true when true" in {
      val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodationQuestion = Some(false))))
      val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodationQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateAccommodationQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set accommodationQuestion to false and accommodation value is cleared when false" in {
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodationQuestion = Some(false), accommodation = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateAccommodationQuestion(authorisationRequest.user, taxYear, employmentId, anEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateAccommodation" should {
    "set accommodation amount" in {
      val employmentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateAccommodation(authorisationRequest.user, taxYear, employmentId, employmentUserData, amount = 123)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateQualifyingExpensesQuestion" should {
    "set qualifyingRelocationExpensesQuestion to true when true" in {
      val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(qualifyingRelocationExpensesQuestion = Some(false))))
      val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(qualifyingRelocationExpensesQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateQualifyingExpensesQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set qualifyingRelocationExpensesQuestion to false and qualifyingRelocationExpenses value is cleared when false" in {
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(qualifyingRelocationExpensesQuestion = Some(false), qualifyingRelocationExpenses = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateQualifyingExpensesQuestion(authorisationRequest.user, taxYear, employmentId, anEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateQualifyingExpenses" should {
    "set qualifyingRelocationExpenses amount" in {
      val employmentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(qualifyingRelocationExpenses = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateQualifyingExpenses(authorisationRequest.user, taxYear, employmentId, employmentUserData, amount = 123)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateNonQualifyingExpensesQuestion" should {
    "set nonQualifyingRelocationExpensesQuestion to true when true" in {
      val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpensesQuestion = Some(false))))
      val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpensesQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateNonQualifyingExpensesQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set nonQualifyingRelocationExpensesQuestion to false and nonQualifyingRelocationExpenses value is cleared when false" in {
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpensesQuestion = Some(false), nonQualifyingRelocationExpenses = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateNonQualifyingExpensesQuestion(authorisationRequest.user, taxYear, employmentId, anEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateNonQualifyingExpenses" should {
    "set nonQualifyingRelocationExpenses amount" in {
      val employmentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpenses = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateNonQualifyingExpenses(authorisationRequest.user, taxYear, employmentId, employmentUserData, amount = 123)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }
}
