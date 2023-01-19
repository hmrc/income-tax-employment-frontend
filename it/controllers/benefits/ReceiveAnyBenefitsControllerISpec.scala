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

package controllers.benefits

import forms.YesNoForm
import models.benefits.BenefitsViewModel
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import utils.PageUrls.{carVanFuelBenefitsUrl, checkYourBenefitsUrl, companyBenefitsUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ReceiveAnyBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "return Did you receive any benefits question page" when {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        urlGet(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "status OK" in {
        result.status shouldBe 200
      }
    }

    "redirect to Check your benefits page when there is no cya" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "status SEE_OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirect to Check Employment Details page" in {
        result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to Overview page when trying to hit the page in year" when {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = false, isBenefitsReceived = true)
        val employmentCYAModel = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = employmentCYAModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(companyBenefitsUrl(taxYear, defaultUser.employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "status SEE_OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirect to Overview page" in {
        result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" should {
    val yesNoFormYes = Map(YesNoForm.yesNo -> YesNoForm.yes)
    val yesNoFormNo = Map(YesNoForm.yesNo -> YesNoForm.no)
    val yesNoFormEmpty = Map[String, String]()

    "return the Did you receive any employments Page with errors when no radio button is selected" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), body = yesNoFormEmpty, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "status BAD_REQUEST" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to the car van fuel benefits page when value updated from no to yes, and prior benefits exist " when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = true, hasPriorBenefits = true, employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to Car van fuel Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(carVanFuelBenefitsUrl(taxYearEOY, defaultUser.employmentId)) shouldBe true
      }

      "update the isBenefitsReceived value to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, defaultUser.employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(true)
      }
    }

    "redirect to the car van fuel benefits page when value updated from no to yes" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = true, hasPriorBenefits = true, employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to Car van fuel Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(carVanFuelBenefitsUrl(taxYearEOY, defaultUser.employmentId)) shouldBe true
      }

      "update the isBenefitsReceived value to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, defaultUser.employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(true)
      }
    }

    "redirect to the Car van fuel Benefits page when radio button yes is selected and no prior benefits" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to Car van fuel Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(carVanFuelBenefitsUrl(taxYearEOY, defaultUser.employmentId)) shouldBe true
      }

      "update the isBenefitsReceived value to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, defaultUser.employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(true)
      }
    }

    "redirect to the Check your benefits page when radio button no is selected, and no prior benefits exist" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, body = yesNoFormNo, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to check your Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(checkYourBenefitsUrl(taxYearEOY, defaultUser.employmentId)) shouldBe true
      }

      "update the isBenefitsReceived value to false" in {
        lazy val cyaModel = findCyaData(taxYearEOY, defaultUser.employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(false)
      }
    }

    "redirect to the Check your benefits page when radio button no is selected, and prior benefits exist" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = true, hasPriorBenefits = true, employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, body = yesNoFormNo, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to check your Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(checkYourBenefitsUrl(taxYearEOY, defaultUser.employmentId)) shouldBe true
      }

      "update the isBenefitsReceived value to false" in {
        lazy val cyaModel = findCyaData(taxYearEOY, defaultUser.employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(false)
      }
    }

    "redirect to the Check your benefits page when there is no cya" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(companyBenefitsUrl(taxYear, defaultUser.employmentId)), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "status SEE_OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirect to Check Employment Benefits page" in {
        result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }
}