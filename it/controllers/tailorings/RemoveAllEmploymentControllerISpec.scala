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

package controllers.tailorings

import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import utils.PageUrls.{fullUrl, removeAllEmploymentUrl}
import utils.{IntegrationTest, ViewHelpers}

class RemoveAllEmploymentControllerISpec extends IntegrationTest with ViewHelpers {

  val url: String = fullUrl(removeAllEmploymentUrl(taxYearEOY))

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "Render the remove all employment page" in {
      lazy val result: WSResponse = {
        authoriseIndividual()
        urlGet(url, false, false, Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }
  }

  ".submit" should {
    "Redirect to employment summary after making the correct calls" in {
      lazy val result: WSResponse = {
        authoriseIndividual()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        userDataStubDeleteOrIgnoreEmployment(anIncomeTaxUserData, nino, taxYearEOY, "employmentId", "HMRC-HELD")
        userDataStubDeleteExpenses(anIncomeTaxUserData, nino, taxYearEOY, "HMRC-HELD")
        excludePostStub(nino, taxYearEOY)
        urlPost(url, body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      result.status shouldBe SEE_OTHER
      result.headers("Location").contains(controllers.employment.routes.EmploymentSummaryController.show(taxYearEOY).url) shouldBe true
    }
    "Redirect to summary page when there is no priorData" in {
      lazy val result: WSResponse = {
        authoriseIndividual()
        userDataStub(anIncomeTaxUserData.copy(employment = None), nino, taxYearEOY)
        userDataStubDeleteOrIgnoreEmployment(anIncomeTaxUserData, nino, taxYearEOY, "employmentId", "HMRC-HELD")
        userDataStubDeleteExpenses(anIncomeTaxUserData, nino, taxYearEOY, "HMRC-HELD")
        excludePostStub(nino, taxYearEOY)
        urlPost(url, body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      result.status shouldBe SEE_OTHER
      result.headers("Location").contains(controllers.employment.routes.EmploymentSummaryController.show(taxYearEOY).url) shouldBe true
    }
    "Return an error when tailoring call fails" in {
      lazy val result: WSResponse = {
        authoriseIndividual()
        userDataStub(anIncomeTaxUserData.copy(employment = None), nino, taxYearEOY)
        userDataStubDeleteOrIgnoreEmployment(anIncomeTaxUserData, nino, taxYearEOY, "employmentId", "HMRC-HELD")
        userDataStubDeleteExpenses(anIncomeTaxUserData, nino, taxYearEOY, "HMRC-HELD")
        excludePostStub(nino, taxYearEOY)
        urlPost(url, body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      result.status shouldBe SEE_OTHER
    }

  }
}
