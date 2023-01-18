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

import forms.YesNoForm
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import utils.PageUrls.{employmentGatewayUrl, fullUrl}
import utils.{IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class EmploymentGatewayControllerISpec extends IntegrationTest with ViewHelpers {

  val url: String = fullUrl(employmentGatewayUrl(taxYearEOY))

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "Render the employment gateway page" in {
      lazy val result: WSResponse = {
        authoriseIndividual()
        urlGet(url, false, false, Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }

    "Redirect to overview page" when {
      "tailoring is disabled" in {
        val request = FakeRequest("GET", employmentGatewayUrl(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
        lazy val result: Future[Result] = {
          authoriseIndividual(true)
          route(appWithFeatureSwitchesOff, request, "{}").get
        }
        status(result) shouldBe SEE_OTHER
        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
      }
    }
  }

  ".submit" should {
    "Redirect to employment summary when 'yes' is selected" in {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        authoriseIndividual()
        urlPost(url, body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      result.status shouldBe SEE_OTHER
      result.headers("Location").contains(controllers.employment.routes.EmploymentSummaryController.show(taxYearEOY).url) shouldBe true
    }

    "Redirect to income tax submission overview when 'no' is selected" in {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        authoriseIndividual()
        urlPost(url, body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      result.status shouldBe SEE_OTHER
      result.headers("Location").contains(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)) shouldBe true
    }

    "Return a bad request when the selection is invalid" in {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      lazy val result: WSResponse = {
        authoriseIndividual()
        urlPost(url, body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      result.status shouldBe BAD_REQUEST
    }

    "Redirect to overview page" when {
      "tailoring is disabled" in {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
        val request = FakeRequest("POST", employmentGatewayUrl(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), "Csrf-Token" -> "nocheck")
        lazy val result: Future[Result] = {
          authoriseIndividual(true)
          route(appWithFeatureSwitchesOff, request, Json.toJson(form)).get
        }
        status(result) shouldBe SEE_OTHER
        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
      }
    }
  }
}
