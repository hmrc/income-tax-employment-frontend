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

import models.User
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import services.EmploymentSessionService
import utils.Clock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExpensesService @Inject()(employmentSessionService: EmploymentSessionService,
                                implicit val ec: ExecutionContext) {

  def updateClaimingEmploymentExpenses(taxYear: Int, originalExpensesUserData: ExpensesUserData, questionValue: Boolean)
                                      (implicit user: User[_], clock: Clock): Future[Either[Unit, ExpensesUserData]] = {
    val expensesCYAModel = originalExpensesUserData.expensesCya
    val expenses = expensesCYAModel.expenses

    val updatedExpenses: ExpensesCYAModel = if (questionValue) {
      expensesCYAModel.copy(expenses = expenses.copy(claimingEmploymentExpenses = true))
    } else {
      expensesCYAModel.copy(expenses = ExpensesViewModel.clear(expensesCYAModel.expenses.isUsingCustomerData))
    }

    employmentSessionService.createOrUpdateExpensesUserDataWith(
      taxYear,
      originalExpensesUserData.isPriorSubmission,
      originalExpensesUserData.isPriorSubmission,
      updatedExpenses
    )
  }

  def updateJobExpensesQuestion(taxYear: Int, originalExpensesUserData: ExpensesUserData, questionValue: Boolean)
                               (implicit user: User[_], clock: Clock): Future[Either[Unit, ExpensesUserData]] = {
    val expensesCYAModel = originalExpensesUserData.expensesCya
    val expenses = originalExpensesUserData.expensesCya.expenses

    val updatedExpenses: ExpensesCYAModel = if (questionValue) {
      expensesCYAModel.copy(expenses = expenses.copy(jobExpensesQuestion = Some(true)))
    } else {
      expensesCYAModel.copy(expenses = expenses.copy(jobExpensesQuestion = Some(false), jobExpenses = None))
    }

    employmentSessionService.createOrUpdateExpensesUserDataWith(
      taxYear,
      originalExpensesUserData.isPriorSubmission,
      originalExpensesUserData.isPriorSubmission,
      updatedExpenses
    )
  }

  def updateJobExpenses(taxYear: Int, originalExpensesUserData: ExpensesUserData, amount: BigDecimal)
                       (implicit user: User[_], clock: Clock): Future[Either[Unit, ExpensesUserData]] = {
    val expensesCYAModel = originalExpensesUserData.expensesCya
    val cya = expensesCYAModel.expenses
    val updatedExpenses: ExpensesCYAModel = expensesCYAModel.copy(expenses = cya.copy(jobExpenses = Some(amount)))

    employmentSessionService.createOrUpdateExpensesUserDataWith(
      taxYear,
      originalExpensesUserData.isPriorSubmission,
      originalExpensesUserData.isPriorSubmission,
      updatedExpenses
    )
  }

  def updateFlatRateJobExpensesQuestion(taxYear: Int, originalExpensesUserData: ExpensesUserData, questionValue: Boolean)
                                       (implicit user: User[_], clock: Clock): Future[Either[Unit, ExpensesUserData]] = {
    val expensesCYAModel = originalExpensesUserData.expensesCya
    val expensesViewModel = expensesCYAModel.expenses
    val updatedExpenses = if (questionValue) {
      expensesCYAModel.copy(expenses = expensesViewModel.copy(flatRateJobExpensesQuestion = Some(true)))
    } else {
      expensesCYAModel.copy(expenses = expensesViewModel.copy(flatRateJobExpensesQuestion = Some(false), flatRateJobExpenses = None))
    }

    employmentSessionService.createOrUpdateExpensesUserDataWith(
      taxYear,
      originalExpensesUserData.isPriorSubmission,
      originalExpensesUserData.isPriorSubmission,
      updatedExpenses
    )
  }

  def updateFlatRateJobExpenses(taxYear: Int, originalExpensesUserData: ExpensesUserData, amount: BigDecimal)
                               (implicit user: User[_], clock: Clock): Future[Either[Unit, ExpensesUserData]] = {
    val expensesCYAModel = originalExpensesUserData.expensesCya
    val expensesViewModel = expensesCYAModel.expenses
    val updatedExpenses = expensesCYAModel.copy(expenses = expensesViewModel.copy(flatRateJobExpenses = Some(amount)))

    employmentSessionService.createOrUpdateExpensesUserDataWith(
      taxYear,
      originalExpensesUserData.isPriorSubmission,
      originalExpensesUserData.isPriorSubmission,
      updatedExpenses
    )
  }

  def updateProfessionalSubscriptionsQuestion(taxYear: Int, originalExpensesUserData: ExpensesUserData, questionValue: Boolean)
                                             (implicit user: User[_], clock: Clock): Future[Either[Unit, ExpensesUserData]] = {
    val expensesCYAModel = originalExpensesUserData.expensesCya
    val expensesCyaModel = expensesCYAModel.expenses
    val updatedExpenses: ExpensesCYAModel = if (questionValue) {
      expensesCYAModel.copy(expenses = expensesCyaModel.copy(professionalSubscriptionsQuestion = Some(true)))
    } else {
      expensesCYAModel.copy(expenses = expensesCyaModel.copy(professionalSubscriptionsQuestion = Some(false), professionalSubscriptions = None))
    }

    employmentSessionService.createOrUpdateExpensesUserDataWith(
      taxYear,
      originalExpensesUserData.isPriorSubmission,
      originalExpensesUserData.isPriorSubmission,
      updatedExpenses
    )
  }

  def updateProfessionalSubscriptions(taxYear: Int, originalExpensesUserData: ExpensesUserData, amount: BigDecimal)
                                     (implicit user: User[_], clock: Clock): Future[Either[Unit, ExpensesUserData]] = {
    val cyaModel = originalExpensesUserData.expensesCya
    val expenses = cyaModel.expenses
    val updatedExpenses = cyaModel.copy(expenses = expenses.copy(professionalSubscriptions = Some(amount)))

    employmentSessionService.createOrUpdateExpensesUserDataWith(
      taxYear,
      originalExpensesUserData.isPriorSubmission,
      originalExpensesUserData.isPriorSubmission,
      updatedExpenses
    )
  }

  def updateOtherAndCapitalAllowancesQuestion(taxYear: Int, originalExpensesUserData: ExpensesUserData, questionValue: Boolean)
                                             (implicit user: User[_], clock: Clock): Future[Either[Unit, ExpensesUserData]] = {
    val expensesCYAModel = originalExpensesUserData.expensesCya
    val expenses = expensesCYAModel.expenses
    val updatedExpenses: ExpensesCYAModel = if (questionValue) {
      expensesCYAModel.copy(expenses = expenses.copy(otherAndCapitalAllowancesQuestion = Some(true)))
    } else {
      expensesCYAModel.copy(expenses = expenses.copy(otherAndCapitalAllowancesQuestion = Some(false), otherAndCapitalAllowances = None))
    }

    employmentSessionService.createOrUpdateExpensesUserDataWith(
      taxYear,
      originalExpensesUserData.isPriorSubmission,
      originalExpensesUserData.isPriorSubmission,
      updatedExpenses
    )
  }

  def updateOtherAndCapitalAllowances(taxYear: Int, originalExpensesUserData: ExpensesUserData, amount: BigDecimal)
                                     (implicit user: User[_], clock: Clock): Future[Either[Unit, ExpensesUserData]] = {
    val expensesCYAModel = originalExpensesUserData.expensesCya
    val expensesViewModel = expensesCYAModel.expenses
    val updatedExpenses = expensesCYAModel.copy(expenses = expensesViewModel.copy(otherAndCapitalAllowances = Some(amount)))

    employmentSessionService.createOrUpdateExpensesUserDataWith(
      taxYear,
      originalExpensesUserData.isPriorSubmission,
      originalExpensesUserData.isPriorSubmission,
      updatedExpenses
    )
  }
}