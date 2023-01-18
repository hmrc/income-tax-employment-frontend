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

import forms.YesNoForm
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import utils.PageUrls.{checkYourExpensesUrl, claimEmploymentExpensesUrl, fullUrl, overviewUrl, startEmploymentExpensesUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmploymentExpensesControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    ExpensesUserData(sessionId, mtditid, nino, taxYear - 1, isPriorSubmission = isPrior, hasPriorExpenses, expensesCyaModel)

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render 'Do you want to claim employment expenses?' page with the correct content and no values pre-filled when no user data" which {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          ExpensesCYAModel(ExpensesViewModel(isUsingCustomerData = false))))
        urlGet(fullUrl(claimEmploymentExpensesUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "return a redirect when in year" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)

          userDataStub(anIncomeTaxUserData, nino, taxYear)
          urlGet(fullUrl(claimEmploymentExpensesUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has a url of overview page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }
    }
  }

  ".submit" should {
    s"return a BAD_REQUEST($BAD_REQUEST) status" when {
      "the value is empty" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
        lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(claimEmploymentExpensesUrl(taxYearEOY)), body = form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }
    }

    "redirect to another page when a valid request is made and then" should {
      "redirect to 'check your expenses', update claimingEmploymentExpenses to no and wipe the expenses amounts when the user chooses no" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropExpensesDB()
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(claimEmploymentExpensesUrl(taxYearEOY)), body = form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get

          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe false
          cyaModel.expensesCya.expenses.jobExpensesQuestion shouldBe None
          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe None
          cyaModel.expensesCya.expenses.otherAndCapitalAllowancesQuestion shouldBe None
          cyaModel.expensesCya.expenses.businessTravelCosts shouldBe None
          cyaModel.expensesCya.expenses.jobExpenses shouldBe None
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe None
          cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe None
          cyaModel.expensesCya.expenses.hotelAndMealExpenses shouldBe None
          cyaModel.expensesCya.expenses.otherAndCapitalAllowances shouldBe None
          cyaModel.expensesCya.expenses.vehicleExpenses shouldBe None
          cyaModel.expensesCya.expenses.mileageAllowanceRelief shouldBe None
        }
      }

      "redirect to 'expenses interrupt' page and update claimingEmploymentExpenses to yes when the user chooses yes" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
            ExpensesCYAModel(ExpensesViewModel(isUsingCustomerData = false))))
          urlPost(fullUrl(claimEmploymentExpensesUrl(taxYearEOY)), body = form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the 'expenses interrupt' page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(startEmploymentExpensesUrl(taxYearEOY)) shouldBe true

          lazy val cyaModel = findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get
          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
          cyaModel.expensesCya.expenses.jobExpensesQuestion shouldBe None
          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe None
          cyaModel.expensesCya.expenses.otherAndCapitalAllowancesQuestion shouldBe None
          cyaModel.expensesCya.expenses.businessTravelCosts shouldBe None
          cyaModel.expensesCya.expenses.jobExpenses shouldBe None
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe None
          cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe None
          cyaModel.expensesCya.expenses.hotelAndMealExpenses shouldBe None
          cyaModel.expensesCya.expenses.otherAndCapitalAllowances shouldBe None
          cyaModel.expensesCya.expenses.vehicleExpenses shouldBe None
          cyaModel.expensesCya.expenses.mileageAllowanceRelief shouldBe None
        }
      }

      "return a redirect when in year" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(anIncomeTaxUserData, nino, taxYear)
          urlPost(fullUrl(claimEmploymentExpensesUrl(taxYear)), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        lazy val document = Jsoup.parse(result.body)

        implicit def documentSupplier: () => Document = () => document

        "has a url of overview page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }
    }
  }
}
