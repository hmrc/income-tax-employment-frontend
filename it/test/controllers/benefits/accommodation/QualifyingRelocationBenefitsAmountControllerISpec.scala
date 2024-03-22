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

package controllers.benefits.accommodation

import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{fullUrl, nonQualifyingRelocationBenefitsUrl, qualifyingRelocationBenefitsAmountUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class QualifyingRelocationBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = aHmrcEmploymentSource.employmentId

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "redirect to Overview Page when in year" in {
      val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(qualifyingRelocationBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "render page successfully" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(qualifyingRelocationExpenses = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(qualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }
  }

  ".submit" should {
    "redirect to Overview page when in year" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(qualifyingRelocationBenefitsAmountUrl(taxYear, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "100"))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "render page with an error when validation fails" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(qualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map[String, String]())
      }

      result.status shouldBe BAD_REQUEST
    }

    "persist data and redirect to next page" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(qualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the CYA model with the new value" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        val qualifyingRelocationAmount = cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses))
        qualifyingRelocationAmount shouldBe Some(100.0)
      }
    }
  }
}
