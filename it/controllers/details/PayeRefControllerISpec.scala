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

import controllers.details.routes.EmployerPayrollIdController
import forms.details.PayeRefForm
import models.details.EmploymentDetails
import models.employment.AllEmploymentData
import models.mongo.EmploymentUserData
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentFinancialDataBuilder.aHmrcEmploymentFinancialData
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithDetails}
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
        getInputFieldValue("#payeRef") shouldBe "123/AA12345"
        result.status shouldBe OK
      }
    }

    "render What's the PAYE reference of xxx? page when there is no cya data" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentDetails: EmploymentDetails = cya().employment.employmentDetails.copy(employerRef = None)
        insertCyaData(cya().copy(employment = cya().employment.copy(employmentDetails = employmentDetails)))
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue("#payeRef") shouldBe ""
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
    "return a bad request when the input is in incorrect format" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlPost(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map(PayeRefForm.payeRef -> ("123/abc " + employmentId + "<Q>")))
      }

      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to Overview page when isFinished" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        val form = Map(PayeRefForm.payeRef -> payeRef)
        urlPost(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = form)
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).head shouldBe checkYourDetailsUrl(taxYearEOY, employmentId)
      }
    }

    "redirect to EmployerPayrollId page when is not finished" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val employmentDetails = anEmploymentDetails.copy(totalTaxToDate = None)
        val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentDetails = employmentDetails))
        insertCyaData(employmentUserData)
        val form = Map(PayeRefForm.payeRef -> payeRef)
        urlPost(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = form)
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).head shouldBe EmployerPayrollIdController.show(taxYearEOY, employmentId).url
      }
    }
  }
}
