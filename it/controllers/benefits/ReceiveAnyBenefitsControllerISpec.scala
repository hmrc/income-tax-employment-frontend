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
import controllers.benefits.fuel.routes.CarVanFuelBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.benefits.BenefitsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ReceiveAnyBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val validTaxYear2021 = 2021

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
    val expectedCaption = s"Employment for 6 April ${validTaxYear2021 - 1} to 5 April $validTaxYear2021"
    val paragraphText = "Examples of benefits include company cars or vans, fuel allowance and medical insurance."
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${validTaxYear2021 - 1} to 5 April $validTaxYear2021"
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

  private def url(taxYear: Int) = s"$appUrl/$taxYear/benefits/company-benefits?employmentId=${defaultUser.employmentId}"

  private val postUrl = s"/update-and-submit-income-tax-return/employment-income/2021/benefits/company-benefits?employmentId=${defaultUser.employmentId}"

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
          val taxYear = validTaxYear2021
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
            urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
          formPostLinkCheck(postUrl, formSelector)
        }

        "return Did you receive any benefits question page with radio button pre-filled if isBenefits received field true" when {
          val taxYear = validTaxYear2021
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = false, isBenefitsReceived = true)
            insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
            urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> "fake-id"))))
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
          formPostLinkCheck(postUrl, formSelector)
        }
      }
    }

    "redirect to Check your benefits page when there is no cya" when {
      val taxYear = validTaxYear2021
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(url(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "status SEE_OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirect to Check Employment Details page" in {
        result.header(HeaderNames.LOCATION) shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$validTaxYear2021/view")
      }
    }

    "redirect to Overview page when trying to hit the page in year" when {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = false, isBenefitsReceived = true)
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(url(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "status SEE_OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirect to Overview page" in {
        result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
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
            urlPost(url(validTaxYear2021), body = yesNoFormEmpty, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
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
          formPostLinkCheck(postUrl, formSelector)
        }
      }
    }

    "redirect to the car van fuel benefits page when value updated from no to yes, and prior benefits exist " when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = true, hasPriorBenefits = true, employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        urlPost(url(validTaxYear2021), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
      }

      "redirect to Car van fuel Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(CarVanFuelBenefitsController.show(validTaxYear2021, defaultUser.employmentId).url)
      }

      "update the isBenefitsReceived value to true" in {
        lazy val cyaModel = findCyaData(validTaxYear2021, defaultUser.employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(true)
      }
    }

    "redirect to the car van fuel benefits page when value updated from no to yes" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = true, hasPriorBenefits = true, employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        urlPost(url(validTaxYear2021), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
      }

      "redirect to Car van fuel Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(CarVanFuelBenefitsController.show(validTaxYear2021, defaultUser.employmentId).url)
      }

      "update the isBenefitsReceived value to true" in {
        lazy val cyaModel = findCyaData(validTaxYear2021, defaultUser.employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(true)
      }
    }

    "redirect to the Car van fuel Benefits page when radio button yes is selected and no prior benefits" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
        urlPost(url(validTaxYear2021), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
      }

      "redirect to Car van fuel Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(CarVanFuelBenefitsController.show(validTaxYear2021, defaultUser.employmentId).url)
      }

      "update the isBenefitsReceived value to true" in {
        lazy val cyaModel = findCyaData(validTaxYear2021, defaultUser.employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(true)
      }
    }

    "redirect to the Check your benefits page when radio button no is selected, and no prior benefits exist" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
        urlPost(url(validTaxYear2021), follow = false, body = yesNoFormNo, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
      }

      "redirect to check your Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(CheckYourBenefitsController.show(validTaxYear2021, defaultUser.employmentId).url)
      }

      "update the isBenefitsReceived value to false" in {
        lazy val cyaModel = findCyaData(validTaxYear2021, defaultUser.employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(false)
      }
    }

    "redirect to the Check your benefits page when radio button no is selected, and prior benefits exist" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(defaultUser.copy(isPriorSubmission = true, hasPriorBenefits = true, employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
        urlPost(url(validTaxYear2021), follow = false, body = yesNoFormNo, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
      }

      "redirect to check your Benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(CheckYourBenefitsController.show(validTaxYear2021, defaultUser.employmentId).url)
      }

      "update the isBenefitsReceived value to false" in {
        lazy val cyaModel = findCyaData(validTaxYear2021, defaultUser.employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.map(_.isBenefitsReceived) shouldBe Some(false)
      }
    }

    "redirect to the Check your benefits page when there is no cya" when {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(url(validTaxYear2021), follow = false, body = yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
      }

      "status SEE_OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirect to Check Employment Benefits page" in {
        result.header(HeaderNames.LOCATION) shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$validTaxYear2021/view")
      }
    }
  }
}