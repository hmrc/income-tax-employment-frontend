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

import models.IncomeTaxUserData
import models.employment.{AllEmploymentData, EmploymentSource, HmrcEmploymentSource}
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import utils.PageUrls.{employmentSummaryUrl, fullUrl, overviewUrl, removeExpensesUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class RemoveExpensesControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val model: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      HmrcEmploymentSource(employmentId = "002", employerName = "apple", None, None, None, None, None, None, None, None, None),
      HmrcEmploymentSource(employmentId = "003", employerName = "google", None, None, None, None, None, None, None, None, None)
    ),
    hmrcExpenses = Some(anEmploymentExpenses),
    customerEmploymentData = Seq(
      EmploymentSource(employmentId = "001", employerName = "maggie", None, None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "004", employerName = "microsoft", None, None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "005", employerName = "name", None, None, None, None, None, None, None, None, None)
    ),
    customerExpenses = Some(anEmploymentExpenses),
    otherEmploymentIncome = None
  )

  private val modelToDelete: AllEmploymentData = model.copy(
    hmrcExpenses = Some(anEmploymentExpenses),
    customerExpenses = Some(anEmploymentExpenses)
  )

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the remove expenses page when user has expenses" which {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(IncomeTaxUserData(Some(model)), nino, taxYearEOY)
        urlGet(fullUrl(removeExpensesUrl(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK ($OK) status" in {
        result.status shouldBe OK
      }
    }

    "redirect to the overview page" when {
      "it is not end of year" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          userDataStub(IncomeTaxUserData(Some(modelToDelete)), nino, taxYear)
          urlGet(fullUrl(removeExpensesUrl(taxYear)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }
    }
  }

  ".submit" should {
    "redirect the user to the overview page" when {
      "it is not end of year" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          urlPost(fullUrl(removeExpensesUrl(taxYear)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "there is no employment data found" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
          urlPost(fullUrl(removeExpensesUrl(taxYearEOY)), body = "",
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYearEOY)) shouldBe true
        }
      }
    }

    "redirect to the employment summary page" when {
      "expenses is removed" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          userDataStub(IncomeTaxUserData(Some(modelToDelete)), nino, taxYearEOY)
          userDataStubDeleteExpenses(IncomeTaxUserData(Some(modelToDelete)), nino, taxYearEOY, "ALL")
          urlPost(fullUrl(removeExpensesUrl(taxYearEOY)), body = "",
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the employment summary page" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }
}
