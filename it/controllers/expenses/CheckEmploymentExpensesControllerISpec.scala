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

import common.SessionValues
import controllers.expenses.routes.TravelAndOvernightAmountController
import helpers.SessionCookieCrumbler.getSessionMap
import models.IncomeTaxUserData
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import models.requests.CreateUpdateExpensesRequest
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import play.api.{Environment, Mode}
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.builders.models.expenses.ExpensesBuilder.anExpenses
import support.builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class CheckEmploymentExpensesControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private def expensesUserData(expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    ExpensesUserData(sessionId, mtditid, nino, taxYear - 1, isPriorSubmission = true, hasPriorExpenses = true, expensesCyaModel)

  ".show" when {
    "render page and return OK when in year" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentExpenses = anEmploymentExpenses.copy(expenses = Some(anExpenses.copy(professionalSubscriptions = None)))
        userDataStub(IncomeTaxUserData(Some(anAllEmploymentData.copy(hmrcExpenses = Some(employmentExpenses)))), nino, taxYear)
        urlGet(fullUrl(checkYourExpensesUrl(taxYear)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "render page and return OK when end of year" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlGet(fullUrl(checkYourExpensesUrl(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect to overview page when all the fields are populated at the end of the year and employmentEOYEnabled is false" in {
      implicit lazy val result: Future[Result] = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val request = FakeRequest("GET", checkYourExpensesUrl(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
        route(GuiceApplicationBuilder().in(Environment.simple(mode = Mode.Dev))
          .configure(config() + ("feature-switch.employmentEOYEnabled" -> "false"))
          .build(),
          request,
          "{}").get
      }

      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }

    "redirect to page with not finished data when Cya exists and not finished" in {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined).copy(jobExpenses = None))))
        urlGet(fullUrl(checkYourExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(TravelAndOvernightAmountController.show(taxYearEOY).url)
    }

    "redirect to overview page when theres no expenses" in {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None))), nino, taxYear)
        urlGet(fullUrl(checkYourExpensesUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }

    "redirect to employment expenses page when no expenses has been added yet (making a new employment journey)" in {
      val customerData = anEmploymentSource
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None, customerEmploymentData = Seq(customerData)))), nino, taxYear - 1)
        urlGet(fullUrl(checkYourExpensesUrl(taxYear - 1)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1, Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> customerData.employmentId))))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(claimEmploymentExpensesUrl(taxYearEOY)) shouldBe true
    }

    "redirect to 'do you need to add any additional new expenses?' page when there's prior expenses (making a new employment journey)" in {
      val customerData = anEmploymentSource

      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(customerEmploymentData = Seq(customerData)))), nino, taxYear - 1)
        urlGet(fullUrl(checkYourExpensesUrl(taxYear - 1)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1, Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> customerData.employmentId))))
      }

      result.status shouldBe SEE_OTHER
      //TODO: add a redirect for "do you need to add any additional/new expenses?" page when available
      result.header("location").contains(claimEmploymentExpensesUrl(taxYearEOY)) shouldBe true
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        dropExpensesDB()
        unauthorisedAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(checkYourExpensesUrl(taxYear)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

  ".submit" when {
    "return a redirect when in year" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlPost(fullUrl(checkYourExpensesUrl(taxYear)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect when at the end of the year when no cya data" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has a url of expenses show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the missing section if the expense questions are incomplete when submitting CYA data at the end of the year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined).copy(professionalSubscriptionsQuestion = None))))

        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has a url of expenses show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(professionalFeesExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the first missing section if there are more than one incomplete expense questions when submitting CYA data at the end of the year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(ExpensesCYAModel(anExpensesViewModel.copy(
          professionalSubscriptionsQuestion = None, otherAndCapitalAllowancesQuestion = None))))

        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has a url of expenses show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(professionalFeesExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "create the model to update the data and return the correct redirect when no customer data and cya data submitted cya data is different from hmrc expense" which {
      implicit lazy val result: WSResponse = {
        val newAmount = BigDecimal(10000.99)
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined).copy(
          professionalSubscriptions = Some(newAmount)))))

        val model = CreateUpdateExpensesRequest(
          Some(true), anExpenses.copy(professionalSubscriptions = Some(newAmount))
        )

        stubPutWithHeadersCheck(s"/income-tax-expenses/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an SEE OTHER status and cyaData cleared as data was submitted" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
        findExpensesCyaData(taxYear - 1, anAuthorisationRequest) shouldBe None
        getSessionMap(result, "mdtp").get("TEMP_NEW_EMPLOYMENT_ID") shouldBe None
      }
    }

    "create the model to update the data and return redirect when there is no customer expenses and nothing has changed in relation to hmrc expenses" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined))))

        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an SEE OTHER status and cyaData not cleared as no changes were made" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
        findExpensesCyaData(taxYear - 1, anAuthorisationRequest) shouldBe defined
        getSessionMap(result, "mdtp").get("TEMP_NEW_EMPLOYMENT_ID") shouldBe None
      }
    }

    "create the model to update the data and return redirect when there are no hmrc expenses and nothing has changed in relation to customer expenses" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None, customerExpenses = Some(anEmploymentExpenses)))), nino, taxYear - 1)
        insertExpensesCyaData(expensesUserData(ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined))))
        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an SEE OTHER status and cyaData not cleared as no changes were made" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
        findExpensesCyaData(taxYear - 1, anAuthorisationRequest) shouldBe defined
        getSessionMap(result, "mdtp").get("TEMP_NEW_EMPLOYMENT_ID") shouldBe None
      }
    }

    "redirect to overview page when employmentEOYEnabled is false" in {
      implicit val result: Future[Result] = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None, customerExpenses = Some(anEmploymentExpenses)))), nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined))))
        val headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), "Csrf-Token" -> "nocheck")
        val request = FakeRequest("POST", checkYourExpensesUrl(taxYearEOY)).withHeaders(headers: _*)
        route(GuiceApplicationBuilder().in(Environment.simple(mode = Mode.Dev))
          .configure(config() + ("feature-switch.employmentEOYEnabled" -> "false"))
          .build(),
          request,
          "{}").get
      }

      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }
  }
}
