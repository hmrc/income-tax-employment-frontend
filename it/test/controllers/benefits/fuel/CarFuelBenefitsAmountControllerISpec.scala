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

package controllers.benefits.fuel

import forms.AmountForm
import models.benefits.{BenefitsViewModel, CarVanFuelModel}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{carFuelBenefitsAmountUrl, fullUrl, vanBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CarFuelBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = anEmploymentSource.employmentId
  private val carFuelAmount: BigDecimal = 200

  val benefitsWithFalseCarFuelQuestion: BenefitsViewModel = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(true), carFuelQuestion = Some(false))),
    isUsingCustomerData = true)

  val benefitsWithNoCarFuelQuestion: BenefitsViewModel = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(true))),
    isUsingCustomerData = true)

  val benefitsWithNoCarFuel: BenefitsViewModel = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(true), carFuelQuestion = Some(true))),
    isUsingCustomerData = true)

  val benefitsWithCarFuel: BenefitsViewModel = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(true), carFuelQuestion = Some(true),
      carFuel = Some(carFuelAmount))), isUsingCustomerData = true)

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" when {
    "redirect to Overview Page when in year" in {
      val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(carFuelBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "render page successfully" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlGet(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe "200"
        result.status shouldBe OK
      }
    }
  }

  ".submit" when {
    "redirect to Overview page when in year" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYear, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "100"))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "render page with an error when validation fails" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarFuel, isPriorSubmission = false))
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map(AmountForm.amount -> ""))
      }

      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "persist data and redirect to next page" when {
      val newAmount = 100
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarFuel))
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> newAmount.toString))
      }

      "redirects to the check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(vanBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the CYA model with the new value" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        val carFuelAmount: Option[BigDecimal] = cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel))
        carFuelAmount shouldBe Some(newAmount)
      }
    }
  }
}
