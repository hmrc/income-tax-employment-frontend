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
    submittedOn = Some(s"${taxYearEOY-1}-11-11"),
    isUsingCustomerData = true
  )

  private def expensesCYAModel(expensesModel: ExpensesViewModel) = ExpensesCYAModel(expenses = expensesModel)

  private val expensesUserData = ExpensesUserData(
    sessionId, mtditid, nino, taxYearEOY, isPriorSubmission = false, hasPriorExpenses = false, expensesCya = ExpensesCYAModel(fullExpensesViewModel))


  "expensesSubmitRedirect" should {
    "redirect to the CYA page if the journey is finished" in {
      val result = Future.successful(ExpensesRedirectService.expensesSubmitRedirect(expensesCYAModel(fullExpensesViewModel), Call("GET", "/next"))(taxYearEOY))
      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe CheckEmploymentExpensesController.show(taxYearEOY).url
    }

    "redirect to next page if the journey is not finished" in {
      val result = Future.successful(ExpensesRedirectService.expensesSubmitRedirect(expensesCYAModel(emptyExpensesViewModel.copy(
        claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false), flatRateJobExpensesQuestion = Some(false))),
        Call("GET", "/next"))(taxYearEOY))
      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe "/next"
    }
  }

  "redirectBasedOnCurrentAnswers" should {
    "redirect to jobExpenses Question page" when {
      "it's a new submission and user attempts to view jobExpenses amount page but jobExpensesQuestion is None" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = expensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = None, jobExpenses = None, isUsingCustomerData = false)))))(
          cya => ExpensesRedirectService.jobExpensesAmountRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe BusinessTravelOvernightExpensesController.show(taxYearEOY).url
      }
      "it's a new submission and user attempts to view Uniforms Or Tools Question page but jobExpensesQuestion is None" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = expensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = None, jobExpenses = None, isUsingCustomerData = false)))))(
          cya => ExpensesRedirectService.flatRateRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe BusinessTravelOvernightExpensesController.show(taxYearEOY).url
      }
    }

    "redirect to jobExpenses amount page" when {
      "it's a new submission and user attempts to view flatRate Question page but jobExpenses amount is empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(true))))))(
          cya => ExpensesRedirectService.flatRateRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelAndOvernightAmountController.show(taxYearEOY).url
      }
    }

    "redirect to Uniforms or Tools (flatRate) Question page" when {
      "it's a new submission and user attempts to view flatRate amount page but flatRateJobExpensesQuestion is None" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.flatRateAmountRedirect(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UniformsOrToolsExpensesController.show(taxYearEOY).url
      }
      "it's a new submission and user attempts to view jobExpenses amount page but jobExpensesQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(isPriorSubmission = false, hasPriorExpenses = false, expensesCya = expensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.jobExpensesAmountRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UniformsOrToolsExpensesController.show(taxYearEOY).url
      }
    }

    "redirect to Uniforms or Tools (flatRate) amount page" when {
      "it's a new submission and user attempts to view Professional Subscriptions question page but flatRateJobExpenses is empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false), flatRateJobExpensesQuestion = Some(true))))))(
          cya => ExpensesRedirectService.professionalSubscriptionsRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UniformsOrToolsExpensesAmountController.show(taxYearEOY).url
      }
    }

    "redirect to Professional Fees and Subscriptions question page" when {
      "it's a new submission and user attempts to view Professional Subscriptions amount page but professionalSubscriptionsQuestion is None" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false), flatRateJobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.professionalSubscriptionsAmountRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ProfessionalFeesAndSubscriptionsExpensesController.show(taxYearEOY).url
      }
      "it's a new submission and user attempts to view Uniforms or Tools (flatRate) amount page but flatRateJobExpenses is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false), flatRateJobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.flatRateAmountRedirect(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ProfessionalFeesAndSubscriptionsExpensesController.show(taxYearEOY).url
      }

    }

    "redirect to Professional Fees and Subscriptions amount page" when {
      "it's a new submission and user attempts to view Other Equipment question page but professionalSubscriptionsQuestion is empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false),
            flatRateJobExpensesQuestion = Some(false), professionalSubscriptionsQuestion = Some(true))))))(
          cya => ExpensesRedirectService.otherAllowancesRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ProfFeesAndSubscriptionsExpensesAmountController.show(taxYearEOY).url
      }
    }

    "redirect to Other Equipment (otherAndCapitalAllowances) question page" when {
      "it's a new submission and user attempts to view Other Equipment amount page but otherAndCapitalAllowancesQuestion is None" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false),
            flatRateJobExpensesQuestion = Some(false), professionalSubscriptionsQuestion = Some(false))))))(
          cya => ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe OtherEquipmentController.show(taxYearEOY).url
      }
      "it's a new submission and user attempts to view Professional Subscriptions amount page but professionalSubscriptionsQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false),
            flatRateJobExpensesQuestion = Some(false), professionalSubscriptionsQuestion = Some(false))))))(
          cya => ExpensesRedirectService.professionalSubscriptionsAmountRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe OtherEquipmentController.show(taxYearEOY).url
      }
    }

    "redirect to Check Employment Expenses page" when {
      "it's a new submission and user attempts to view jobExpensesQuestion page but claimingEmploymentExpenses is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(isPriorSubmission = false, hasPriorExpenses = false,
          expensesCya = ExpensesCYAModel(emptyExpensesViewModel))))(
          cya => ExpensesRedirectService.commonExpensesRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYearEOY).url
      }

      "it's a new submission and user attempts to view Other Equipment amount page but otherAndCapitalAllowancesQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(expensesCya = ExpensesCYAModel(
          emptyExpensesViewModel.copy(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false),
            flatRateJobExpensesQuestion = Some(false), professionalSubscriptionsQuestion = Some(false), otherAndCapitalAllowancesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYearEOY)) { _ => result }


        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYearEOY).url
      }

      "it's a prior submission and user attempts to view jobExpensesQuestion page but claimingEmploymentExpenses is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true,
          expensesCya = ExpensesCYAModel(emptyExpensesViewModel.copy(isUsingCustomerData = true)))))(
          cya => ExpensesRedirectService.commonExpensesRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYearEOY).url
      }

      "it's a prior submission and user attempts to view jobExpenses amount page but jobExpensesQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true, expensesCya = expensesCYAModel(
          fullExpensesViewModel.copy(jobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.jobExpensesAmountRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYearEOY).url
      }

      "it's a prior submission and user attempts to view flatRate amount page but flatRateJobExpensesQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true, expensesCya = expensesCYAModel(
          fullExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.flatRateAmountRedirect(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYearEOY).url
      }

      "it's a prior submission and user attempts to view Professional Subscriptions amount page but professionalSubscriptionsQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true, expensesCya = expensesCYAModel(
          fullExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(false))))))(
          cya => ExpensesRedirectService.professionalSubscriptionsAmountRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYearEOY).url
      }

      "it's a prior submission and user attempts to view Other Equipment amount page but otherAndCapitalAllowancesQuestion is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true, expensesCya = expensesCYAModel(
          fullExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = Some(false))))))(
          cya => ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYearEOY).url
      }

      "it's a prior submission and user attempts to view Other Equipment page but claimingEmploymentExpenses is false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = Some(expensesUserData.copy(isPriorSubmission = true, hasPriorExpenses = true,
          expensesCya = ExpensesCYAModel(emptyExpensesViewModel.copy(isUsingCustomerData = true)))))(
          cya => ExpensesRedirectService.otherAllowancesRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYearEOY).url
      }

      "there is no expenses user data" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, data = None)(
          cya => ExpensesRedirectService.otherAllowancesRedirects(cya, taxYearEOY)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentExpensesController.show(taxYearEOY).url
      }

      "continue with request if all previous required fields are filled" when {
        "it's a new submission" in {
          val response = redirectBasedOnCurrentAnswers(taxYearEOY, Some(expensesUserData))(cya =>
            ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYearEOY)) { _ => result }

          status(response) shouldBe OK
          bodyOf(response) shouldBe "OK"
        }
        "it's a prior submission" in {
          val response = redirectBasedOnCurrentAnswers(taxYearEOY, Some(expensesUserData.copy(hasPriorExpenses = true, isPriorSubmission = true)))(cya =>
            ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYearEOY)) { _ => result }

          status(response) shouldBe OK
          bodyOf(response) shouldBe "OK"
        }
      }
    }
  }
}
