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

import common.SessionValues
import forms.employment.EmployerNameForm
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import utils.PageUrls.{checkYourDetailsUrl, employerNameUrl, employerPayeReferenceUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerNameControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val employerName: String = "HMRC"
  private val updatedEmployerName: String = "Microsoft"
  private val employmentId: String = "001"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employmentCyaModel)

  private def cyaModel(employerName: String): EmploymentCYAModel = EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = false))

  ".show" should {
    "render the 'name of your employer' page when it is end of year" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employerNameUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY,
          Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employerNameUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" should {
    s"return a BAD_REQUEST($BAD_REQUEST) status when the submitted data is empty" which {
      lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> "")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerNameUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerNameUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
    "create a new cya model with the employer name (not prior submission)" which {
      lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> employerName)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerNameUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "redirects to the next question page (PAYE reference)" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerPayeReferenceUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.employerName shouldBe employerName
      }
    }
    "update a recently created cya model with the employer name (not prior submission)" which {
      lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> employerName)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = false, cyaModel(employerName)))
        urlPost(fullUrl(employerNameUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the next question page (PAYE reference)" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerPayeReferenceUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.employerName shouldBe employerName
      }
    }
    "update existing cya model with the new employer name" which {
      lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> updatedEmployerName)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentDetails = anEmploymentDetails.copy(employerName = employerName))))
        urlPost(fullUrl(employerNameUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "redirects to employment details CYA page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.employerName shouldBe updatedEmployerName
      }
    }
  }
}
