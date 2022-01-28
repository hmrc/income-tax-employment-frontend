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

package controllers.benefits

import builders.models.UserBuilder.aUserRequest
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import common.SessionValues
import forms.YesNoForm
import models.benefits.BenefitsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{carVanFuelBenefitsUrl, checkYourBenefitsUrl, companyBenefitsUrl, fullUrl, overviewUrl}

class ReceiveAnyBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  object Selectors {
    val valueHref = "#value"
    val expectedErrorHref = "#value"
    val headingSelector = "#main-content > div > div > form > div > fieldset > legend > header > h1"
    val captionSelector = ".govuk-caption-l"
    val paragraphSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val formSelector = "#main-content > div > div > form"
    val yesRadioButton = "#value"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val continueButton: String
    val expectedCaption: String
    val paragraphText: String
    val yesText: String
    val noText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val paragraphText = "Examples of benefits include company cars or vans, fuel allowance and medical insurance."
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val paragraphText = "Examples of benefits include company cars or vans, fuel allowance and medical insurance."
    val yesText = "Yes"
    val noText = "No"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "Did you get any benefits from this company?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you got any benefits from this company"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "Did your client get any benefits from this company?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client got any benefits from this company"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "Did you get any benefits from this company?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you got any benefits from this company"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "Did your client get any benefits from this company?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client got any benefits from this company"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {
    import Selectors._
    userScenarios.foreach { user =>
      import user.commonExpectedResults._
      val specific = user.specificExpectedResults.get
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return Did you receive any benefits question page" when {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
            urlGet(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "status OK" in {
            result.status shouldBe 200
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)
          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(paragraphText, paragraphSelector)
          buttonCheck(continueButton)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          formPostLinkCheck(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId), formSelector)
        }

        "return Did you receive any benefits question page with radio button pre-filled if isBenefits received field true" when {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = false, isBenefitsReceived = true)
            insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
            urlGet(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> "fake-id"))))
          }

          "status OK" in {
            result.status shouldBe 200
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)
          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(paragraphText, paragraphSelector)
          buttonCheck(continueButton)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          formPostLinkCheck(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId), formSelector)
        }
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
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
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
    import Selectors._
    val yesNoFormYes = Map(YesNoForm.yesNo -> YesNoForm.yes)
    val yesNoFormNo = Map(YesNoForm.yesNo -> YesNoForm.no)
    val yesNoFormEmpty = Map[String, String]()

    userScenarios.foreach { user =>
      import user.commonExpectedResults._
      val specific = user.specificExpectedResults.get
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return the Did you receive any employments Page with errors when no radio button is selected" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
            urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), body = yesNoFormEmpty, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "status BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)
          titleCheck(specific.expectedErrorTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          buttonCheck(continueButton)
          errorSummaryCheck(specific.expectedErrorText, expectedErrorHref)
          formPostLinkCheck(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId), formSelector)
        }
      }
    }

    "redirect to the car van fuel benefits page when value updated from no to yes, and prior benefits exist " when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = true, hasPriorBenefits = true, employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to Car van fuel Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(carVanFuelBenefitsUrl(taxYearEOY, defaultUser.employmentId)) shouldBe true
      }

      "update the isBenefitsReceived value to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, defaultUser.employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(true)
      }
    }

    "redirect to the car van fuel benefits page when value updated from no to yes" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = true, hasPriorBenefits = true, employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to Car van fuel Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(carVanFuelBenefitsUrl(taxYearEOY, defaultUser.employmentId)) shouldBe true
      }

      "update the isBenefitsReceived value to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, defaultUser.employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(true)
      }
    }

    "redirect to the Car van fuel Benefits page when radio button yes is selected and no prior benefits" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to Car van fuel Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(carVanFuelBenefitsUrl(taxYearEOY, defaultUser.employmentId)) shouldBe true
      }

      "update the isBenefitsReceived value to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, defaultUser.employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(true)
      }
    }

    "redirect to the Check your benefits page when radio button no is selected, and no prior benefits exist" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, body = yesNoFormNo, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to check your Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(checkYourBenefitsUrl(taxYearEOY, defaultUser.employmentId)) shouldBe true
      }

      "update the isBenefitsReceived value to false" in {
        lazy val cyaModel = findCyaData(taxYearEOY, defaultUser.employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(false)
      }
    }

    "redirect to the Check your benefits page when radio button no is selected, and prior benefits exist" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = true, hasPriorBenefits = true, employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
        urlPost(fullUrl(companyBenefitsUrl(taxYearEOY, defaultUser.employmentId)), follow = false, body = yesNoFormNo, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to check your Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(checkYourBenefitsUrl(taxYearEOY, defaultUser.employmentId)) shouldBe true
      }

      "update the isBenefitsReceived value to false" in {
        lazy val cyaModel = findCyaData(taxYearEOY, defaultUser.employmentId, aUserRequest).get
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