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

import models.expenses.ExpensesViewModel
import support.ServiceUnitTest
import support.builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserDataWithBenefits
import support.builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import support.builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import support.mocks.MockEmploymentSessionService

class ExpensesServiceSpec extends ServiceUnitTest with MockEmploymentSessionService {

  private val underTest = new ExpensesService(mockEmploymentSessionService, ec)

  "updateClaimingEmploymentExpenses" should {
    "update expenses model and set claimingEmploymentExpenses to true when true value passed" in {
      val expensesViewModel = anExpensesViewModel.copy(claimingEmploymentExpenses = false)
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel)
      val expectedExpensesViewModel = anExpensesViewModel.copy(claimingEmploymentExpenses = true)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = true, hasPriorExpenses = true, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateClaimingEmploymentExpenses(authorisationRequest.user, taxYearEOY, employmentUserData, questionValue = true)) shouldBe Right(expectedExpensesUserData)
    }

    "clear expenses model when claimingEmploymentExpenses is set to false" in {
      val employmentUserData = anExpensesUserDataWithBenefits(anExpensesViewModel)
      val expectedExpensesViewModel = ExpensesViewModel.clear(anExpensesCYAModel.expenses.isUsingCustomerData)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = true, hasPriorExpenses = true, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateClaimingEmploymentExpenses(authorisationRequest.user, taxYearEOY, employmentUserData, questionValue = false)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateJobExpensesQuestion" should {
    "set jobExpensesQuestion to true when true" in {
      val expensesViewModel = anExpensesViewModel.copy(jobExpensesQuestion = Some(false))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(jobExpensesQuestion = Some(true))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateJobExpensesQuestion(authorisationRequest.user, taxYearEOY, employmentUserData, questionValue = true)) shouldBe Right(expectedExpensesUserData)
    }

    "set jobExpensesQuestion to false and jobExpenses value is cleared when false" in {
      val expensesViewModel = anExpensesViewModel.copy(jobExpensesQuestion = Some(true))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(jobExpensesQuestion = Some(false), jobExpenses = None)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateJobExpensesQuestion(authorisationRequest.user, taxYearEOY, employmentUserData, questionValue = false)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateJobExpenses" should {
    "set jobExpenses amount" in {
      val expensesViewModel = anExpensesViewModel.copy(jobExpenses = Some(100))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(jobExpenses = Some(123))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateJobExpenses(authorisationRequest.user, taxYearEOY, employmentUserData, amount = 123)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateFlatRateJobExpensesQuestion" should {
    "set flatRateJobExpensesQuestion to true when true" in {
      val expensesViewModel = anExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(false))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(true))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateFlatRateJobExpensesQuestion(authorisationRequest.user, taxYearEOY, employmentUserData, questionValue = true)) shouldBe Right(expectedExpensesUserData)
    }

    "set flatRateJobExpensesQuestion to false and flatRateJobExpenses value is cleared when false" in {
      val expensesViewModel = anExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(true))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(false), flatRateJobExpenses = None)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateFlatRateJobExpensesQuestion(authorisationRequest.user, taxYearEOY, employmentUserData, questionValue = false)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateFlatRateJobExpenses" should {
    "set flatRateJobExpenses amount" in {
      val expensesViewModel = anExpensesViewModel.copy(flatRateJobExpenses = Some(100))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(flatRateJobExpenses = Some(123))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateFlatRateJobExpenses(authorisationRequest.user, taxYearEOY, employmentUserData, amount = 123)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateProfessionalSubscriptionsQuestion" should {
    "set professionalSubscriptionsQuestion to true when true" in {
      val expensesViewModel = anExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(false))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(true))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateProfessionalSubscriptionsQuestion(authorisationRequest.user, taxYearEOY, employmentUserData, questionValue = true)) shouldBe Right(expectedExpensesUserData)
    }

    "set professionalSubscriptionsQuestion to false and professionalSubscriptions value is cleared when false" in {
      val expensesViewModel = anExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(true))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(false), professionalSubscriptions = None)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateProfessionalSubscriptionsQuestion(authorisationRequest.user, taxYearEOY, employmentUserData, questionValue = false)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateProfessionalSubscriptions" should {
    "set professionalSubscriptions amount" in {
      val expensesViewModel = anExpensesViewModel.copy(professionalSubscriptions = Some(100))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(professionalSubscriptions = Some(123))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateProfessionalSubscriptions(authorisationRequest.user, taxYearEOY, employmentUserData, amount = 123)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateOtherAndCapitalAllowancesQuestion" should {
    "set otherAndCapitalAllowancesQuestion to true when true" in {
      val expensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = Some(false))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = Some(true))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateOtherAndCapitalAllowancesQuestion(authorisationRequest.user, taxYearEOY, employmentUserData, questionValue = true)) shouldBe Right(expectedExpensesUserData)
    }

    "set otherAndCapitalAllowancesQuestion to false and otherAndCapitalAllowances value is cleared when false" in {
      val expensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = Some(true))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = Some(false), otherAndCapitalAllowances = None)
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateOtherAndCapitalAllowancesQuestion(authorisationRequest.user, taxYearEOY, employmentUserData, questionValue = false)) shouldBe Right(expectedExpensesUserData)
    }
  }

  "updateOtherAndCapitalAllowances" should {
    "set otherAndCapitalAllowances amount" in {
      val expensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowances = Some(100))
      val employmentUserData = anExpensesUserDataWithBenefits(expensesViewModel, isPriorSubmission = false)
      val expectedExpensesViewModel = anExpensesViewModel.copy(otherAndCapitalAllowances = Some(123))
      val expectedExpensesUserData = anExpensesUserDataWithBenefits(expectedExpensesViewModel, isPriorSubmission = false).copy(hasPriorExpenses = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expectedExpensesUserData.expensesCya, Right(expectedExpensesUserData))

      await(underTest.updateOtherAndCapitalAllowances(authorisationRequest.user, taxYearEOY, employmentUserData, amount = 123)) shouldBe Right(expectedExpensesUserData)
    }
  }
}
