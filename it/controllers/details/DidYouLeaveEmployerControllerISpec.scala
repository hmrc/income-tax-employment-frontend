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

package controllers.details

import forms.YesNoForm
import models.details.EmploymentDetails
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{didYouLeaveUrl, employmentEndDateUrl, fullUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class DidYouLeaveEmployerControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val employerName: String = "HMRC"
  private val employmentStartDate: String = s"${taxYearEOY - 1}-01-01"
  private val employmentId: String = anEmploymentUserData.employmentId

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, employerRef: Option[String] = Some("123/AB456"), startDate: Option[String] = Some(employmentStartDate), cessationDate: Option[String] = None,
               didYouLeaveQuestion: Option[Boolean] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc, employerRef = employerRef, payrollId = Some("payRollId"),
      startDate = startDate, cessationDate = cessationDate, didYouLeaveQuestion = didYouLeaveQuestion), None)

  ".show" should {
    "redirect to Overview Page when in year" in {
      val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employmentEndDateUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "render page successfully" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel(employerName, hmrc = true)))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(didYouLeaveUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }
  }

  ".submit" should {
    "redirect to Overview Page when in year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(didYouLeaveUrl(taxYear, employmentId)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(didYouLeaveUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe BAD_REQUEST
    }

    "persist didYouLeaveQuestion and return next page" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(didYouLeaveUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the start date page" in {
        result.status shouldBe SEE_OTHER
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.didYouLeaveQuestion shouldBe Some(false)
      }
    }
  }
}
