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
import builders.models.benefits.TravelEntertainmentModelBuilder.aTravelEntertainmentModel
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import config.MockEmploymentSessionService
import models.benefits.TravelEntertainmentModel
import utils.UnitTest

class TravelServiceSpec extends UnitTest with MockEmploymentSessionService {

  private val taxYear = 2021
  private val employmentId = "some-employment-id"

  private val underTest = new TravelService(mockEmploymentSessionService, mockExecutionContext)

  "updateSectionQuestion" should {
    "update travel model and set section question to true when true value passed" in {
      val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(sectionQuestion = Some(false))))
      val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel)
      val expectedBenefits = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(sectionQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(taxYear, employmentId, employmentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "clear travel model when sectionQuestion is set to false" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel)
      val expectedBenefits = aBenefitsViewModel.copy(travelEntertainmentModel = Some(TravelEntertainmentModel.clear))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "add travel model and set sectionQuestion given model not initially present" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel.copy(travelEntertainmentModel = None))
      val expectedBenefits = aBenefitsViewModel.copy(travelEntertainmentModel = Some(TravelEntertainmentModel(sectionQuestion = Some(false))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateTravelAndSubsistenceQuestion" should {
    "set travelAndSubsistenceQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(travelAndSubsistenceQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(travelAndSubsistenceQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTravelAndSubsistenceQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set travelAndSubsistenceQuestion to false and travelAndSubsistence value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(travelAndSubsistenceQuestion = Some(false), travelAndSubsistence = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTravelAndSubsistenceQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateTravelAndSubsistence" should {
    "set travelAndSubsistence amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(travelAndSubsistence = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTravelAndSubsistence(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updatePersonalIncidentalExpensesQuestion" should {
    "set personalIncidentalExpensesQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updatePersonalIncidentalExpensesQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set personalIncidentalExpensesQuestion to false and personalIncidentalExpenses value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false), personalIncidentalExpenses = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updatePersonalIncidentalExpensesQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updatePersonalIncidentalExpenses" should {
    "set personalIncidentalExpenses amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpenses = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updatePersonalIncidentalExpenses(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateEntertainingQuestion" should {
    "set entertainingQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(entertainingQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(entertainingQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEntertainingQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set entertainingQuestion to false and entertaining value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(entertainingQuestion = Some(false), entertaining = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEntertainingQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateEntertaining" should {
    "set entertaining amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(entertaining = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEntertaining(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }
}
