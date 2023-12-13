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
import models.mongo.EmploymentUserData
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithDetails}
import utils.PageUrls.{employerOffPayrollWorkingUrl, employerOffPayrollWorkingWarningUrl, fullUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class EmployerOffPayrollWorkingControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper{

  private val employmentId: String = anEmploymentUserData.employmentId
  val url: String = fullUrl(employerOffPayrollWorkingUrl(taxYearEOY, employmentId))

  private def cya(isPriorSubmission: Boolean = true): EmploymentUserData =
    anEmploymentUserDataWithDetails(
      anEmploymentDetails.copy("hmrc"),
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = isPriorSubmission
    )

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "Render the employer off payroll working page" in {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya())
        authoriseIndividual()
        urlGet(url, false, false, Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      result.status shouldBe OK
    }

    "redirect to submission overview when OPW feature switch is set to false" in {
      val request = FakeRequest("GET", employerOffPayrollWorkingUrl(taxYear, employmentId)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear))
      lazy val result: Future[Result] = {
        dropEmploymentDB()
        authoriseIndividual()
        route(appWithFeatureSwitchesOff, request, "{}").get
      }
      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }
  }

  ".submit" should {
    "redirect to Overview Page when in year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerOffPayrollWorkingUrl(taxYear, employmentId)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }

    s"render page with an error when validation fails" in {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya())
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerOffPayrollWorkingUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe BAD_REQUEST
    }

    "return next page when answer is no" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerOffPayrollWorkingUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to OffPayroll Working Warning page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe employerOffPayrollWorkingWarningUrl(taxYearEOY, employmentId)
      }
    }

    "return next page when answer is yes" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerOffPayrollWorkingUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to OffPayroll Working Warning page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe employerOffPayrollWorkingWarningUrl(taxYearEOY, employmentId)
      }
    }
  }
 }