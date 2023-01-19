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

import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER, UNAUTHORIZED}
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import utils.PageUrls.{employerInformationUrl, fullUrl, overviewUrl}
import utils.{IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class EmployerInformationControllerISpec extends IntegrationTest with ViewHelpers {

  private val employmentId = "employmentId"

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" when {
    "render the employer information page when in year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has OK status" in {
        result.status shouldBe OK
      }
    }

    "render the employer information page when end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(employerInformationUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "has OK status" in {
        result.status shouldBe OK
      }
    }

    "render the employer information page when the student loans feature switch is turned off in year" which {
      val headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear))
      val request = FakeRequest("GET", employerInformationUrl(taxYear, employmentId)).withHeaders(headers: _*)

      lazy val result: Future[Result] = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        route(appWithFeatureSwitchesOff, request, "{}").get
      }
      "has OK status" in {
        status(result) shouldBe OK
      }
    }

    "render the employer information page when the student loans feature switch is turned off end of year" which {
      val headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
      val request = FakeRequest("GET", employerInformationUrl(taxYearEOY, employmentId)).withHeaders(headers: _*)

      lazy val result: Future[Result] = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        route(appWithFeatureSwitchesOff, request, "{}").get
      }
      "has OK status" in {
        status(result) shouldBe OK
      }
    }
  }

  "redirect to the overview page when there is no data in year" in {
    lazy val result: WSResponse = {
      authoriseAgentOrIndividual(isAgent = false)
      userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq()))), nino, taxYear)
      urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
    }

    result.status shouldBe SEE_OTHER
    result.header("location").contains(overviewUrl(taxYear)) shouldBe true
  }

  "redirect to Unauthorised user error page when user is unauthorised" which {
    lazy val result: WSResponse = {
      unauthorisedAgentOrIndividual(isAgent = true)
      urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
    }
    "has an UNAUTHORIZED(401) status" in {
      result.status shouldBe UNAUTHORIZED
    }
  }
}
