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

package services.expenses

import builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserDataWithBenefits
import builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import config.MockEmploymentSessionService
import models.expenses.ExpensesViewModel
import utils.UnitTest

class ExpensesServiceSpec extends UnitTest with MockEmploymentSessionService {

  private val taxYear = 2021

  private val underTest = new ExpensesService(mockEmploymentSessionService, mockExecutionContext)

  "updateClaimingEmploymentExpenses" should {
    "update expenses model and set claimingEmploymentExpenses to true when true value passed" in {
      val expensesViewModel = anExpensesViewModel.copy(claimingEmploymentExpenses = false)
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel)
      val expectedExpensesViewModel = anExpensesViewModel.copy(claimingEmploymentExpenses = true)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = true, hasPriorExpenses = true, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateClaimingEmploymentExpenses(authorisationRequest.user, taxYear, employmentUserData, questionValue = true)) shouldBe Right(expectedExpensesUserData)
    }

    "clear expenses model when claimingEmploymentExpenses is set to false" in {
      val employmentUserData = anExpensesUserDataWithBenefits(anExpensesViewModel)
      val expectedExpensesViewModel = ExpensesViewModel.clear(anExpensesCYAModel.expenses.isUsingCustomerData)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = true, hasPriorExpenses = true, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateClaimingEmploymentExpenses(authorisationRequest.user, taxYear, employmentUserData, questionValue = false)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateJobExpensesQuestion" should {
    "set jobExpensesQuestion to true when true" in {
      val expensesViewModel = anExpensesViewModel.copy(jobExpensesQuestion = Some(false))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(jobExpensesQuestion = Some(true))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateJobExpensesQuestion(authorisationRequest.user, taxYear, employmentUserData, questionValue = true)) shouldBe Right(expectedExpensesUserData)
    }

    "set jobExpensesQuestion to false and jobExpenses value is cleared when false" in {
      val expensesViewModel = anExpensesViewModel.copy(jobExpensesQuestion = Some(true))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(jobExpensesQuestion = Some(false), jobExpenses = None)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateJobExpensesQuestion(authorisationRequest.user, taxYear, employmentUserData, questionValue = false)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateJobExpenses" should {
    "set jobExpenses amount" in {
      val expensesViewModel = anExpensesViewModel.copy(jobExpenses = Some(100))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(jobExpenses = Some(123))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateJobExpenses(authorisationRequest.user, taxYear, employmentUserData, amount = 123)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateFlatRateJobExpensesQuestion" should {
    "set flatRateJobExpensesQuestion to true when true" in {
      val expensesViewModel = anExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(false))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(true))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateFlatRateJobExpensesQuestion(authorisationRequest.user, taxYear, employmentUserData, questionValue = true)) shouldBe Right(expectedExpensesUserData)
    }

    "set flatRateJobExpensesQuestion to false and flatRateJobExpenses value is cleared when false" in {
      val expensesViewModel = anExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(true))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(false), flatRateJobExpenses = None)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateFlatRateJobExpensesQuestion(authorisationRequest.user, taxYear, employmentUserData, questionValue = false)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateFlatRateJobExpenses" should {
    "set flatRateJobExpenses amount" in {
      val expensesViewModel = anExpensesViewModel.copy(flatRateJobExpenses = Some(100))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(flatRateJobExpenses = Some(123))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateFlatRateJobExpenses(authorisationRequest.user, taxYear, employmentUserData, amount = 123)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateProfessionalSubscriptionsQuestion" should {
    "set professionalSubscriptionsQuestion to true when true" in {
      val expensesViewModel = anExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(false))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(true))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateProfessionalSubscriptionsQuestion(authorisationRequest.user, taxYear, employmentUserData, questionValue = true)) shouldBe Right(expectedExpensesUserData)
    }

    "set professionalSubscriptionsQuestion to false and professionalSubscriptions value is cleared when false" in {
      val expensesViewModel = anExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(true))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(false), professionalSubscriptions = None)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateProfessionalSubscriptionsQuestion(authorisationRequest.user, taxYear, employmentUserData, questionValue = false)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateProfessionalSubscriptions" should {
    "set professionalSubscriptions amount" in {
      val expensesViewModel = anExpensesViewModel.copy(professionalSubscriptions = Some(100))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(professionalSubscriptions = Some(123))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateProfessionalSubscriptions(authorisationRequest.user, taxYear, employmentUserData, amount = 123)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateOtherAndCapitalAllowancesQuestion" should {
    "set otherAndCapitalAllowancesQuestion to true when true" in {
      val expensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = Some(false))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = Some(true))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateOtherAndCapitalAllowancesQuestion(authorisationRequest.user, taxYear, employmentUserData, questionValue = true)) shouldBe Right(expectedExpensesUserData)
    }

    "set otherAndCapitalAllowancesQuestion to false and otherAndCapitalAllowances value is cleared when false" in {
      val expensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = Some(true))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = Some(false), otherAndCapitalAllowances = None)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateOtherAndCapitalAllowancesQuestion(authorisationRequest.user, taxYear, employmentUserData, questionValue = false)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateOtherAndCapitalAllowances" should {
    "set otherAndCapitalAllowances amount" in {
      val expensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowances = Some(100))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowances = Some(123))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYear, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateOtherAndCapitalAllowances(authorisationRequest.user, taxYear, employmentUserData, amount = 123)) shouldBe Right(expectedExpensesUserData)
    }
  }
}
