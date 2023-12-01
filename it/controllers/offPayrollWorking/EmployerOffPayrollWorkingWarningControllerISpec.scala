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
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithDetails}
import utils.PageUrls.{didYouLeaveUrl, employerOffPayrollWorkingWarningUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import views.html.helper.form

import scala.concurrent.Future

class EmployerOffPayrollWorkingWarningControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {


  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  private val employmentId: String = anEmploymentUserData.employmentId
  lazy val offPayrollWorkingStatus: Boolean = false
  private def cya(offPayrollWorkingStatus: Option[Boolean] = Some(false), isPriorSubmission: Boolean = true): EmploymentUserData =
    anEmploymentUserDataWithDetails(
      anEmploymentDetails.copy("hmrc", offPayrollWorkingStatus = offPayrollWorkingStatus),
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = isPriorSubmission
    )

  ".show" when {
    "render the correct view for an individual in year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employerOffPayrollWorkingWarningUrl(taxYear, employmentId, offPayrollWorkingStatus)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has OK status" in {
        result.status shouldBe OK
      }

      "render the correct view for an agent in year" which {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = true)
          urlGet(fullUrl(employerOffPayrollWorkingWarningUrl(taxYear, employmentId, offPayrollWorkingStatus)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }
        "has OK status" in {
          result.status shouldBe OK
        }
      }

      "redirect to submission overview when OPW feature switch is set to false" in {
        val request = FakeRequest("GET", employerOffPayrollWorkingWarningUrl(taxYear, employmentId, offPayrollWorkingStatus)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear))
        lazy val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(appWithFeatureSwitchesOff, request, "{}").get
        }
        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }

  }

  ".submit" should {
    "redirect to Overview Page when in year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerOffPayrollWorkingWarningUrl(taxYear, employmentId, offPayrollWorkingStatus)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }

    "persist offPayrollWorkingStatus and return next page" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerOffPayrollWorkingWarningUrl(taxYearEOY, employmentId, offPayrollWorkingStatus)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check employment details page" in {
        result.status shouldBe SEE_OTHER
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.offPayrollWorkingStatus shouldBe Some(false)
      }
    }
  }

}