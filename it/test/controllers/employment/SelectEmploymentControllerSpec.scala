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
import play.api.http.Status.{NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import utils.PageUrls.{fullUrl, selectEmployerUrl}
import utils.{IntegrationTest, PageUrls, ViewHelpers}

class SelectEmploymentControllerSpec extends IntegrationTest with ViewHelpers {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "Render select employer page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(employment = anIncomeTaxUserData.employment.map(_.copy(hmrcEmploymentData = Seq(
          anIncomeTaxUserData.employment.get.hmrcEmploymentData.head.copy(dateIgnored = Some("2019-04-21"))
        )))), nino, taxYearEOY)
        urlGet(fullUrl(selectEmployerUrl(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
      result.body should include("Which period of employment do you want to add?")
    }
  }

  ".submit" should {
    "submit employer form data" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(employment = anIncomeTaxUserData.employment.map(_.copy(hmrcEmploymentData = Seq(
          anIncomeTaxUserData.employment.get.hmrcEmploymentData.head.copy(dateIgnored = Some("2019-04-21"))
        )))), nino, taxYearEOY)

        val url = s"/income-tax-employment/income-tax/nino/$nino/sources/employmentId/unignore\\?taxYear=$taxYearEOY"

        stubDeleteWithHeadersCheck(url, NO_CONTENT, "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(selectEmployerUrl(taxYearEOY)), body = Map("value" -> "employmentId"), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header(name = "location").get shouldBe PageUrls.employmentSummaryUrl(taxYearEOY)
    }
  }
}
