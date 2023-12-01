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

package controllers.offPayrollWorking

import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{employerIncomeWarningUrl, fullUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class EmployerIncomeWarningControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper{
  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val employmentId: String = anEmploymentUserData.employmentId

  ".show" when {
    "render the correct view for an individual in year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employerIncomeWarningUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has OK status" in {
        result.status shouldBe OK
      }

      "redirect to submission overview when OPW feature switch is set to false" in {
        val request = FakeRequest("GET", employerIncomeWarningUrl(taxYear, employmentId)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear))
        lazy val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(appWithFeatureSwitchesOff, request, "{}").get
        }
        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

    }

    "render the correct view for an agent in year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = true)
        urlGet(fullUrl(employerIncomeWarningUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has OK status" in {
        result.status shouldBe OK
      }
    }
  }

}
