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

package controllers.details

import forms.YesNoForm
import models.details.EmploymentDetails
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import utils.PageUrls.{didYouLeaveUrl, employmentDatesUrl, employmentStartDateUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class DidYouLeaveEmployerControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {
  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val employerName: String = "HMRC"
  private val employmentStartDate: String = s"${taxYearEOY - 1}-01-01"
  private val employmentId: String = "001"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, employerRef: Option[String] = Some("123/AB456"), startDate: Option[String] = Some(employmentStartDate), cessationDate: Option[String] = None,
               didYouLeaveQuestion: Option[Boolean] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc, employerRef = employerRef, payrollId = Some("payRollId"),
      startDate = startDate, cessationDate = cessationDate, didYouLeaveQuestion = didYouLeaveQuestion), None)

  ".show" should {
    "render the 'did you leave employer' page" which {
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

    "render the 'did you leave employer' page when its already in session" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, cessationDate = None,
          didYouLeaveQuestion = Some(true), hmrc = true)))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(didYouLeaveUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(didYouLeaveUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true

      }
    }
  }

  ".submit" should {
    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(didYouLeaveUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    s"return a BAD_REQUEST($BAD_REQUEST) status when a form is submitted with an empty value" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(didYouLeaveUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "Update the didYouLeaveQuestion to no and wipe the cessationDate data when the user chooses no" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel(employerName, startDate = None,
          didYouLeaveQuestion = None, hmrc = true)))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(didYouLeaveUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "redirects to the start date page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employmentStartDateUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.cessationDate shouldBe None
        cyaModel.employment.employmentDetails.didYouLeaveQuestion shouldBe Some(false)
      }
    }
    "Update the didYouLeaveQuestion to yes and when the user chooses yes and not wipe out the cessation date" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel(employerName, startDate = None,
          didYouLeaveQuestion = None, hmrc = true)))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(didYouLeaveUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "redirects to the employments dates page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employmentDatesUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.cessationDate shouldBe None
        cyaModel.employment.employmentDetails.didYouLeaveQuestion shouldBe Some(true)
      }
    }
  }
}
