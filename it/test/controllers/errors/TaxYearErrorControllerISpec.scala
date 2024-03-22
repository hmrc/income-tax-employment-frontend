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

package controllers.errors

import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import utils.PageUrls.{fullUrl, wrongTaxYearUrl}
import utils.{IntegrationTest, ViewHelpers}

class TaxYearErrorControllerISpec extends IntegrationTest with ViewHelpers {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" when {
    "render the error page with the right content for multiple Tax Years" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(wrongTaxYearUrl), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(invalidTaxYear, validTaxYears = validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "render the error page with the right content for a single TaxYear" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(wrongTaxYearUrl), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(invalidTaxYear, validTaxYears = validTaxYearListSingle)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }
  }
}
