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

package controllers.employment

import models.IncomeTaxUserData
import models.employment.AllEmploymentData
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER, UNAUTHORIZED}
import play.api.libs.ws.WSResponse
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmploymentSummaryControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" when {
    "render page when EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq()))), nino, taxYearEOY)
        urlGet(fullUrl(employmentSummaryUrl(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "status OK" in {
        result.status shouldBe OK
      }
    }

    "render page when in year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq()))), nino, taxYear)
        urlGet(fullUrl(employmentSummaryUrl(taxYear)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "status OK" in {
        result.status shouldBe OK
      }
    }

    "redirect when there is employment data returned but no hmrc employment data" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = true)
        userDataStub(IncomeTaxUserData(Some(AllEmploymentData(Seq(), None, Seq(anEmploymentSource), None))), nino, taxYear)
        urlGet(fullUrl(employmentSummaryUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "status OK" in {
        result.status shouldBe SEE_OTHER
      }
    }

    "redirect the User to the Overview page no data in session" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = true)
        userDataStub(IncomeTaxUserData(), nino, taxYear)
        urlGet(fullUrl(employmentSummaryUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYear)) shouldBe true
      }

    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        unauthorisedAgentOrIndividual(isAgent = true)
        urlGet(fullUrl(employmentSummaryUrl(taxYear)))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }
}
