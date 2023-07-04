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

import forms.details.DateForm.{day, month, year}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{employmentEndDateUrl, fullUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerEndDateControllerISpec extends IntegrationTest
  with ViewHelpers
  with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val employmentId: String = "employmentId"

  ".show" should {
    "redirect to Overview Page when in year" in {
      val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employmentEndDateUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "render page successfully" in {
      val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)

        urlGet(fullUrl(employmentEndDateUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }
  }

  ".submit" should {
    "redirect to Overview Page when in year" in {
      val formData = Map(s"$day" -> "1", s"$month" -> "1", s"$year" -> taxYearEOY.toString)
      val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employmentEndDateUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = formData)
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "render page with an error when validation fails" in {
      val formData = Map(s"$day" -> "abc", s"$month" -> "1", s"$year" -> taxYearEOY.toString)
      val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employmentEndDateUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = formData)
      }

      result.status shouldBe BAD_REQUEST
    }

    "persist end date and return next page" in {
      val userData = anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentDetails = anEmploymentDetails.copy(cessationDate = None)))
      val formData = Map(s"$day" -> "1", s"$month" -> "1", s"$year" -> taxYearEOY.toString)
      val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(userData)
        urlPost(fullUrl(employmentEndDateUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = formData)
      }

      result.status shouldBe SEE_OTHER
      lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
      cyaModel.employment.employmentDetails.cessationDate shouldBe Some(s"$taxYearEOY-01-01")
    }
  }
}
