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

import models.benefits.ReimbursedCostsVouchersAndNonCashModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.ReimbursedCostsVouchersAndNonCashModelBuilder.aReimbursedCostsVouchersAndNonCashModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import support.mocks.MockEmploymentSessionService
import utils.UnitTest

class ReimbursedServiceSpec extends UnitTest with MockEmploymentSessionService {

  private val taxYear = 2021
  private val employmentId = "some-employment-id"

  private val underTest = new ReimbursedService(mockEmploymentSessionService, mockExecutionContext)

  "updateSectionQuestion" should {
    "update reimbursed model and set section question to true when true value passed" in {
      val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(sectionQuestion = Some(false))))
      val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel)
      val expectedBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(sectionQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "clear reimbursed model when sectionQuestion is set to false" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel)
      val expectedBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(ReimbursedCostsVouchersAndNonCashModel.clear))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "add reimbursed model and set sectionQuestion given model not initially present" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = None))
      val expectedBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(authorisationRequest.user, taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateExpensesQuestion" should {
    "set expensesQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateExpensesQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set expensesQuestion to false and expenses value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = Some(false), expenses = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateExpensesQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateExpenses" should {
    "set expenses amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expenses = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateExpenses(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateTaxableExpensesQuestion" should {
    "set taxableExpensesQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTaxableExpensesQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set taxableExpensesQuestion to false and taxableExpenses value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = Some(false), taxableExpenses = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTaxableExpensesQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateTaxableExpenses" should {
    "set taxableExpenses amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpenses = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTaxableExpenses(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateVouchersAndCreditCardsQuestion" should {
    "set vouchersAndCreditCardsQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateVouchersAndCreditCardsQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set vouchersAndCreditCardsQuestion to false and taxableExpenses value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = Some(false), vouchersAndCreditCards = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateVouchersAndCreditCardsQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateVouchersAndCreditCards" should {
    "set vouchersAndCreditCards amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCards = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateVouchersAndCreditCards(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateNonCashQuestion" should {
    "set nonCashQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateNonCashQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set nonCashQuestion to false and nonCash value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = Some(false), nonCash = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateNonCashQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateNonCash" should {
    "set nonCash amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCash = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateNonCash(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateOtherItemsQuestion" should {
    "set otherItemsQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateOtherItemsQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set otherItemsQuestion to false and otherItems value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = Some(false), otherItems = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateOtherItemsQuestion(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateOtherItems" should {
    "set otherItems amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItems = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateOtherItems(authorisationRequest.user, taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }
}
