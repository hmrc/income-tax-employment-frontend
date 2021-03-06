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

import models.employment.AllEmploymentData
import models.mongo.EmploymentUserData
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentFinancialDataBuilder.aHmrcEmploymentFinancialData
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import support.builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserDataWithDetails
import utils.PageUrls.{checkYourDetailsUrl, employerPayeReferenceUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class PayeRefControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {
  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val payeRef: String = "123/AA12345"
  private val employmentId = "employmentId"

  private def cya(paye: Option[String] = Some(payeRef), isPriorSubmission: Boolean = true): EmploymentUserData = anEmploymentUserDataWithDetails(
    employmentDetails = anEmploymentDetails.copy("maggie", employerRef = paye),
    isPriorSubmission = isPriorSubmission,
    hasPriorBenefits = isPriorSubmission
  )

  val multipleEmployments: AllEmploymentData = anAllEmploymentData.copy(Seq(
    aHmrcEmploymentSource.copy(employmentId = "002", hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None))),
    aHmrcEmploymentSource.copy(employerRef = Some(payeRef), hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None)))
  ))

  ".show" should {
    "render What's the PAYE reference of xxx? page when there is cya data" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(cya())
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "render What's the PAYE reference of xxx? page when user is adding a new employment" which {

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya(None))
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect  to check employment details page when there is no cya data in session" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect  to overview page if the user tries to hit this page with current taxYear" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlGet(fullUrl(employerPayeReferenceUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" when {
    "return a bad request when a form is submitted with no input" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlPost(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map[String, String]())
      }

      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "return a bad request when the input is in incorrect format" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlPost(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("payeRef" -> ("123/abc " + employmentId + "<Q>")))
      }

      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to Overview page when a valid form is submitted" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlPost(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("payeRef" -> payeRef))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
