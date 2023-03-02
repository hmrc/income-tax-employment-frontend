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

import forms.AmountForm
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{fullUrl, livingAccommodationBenefitsAmountUrl, qualifyingRelocationBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class LivingAccommodationBenefitAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render page and show the previous amount" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      getInputFieldValue() shouldBe "100"
      result.status shouldBe OK
    }
  }

  ".submit" should {
    "redirect to qualifying relocation page and update the living accommodation amount when a valid form is submitted and prior data exists" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel
          .copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None)))
          .copy(travelEntertainmentModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> 100.toString))
      }

      "redirects to the check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(qualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the CYA model with the new value" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        val livingAccommodationAmount: Option[BigDecimal] = cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation))
        livingAccommodationAmount shouldBe Some(100)
      }
    }

    "render page with an error when validation fails" in {
      val errorForm = Map(AmountForm.amount -> "")
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = errorForm)
      }

      result.status shouldBe BAD_REQUEST
    }
  }
}
