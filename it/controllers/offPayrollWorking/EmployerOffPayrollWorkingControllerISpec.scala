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

import forms.YesNoForm
import models.tailoring.ExcludedJourneysResponseModel
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import utils.PageUrls.{employerOffPayrollWorkingUrl, fullUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class EmployerOffPayrollWorkingControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper{

  val url: String = fullUrl(employerOffPayrollWorkingUrl(taxYearEOY))

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "Render the employer off payroll working page" in {
      lazy val result: WSResponse = {
        authoriseIndividual()
        excludeStub(ExcludedJourneysResponseModel(Seq()),"1234567890", taxYearEOY)
        urlGet(url, false, false, Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      result.status shouldBe OK
    }

    "redirect to submission overview when OPW feature switch is set to false" in {
      val request = FakeRequest("GET", employerOffPayrollWorkingUrl(taxYear)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear))
      lazy val result: Future[Result] = {
        dropEmploymentDB()
        authoriseIndividual()
        route(appWithFeatureSwitchesOff, request, "{}").get
      }
      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }
  }

    "Return a bad request when the selection is invalid" in {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      lazy val result: WSResponse = {
        authoriseIndividual()
        urlPost(url, body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      result.status shouldBe BAD_REQUEST
    }
 }