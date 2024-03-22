/*
 * Copyright 2023 HM Revenue & Customs
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
import support.builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserData
import support.builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import support.builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import utils.PageUrls.{checkYourExpensesUrl, fullUrl, overviewUrl, professionalFeesExpensesUrl, uniformsClothesToolsExpensesAmountUrl, uniformsWorkClothesToolsExpensesUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class UniformsOrToolsExpensesAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val newAmount: BigDecimal = 250

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    anExpensesUserData.copy(isPriorSubmission = isPrior, hasPriorExpenses = hasPriorExpenses, expensesCya = expensesCyaModel)

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render 'How much do you want to claim for uniforms, work clothes, or tools?' page with the correct content and" +
      " no pre-filled amount no values pre-filled when no user data" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel.copy(expenses = anExpensesViewModel.copy(flatRateJobExpenses = None))))
        urlGet(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe ""
        result.status shouldBe OK
      }
    }

    "render 'How much do you want to claim for uniforms, work clothes, or tools?' page with the correct content and" +
      " pre-filled amount no values pre-filled when no user data" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel.copy(expenses = anExpensesViewModel.copy(flatRateJobExpenses = Some(100)))))
        urlGet(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe "100"
        result.status shouldBe OK
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "Redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(anIncomeTaxUserData, nino, taxYear)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, anExpensesCYAModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYearEOY)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }

      "redirect to the check your expenses page when there is a flatRateJobExpenses amount but the flatRateJobExpensesQuestion is false" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropExpensesDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
            anExpensesCYAModel.copy(expenses = anExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(false)))))
          urlGet(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }

  ".submit" should {
    "return an error when the flatRateJobExpenses amount is in the wrong format" which {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> "abc")
      lazy val result: WSResponse = {
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, anExpensesCYAModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYearEOY)), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))

      }

      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to another page when valid request is made and then" should {
      "redirect to next page and update flatRateJobExpenses to the new amount when not in year and not a prior submission" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
        lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
            ExpensesCYAModel(ExpensesViewModel(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false),
              flatRateJobExpensesQuestion = Some(true), isUsingCustomerData = true))))
          urlPost(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYearEOY)), body = form,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to professional fees and subscriptions question page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(professionalFeesExpensesUrl(taxYearEOY)) shouldBe true
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get

          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
          cyaModel.expensesCya.expenses.jobExpensesQuestion shouldBe Some(false)
          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe Some(newAmount)
        }
      }

      "redirect to flatRate Question page when flatRateJobExpensesQuestion is None" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
        lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
            ExpensesCYAModel(ExpensesViewModel(claimingEmploymentExpenses = true, jobExpensesQuestion = Some(false),
              flatRateJobExpensesQuestion = None, isUsingCustomerData = true))))
          urlPost(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYearEOY)), body = form,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to flatRate Question page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)) shouldBe true
        }
      }

      "redirect to 'check your expenses' page when a prior submission and update flatRateJobExpenses to the new amount" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
        lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYearEOY)), body = form,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER

          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get

          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
          cyaModel.expensesCya.expenses.jobExpensesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.otherAndCapitalAllowancesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.businessTravelCosts shouldBe Some(100.00)
          cyaModel.expensesCya.expenses.jobExpenses shouldBe Some(200.00)
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe Some(newAmount)
          cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe Some(400.00)
          cyaModel.expensesCya.expenses.hotelAndMealExpenses shouldBe Some(500.00)
          cyaModel.expensesCya.expenses.otherAndCapitalAllowances shouldBe Some(600.00)
          cyaModel.expensesCya.expenses.vehicleExpenses shouldBe Some(700.00)
          cyaModel.expensesCya.expenses.mileageAllowanceRelief shouldBe Some(800.00)
        }

      }

      "Redirect user to the tax overview page when in year" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

        lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(anIncomeTaxUserData, nino, taxYear)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, anExpensesCYAModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYear)), body = form,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(uniformsClothesToolsExpensesAmountUrl(taxYearEOY)), body = form,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }
}
