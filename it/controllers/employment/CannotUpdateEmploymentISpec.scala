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

import play.api.http.HeaderNames
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.ws.WSResponse
import utils.PageUrls.fullUrl
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CannotUpdateEmploymentISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" when {
    "should render the page" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(s"$appUrl/$taxYear/cannot-edit", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe OK
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        unauthorisedAgentOrIndividual(isAgent = false)
        urlGet(s"$appUrl/$taxYear/cannot-edit", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

}
