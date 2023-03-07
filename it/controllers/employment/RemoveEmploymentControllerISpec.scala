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

package controllers.employment

import models.IncomeTaxUserData
import models.employment.{AllEmploymentData, EmploymentSource, HmrcEmploymentSource}
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import utils.PageUrls.{employmentSummaryUrl, fullUrl, overviewUrl, removeEmploymentUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class RemoveEmploymentControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  private val employmentId: String = "employmentId"

  private val modelWithMultipleSources: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      HmrcEmploymentSource(employmentId = "002", employerName = "apple", None, None, None, None, None, None, None, None),
      HmrcEmploymentSource(employmentId = "003", employerName = "google", None, None, None, None, None, None, None, None)
    ),
    hmrcExpenses = None,
    customerEmploymentData = Seq(
      EmploymentSource(employmentId = "employmentId", employerName = "maggie", None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "004", employerName = "microsoft", None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "005", employerName = "name", None, None, None, None, None, None, None, None)
    ),
    customerExpenses = None
  )

  ".show" should {
    "render the remove employment page for when it isn't the last employment" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(IncomeTaxUserData(Some(modelWithMultipleSources)), nino, taxYearEOY)
        urlGet(fullUrl(removeEmploymentUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK ($OK) status" in {
        result.status shouldBe OK
      }
    }
    "render the remove employment page for when it isn't the last employment and removing a hmrc employment" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(IncomeTaxUserData(Some(modelWithMultipleSources)), nino, taxYearEOY)
        urlGet(fullUrl(removeEmploymentUrl(taxYearEOY, "002")), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK ($OK) status" in {
        result.status shouldBe OK
      }
    }

    "render the remove employment page for when it's the last employment" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY)
        urlGet(fullUrl(removeEmploymentUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK ($OK) status" in {
        result.status shouldBe OK
      }
    }


    "redirect to the overview page" when {
      "it is not end of year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYear)
          urlGet(fullUrl(removeEmploymentUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "the user does not have employment data with that employmentId" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY)
          urlGet(fullUrl(removeEmploymentUrl(taxYearEOY, "123")), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }

  ".submit" should {

    "redirect the user to the overview page" when {
      "it is not end of year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(removeEmploymentUrl(taxYear, employmentId)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "the user does not have employment data with that employmentId" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(modelWithMultipleSources)), nino, taxYearEOY)
          urlPost(fullUrl(removeEmploymentUrl(taxYearEOY, "123")), body = "",
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYearEOY)) shouldBe true
        }
      }

      "there is no employment data found" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
          urlPost(fullUrl(removeEmploymentUrl(taxYearEOY, employmentId)), body = "",
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYearEOY)) shouldBe true
        }
      }
    }

    "redirect to the employment summary page" when {

      "an employment is removed" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(modelWithMultipleSources)), nino, taxYearEOY)
          userDataStubDeleteOrIgnoreEmployment(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY, employmentId, "CUSTOMER")
          urlPost(fullUrl(removeEmploymentUrl(taxYearEOY, employmentId)), body = "",
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the employment summary page" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
        }
      }

      "the last employment is removed" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY)
          userDataStubDeleteOrIgnoreEmployment(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY, employmentId, "HMRC-HELD")
          userDataStubDeleteExpenses(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY, "HMRC-HELD")
          urlPost(fullUrl(removeEmploymentUrl(taxYearEOY, employmentId)), body = "",
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
