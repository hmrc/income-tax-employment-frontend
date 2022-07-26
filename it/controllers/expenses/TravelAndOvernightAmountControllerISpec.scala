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

package controllers.expenses

import forms.AmountForm
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import support.builders.models.expenses.ExpensesBuilder.anExpenses
import support.builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserData
import support.builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import support.builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import utils.PageUrls.{businessTravelExpensesUrl, checkYourExpensesUrl, fullUrl, overviewUrl, travelAmountExpensesUrl, uniformsWorkClothesToolsExpensesUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class TravelAndOvernightAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val newAmount = 25

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    anExpensesUserData.copy(isPriorSubmission = isPrior, hasPriorExpenses = hasPriorExpenses, expensesCya = expensesCyaModel)

  private def expensesViewModel(jobExpensesQuestion: Option[Boolean] = None, jobExpenses: Option[BigDecimal] = None): ExpensesViewModel =
    ExpensesViewModel(isUsingCustomerData = true, claimingEmploymentExpenses = true, jobExpensesQuestion = jobExpensesQuestion, jobExpenses = jobExpenses)

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" when {
    "display the 'Business travel and Overnight stays Amount' page with correct content and with no pre-filled form" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentData = anAllEmploymentData.copy(hmrcExpenses = Some(anEmploymentExpenses.copy(expenses = Some(anExpenses.copy(jobExpenses = None)))))
        userDataStub(anIncomeTaxUserData.copy(Some(employmentData)), nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel.copy(expensesViewModel(Some(true)))))
        urlGet(fullUrl(travelAmountExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe ""
        result.status shouldBe OK
      }
    }

    "display the 'Business travel and Overnight stays Amount' page with correct content and with pre-filled form" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentData = anAllEmploymentData
        userDataStub(anIncomeTaxUserData.copy(Some(employmentData)), nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel.copy(expensesViewModel(Some(true), Some(100)))))
        urlGet(fullUrl(travelAmountExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe "100"
        result.status shouldBe OK
      }
    }

    "the user has not answered 'yes' to the 'Business travel and Overnight Stays' question" should {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          anExpensesCYAModel.copy(expenses = anExpensesCYAModel.expenses.copy(jobExpensesQuestion = Some(false)))))
        urlGet(fullUrl(travelAmountExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the CheckEmploymentExpenses page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "the user has no cya data in session" should {

      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(travelAmountExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the CheckEmploymentExpenses page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "the user is in year" should {
      lazy val result: WSResponse = {
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          anExpensesCYAModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(travelAmountExpensesUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" when {
    "return an error when the flatRateJobExpenses amount is in the wrong format" which {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> "badThings")
      lazy val result: WSResponse = {
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, anExpensesCYAModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(travelAmountExpensesUrl(taxYearEOY)), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "the user is in year" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")
      lazy val result: WSResponse = {
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, anExpensesCYAModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(travelAmountExpensesUrl(taxYear)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }

      "not update the CYA model" in {
        findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get.expensesCya.expenses.jobExpenses shouldBe Some(BigDecimal(200))
      }
    }

    "there is no CYA data" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(travelAmountExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the CYA page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }

      "not update the CYA model" in {
        findExpensesCyaData(taxYearEOY, anAuthorisationRequest) shouldBe None
      }
    }

    "the user successfully submits a valid amount" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")
      lazy val result: WSResponse = {
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          anExpensesCYAModel.copy(expensesViewModel(Some(true)))))
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          anExpensesCYAModel.copy(anExpensesViewModel.copy(flatRateJobExpensesQuestion = None))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(travelAmountExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to Uniforms Work Clothes or Tools question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)) shouldBe true
      }

      "update the CYA model" in {
        findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get.expensesCya.expenses.jobExpenses shouldBe Some(newAmount)
      }
    }

    "jobExpensesQuestion is empty" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")

      lazy val result: WSResponse = {
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          ExpensesCYAModel(expensesViewModel(None))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(travelAmountExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to Business Travel and Overnight question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(businessTravelExpensesUrl(taxYearEOY)) shouldBe true
      }
    }
  }
}
