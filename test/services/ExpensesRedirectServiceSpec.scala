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

package services

import controllers.expenses.routes._
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Call
import play.api.mvc.Results.Ok
import services.ExpensesRedirectService._
import utils.UnitTest

import scala.concurrent.Future

class ExpensesRedirectServiceSpec extends UnitTest {

  private val cyaModel: ExpensesCYAModel = ExpensesCYAModel(expenses = ExpensesViewModel(isUsingCustomerData = true))
  private val taxYear = 2021

  private val result = Future.successful(Ok("OK"))

  private val emptyExpensesViewModel = ExpensesViewModel(
    claimingEmploymentExpenses = false,
    isUsingCustomerData = false)

  private val fullExpensesViewModel = ExpensesViewModel(
    claimingEmploymentExpenses = true,
    jobExpensesQuestion = Some(true),
    jobExpenses = Some(100.00),
    flatRateJobExpensesQuestion = Some(true),
    flatRateJobExpenses = Some(200.00),
    professionalSubscriptionsQuestion = Some(true),
    professionalSubscriptions = Some(300.00),
    otherAndCapitalAllowancesQuestion = Some(true),
    otherAndCapitalAllowances = Some(400.00),
    businessTravelCosts = None,
    hotelAndMealExpenses = None,
    vehicleExpenses = None,
    mileageAllowanceRelief = None,
    submittedOn = Some("2020-11-11"),
    isUsingCustomerData = true
  )

  private def expensesCYAModel(expensesModel: ExpensesViewModel) = ExpensesCYAModel(expenses = expensesModel)

  private val expensesUserData = ExpensesUserData(
    sessionId, mtditid, nino, taxYear, isPriorSubmission = false, hasPriorExpenses = false, expensesCya = ExpensesCYAModel(fullExpensesViewModel))


  "expensesSubmitRedirect" should {
    "redirect to the CYA page if the journey is finished" in {
      val result = Future.successful(ExpensesRedirectService.expensesSubmitRedirect(expensesCYAModel(fullExpensesViewModel), Call("GET", "/next"))(taxYear))
      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe CheckEmploymentExpensesController.show(taxYear).url
    }

    "redirect to next page if the journey is not finished" in {
      val result = Future.successful(ExpensesRedirectService.expensesSubmitRedirect(expensesCYAModel(emptyExpensesViewModel.copy(
        claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false), flatRateJobExpensesQuestion = Some(false))),
        Call("GET", "/next"))(taxYear))
      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe "/next"
    }
  }

  "redirectBasedOnCurrentAnswers" should {
    "redirect to jobExpenses Question page" when {
      "it's a new submission and user attempts to view jobExpenses amount page but jobExpensesQuestion is None" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = expensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = None, jobExpenses = None, isUsingCustomerData = false)))))(
          cya => ExpensesRedirectService.jobExpensesAmountRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe BusinessTravelOvernightExpensesController.show(taxYear).url
      }
      "it's a new submission and user attempts to view Uniforms Or Tools Question page but jobExpensesQuestion is None" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = expensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = None, jobExpenses = None, isUsingCustomerData = false)))))(
          cya => ExpensesRedirectService.flatRateRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe BusinessTravelOvernightExpensesController.show(taxYear).url
      }
    }

    "redirect to jobExpenses amount page" when {
      "it's a new submission and user attempts to view flatRate Question page but jobExpenses amount is empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(true))))))(
          cya => ExpensesRedirectService.flatRateRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelAndOvernightAmountController.show(taxYear).url
      }
    }

    "redirect to Uniforms or Tools (flatRate) Question page" when {
      "it's a new submission and user attempts to view flatRate amount page but flatRateJobExpensesQuestion is None" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.flatRateAmountRedirect(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UniformsOrToolsExpensesController.show(taxYear).url
      }
      "it's a new submission and user attempts to view jobExpenses amount page but jobExpensesQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(isPriorSubmission = false, hasPriorExpenses = false, expensesCya = expensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.jobExpensesAmountRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UniformsOrToolsExpensesController.show(taxYear).url
      }
    }

    "redirect to Uniforms or Tools (flatRate) amount page" when {
      "it's a new submission and user attempts to view Professional Subscriptions question page but flatRateJobExpenses is empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false), flatRateJobExpensesQuestion = Some(true))))))(
          cya => ExpensesRedirectService.professionalSubscriptionsRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UniformsOrToolsExpensesAmountController.show(taxYear).url
      }
    }

    "redirect to Professional Fees and Subscriptions question page" when {
      "it's a new submission and user attempts to view Professional Subscriptions amount page but professionalSubscriptionsQuestion is None" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false), flatRateJobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.professionalSubscriptionsAmountRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ProfessionalFeesAndSubscriptionsExpensesController.show(taxYear).url
      }
      "it's a new submission and user attempts to view Uniforms or Tools (flatRate) amount page but flatRateJobExpenses is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false), flatRateJobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.flatRateAmountRedirect(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ProfessionalFeesAndSubscriptionsExpensesController.show(taxYear).url
      }

    }

    "redirect to Professional Fees and Subscriptions amount page" when {
      "it's a new submission and user attempts to view Other Equipment question page but professionalSubscriptionsQuestion is empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false),
            flatRateJobExpensesQuestion = Some(false), professionalSubscriptionsQuestion = Some(true))))))(
          cya => ExpensesRedirectService.otherAllowancesRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ProfFeesAndSubscriptionsExpensesAmountController.show(taxYear).url
      }
    }

    "redirect to Other Equipment (otherAndCapitalAllowances) question page" when {
      "it's a new submission and user attempts to view Other Equipment amount page but otherAndCapitalAllowancesQuestion is None" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false),
            flatRateJobExpensesQuestion = Some(false), professionalSubscriptionsQuestion = Some(false))))))(
          cya => ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe OtherEquipmentController.show(taxYear).url
      }
      "it's a new submission and user attempts to view Professional Subscriptions amount page but professionalSubscriptionsQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false),
            flatRateJobExpensesQuestion = Some(false), professionalSubscriptionsQuestion = Some(false))))))(
          cya => ExpensesRedirectService.professionalSubscriptionsAmountRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe OtherEquipmentController.show(taxYear).url
      }
    }

    "redirect to Check Employment Expenses page" when {
      "it's a new submission and user attempts to view jobExpensesQuestion page but claimingEmploymentExpenses is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(isPriorSubmission = false, hasPriorExpenses = false,
          expensesCya = ExpensesCYAModel(emptyExpensesViewModel))))(
          cya => ExpensesRedirectService.commonExpensesRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }

      "it's a new submission and user attempts to view Other Equipment amount page but otherAndCapitalAllowancesQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false),
            flatRateJobExpensesQuestion = Some(false), professionalSubscriptionsQuestion = Some(false), otherAndCapitalAllowancesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYear)) { _ => result }


        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }

      "it's a prior submission and user attempts to view jobExpensesQuestion page but claimingEmploymentExpenses is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true,
          expensesCya = ExpensesCYAModel(emptyExpensesViewModel.copy(isUsingCustomerData = true)))))(
          cya => ExpensesRedirectService.commonExpensesRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }

      "it's a prior submission and user attempts to view jobExpenses amount page but jobExpensesQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true, expensesCya = expensesCYAModel(
          fullExpensesViewModel.copy(jobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.jobExpensesAmountRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }

      "it's a prior submission and user attempts to view flatRate amount page but flatRateJobExpensesQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true, expensesCya = expensesCYAModel(
          fullExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.flatRateAmountRedirect(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }

      "it's a prior submission and user attempts to view Professional Subscriptions amount page but professionalSubscriptionsQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true, expensesCya = expensesCYAModel(
          fullExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(false))))))(
          cya => ExpensesRedirectService.professionalSubscriptionsAmountRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }

      "it's a prior submission and user attempts to view Other Equipment amount page but otherAndCapitalAllowancesQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true, expensesCya = expensesCYAModel(
          fullExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }

      "it's a prior submission and user attempts to view Other Equipment page but claimingEmploymentExpenses is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true,
          expensesCya = ExpensesCYAModel(emptyExpensesViewModel.copy(isUsingCustomerData = true)))))(
          cya => ExpensesRedirectService.otherAllowancesRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }

      "there is no expenses user data" in {
        val response = redirectBasedOnCurrentAnswers(taxYear, data = None)(
          cya => ExpensesRedirectService.otherAllowancesRedirects(cya, taxYear)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }

      "continue with request if all previous required fields are filled" when {
        "it's a new submission" in {
          val response = redirectBasedOnCurrentAnswers(taxYear, Some(expensesUserData))(cya =>
            ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYear)) { _ => result }

          status(response) shouldBe OK
          bodyOf(response) shouldBe "OK"
        }
        "it's a prior submission" in {
          val response = redirectBasedOnCurrentAnswers(taxYear, Some(expensesUserData.copy(hasPriorExpenses = true, isPriorSubmission = true)))(cya =>
            ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYear)) { _ => result }

          status(response) shouldBe OK
          bodyOf(response) shouldBe "OK"
        }
      }
    }
  }
}
