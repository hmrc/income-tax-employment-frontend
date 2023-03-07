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

import models.AuthorisationRequest
import models.details.EmploymentDetails
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import utils.PageUrls.{checkYourDetailsUrl, fullUrl, overviewUrl, payrollIdUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerPayrollIdControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val employmentId = "001"
  private val userRequest: AuthorisationRequest[_] = anAuthorisationRequest

  private def cya(isPriorSubmission: Boolean = true): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission, hasPriorBenefits = isPriorSubmission, hasPriorStudentLoans = isPriorSubmission,
      EmploymentCYAModel(
        EmploymentDetails("maggie", currentDataIsHmrcHeld = false),
        None
      )
    )

  ".show" when {
    "should render the What's your payrollId? page with no pre-filled form" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(cya(false))
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue("#payrollId") shouldBe ""
        result.status shouldBe OK
      }
    }

    "should render the What's your payrollId? page with pre-filled form" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentDetails: EmploymentDetails = cya().employment.employmentDetails.copy(payrollId = Some("123456789"))
        insertCyaData(cya().copy(employment = cya().employment.copy(employmentDetails = employmentDetails)))

        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue("#payrollId") shouldBe "123456789"
        result.status shouldBe OK
      }
    }

    "redirect to check employment details page when there is no cya data in session" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to overview page if the user tries to hit this page with current taxYear" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlGet(fullUrl(payrollIdUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" when {
    "return a bad request when form validation fails" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(cya())
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlPost(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), Map("payrollId" -> "a" * 39),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "should update the payrollId when a valid payrollId is submitted and redirect to the check your details controller" when {
      val payrollId = "123456"
      val body = Map("payrollId" -> payrollId)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = true)
        dropEmploymentDB()
        val data = EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = true, hasPriorStudentLoans = true, anEmploymentCYAModel)
        insertCyaData(data)
        urlPost(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), body,  headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "status SEE_OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirect to the Check Employment Details page" in {
        result.header(HeaderNames.LOCATION).contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      s"update the cya models payroll id to be $payrollId" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentDetails.payrollId shouldBe Some(payrollId)
      }
    }
  }
}
