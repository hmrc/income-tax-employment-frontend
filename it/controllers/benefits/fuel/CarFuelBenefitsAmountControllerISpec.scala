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
import utils.PageUrls.{accommodationRelocationBenefitsUrl, carFuelBenefitsAmountUrl, carFuelBenefitsUrl, checkYourBenefitsUrl, fullUrl, overviewUrl, vanBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CarFuelBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = anEmploymentSource.employmentId
  private val carFuelAmount: BigDecimal = 200
  private val benefitsWithNoBenefitsReceived: BenefitsViewModel = BenefitsViewModel(isUsingCustomerData = true)

  private val benefitsWithFalseCarVanFuelQuestion: BenefitsViewModel = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(false))),
    isUsingCustomerData = true)

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
    "should render How much was your company car fuel benefit? page with no value when theres no cya data" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarFuel, isPriorSubmission = false))
        urlGet(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe ""
        result.status shouldBe OK
      }
    }

    "should render How much was your company car fuel benefit? page with no value when theres is cya data" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlGet(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe "200"
        result.status shouldBe OK
      }
    }

    "redirect to check employment benefits page when there is no cya data in session" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the car fuel question page when benefits has carFuelQuestion set to true but car fuel question empty" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarFuelQuestion, isPriorSubmission = false))
        urlGet(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carFuelBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the company van question page when benefits has carFuelQuestion set to false and no prior benefits" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithFalseCarFuelQuestion, hasPriorBenefits = false))
        urlGet(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(vanBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when benefits has carFuelQuestion set to false and prior benefits exist" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithFalseCarFuelQuestion))
        urlGet(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to accommodation relocation page when benefits has carVanFuelQuestion set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithFalseCarVanFuelQuestion, hasPriorBenefits = false))
        urlGet(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to check employment benefits page when benefits has benefitsReceived set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoBenefitsReceived, isPriorSubmission = false))
        urlGet(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to overview page if the user tries to hit this page with current taxYear" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarFuel, isPriorSubmission = false))
        val inYearUrl = s"$appUrl/$taxYear/details/how-much-pay?employmentId=$employmentId"
        urlGet(inYearUrl, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" when {
    "should render the How much was your company car fuel benefit? page with an error when theres no input" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarFuel, isPriorSubmission = false))
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map(AmountForm.amount -> ""))
      }

      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to company van page and update the car fuel amount when a valid form is submitted and prior benefits exist" when {
      val newAmount = 100
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarFuel))
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
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

    "redirect to company van question page and update the car fuel amount when a valid form is submitted" when {
      val newAmount = 100

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarFuel, isPriorSubmission = false))
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
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

    "redirect to company van question page when benefits has carFuelQuestion set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithFalseCarFuelQuestion, hasPriorBenefits = false))
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(vanBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the car fuel question page when benefits has carFuelQuestion set to true but car fuel question empty" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarFuelQuestion, hasPriorBenefits = false))
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carFuelBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to check employment benefits page when benefits has carFuelQuestion set to false and prior benefits exist" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithFalseCarFuelQuestion))
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to accommodation relocation page when benefits has carVanFuelQuestion set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithFalseCarVanFuelQuestion, hasPriorBenefits = false))
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to check employment benefits page when benefits has benefitsReceived set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoBenefitsReceived, isPriorSubmission = false))
        urlPost(fullUrl(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
