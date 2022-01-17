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

package controllers.benefits.fuel

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import models.benefits.BenefitsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{checkYourBenefitsUrl, fullUrl, mileageBenefitsUrl, overviewUrl, vanBenefitsAmountUrl, vanBenefitsUrl, vanFuelBenefitsUrl}

class CompanyVanBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val poundPrefixText = "£"
  private val amountInputName = "amount"
  private val taxYearEOY: Int = taxYear - 1
  private val employmentId = "employmentId"

  object Selectors {
    val contentSelector = "#main-content > div > div > form > div > label > p"
    val hintTextSelector = "#amount-hint"
    val inputSelector = "#amount"
    val poundPrefixSelector = ".govuk-input__prefix"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val subheading = "#main-content > div > div > form > div > label > header > p"
    val expectedErrorHref = "#amount"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val amountHint: String
    val continue: String
    val previousExpectedContent: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedContent: String
    val expectedErrorTitle: String
    val wrongFormatErrorText: String
    val emptyErrorText: String
    val maxAmountErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val amountHint: String = "For example, £600 or £193.54"
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
    val previousExpectedContent: String = "If it was not £300, tell us the correct amount. "
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "For example, £600 or £193.54"
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
    val previousExpectedContent: String = "If it was not £300, tell us the correct amount. "
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much was your total company van benefit?"
    val expectedHeading: String = "How much was your total company van benefit?"
    val expectedContent: String = "You can find this information on your P11D form in section G, box 9."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter your company van benefit amount in the correct format"
    val emptyErrorText: String = "Enter your company van benefit amount"
    val maxAmountErrorText: String = "Your company van benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much was your total company van benefit?"
    val expectedHeading: String = "How much was your total company van benefit?"
    val expectedContent: String = "You can find this information on your P11D form in section G, box 9."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter your company van benefit amount in the correct format"
    val emptyErrorText: String = "Enter your company van benefit amount"
    val maxAmountErrorText: String = "Your company van benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much was your client’s total company van benefit?"
    val expectedHeading: String = "How much was your client’s total company van benefit?"
    val expectedContent: String = "You can find this information on your client’s P11D form in section G, box 9."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter your client’s company van benefit amount in the correct format"
    val emptyErrorText: String = "Enter your client’s company van benefit amount"
    val maxAmountErrorText: String = "Your client’s company van benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much was your client’s total company van benefit?"
    val expectedHeading: String = "How much was your client’s total company van benefit?"
    val expectedContent: String = "You can find this information on your client’s P11D form in section G, box 9."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter your client’s company van benefit amount in the correct format"
    val emptyErrorText: String = "Enter your client’s company van benefit amount"
    val maxAmountErrorText: String = "Your client’s company van benefit must be less than £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the company van benefits amount page without pre-filled form" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(van = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(vanBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the company van benefits amount page with pre-filled form" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent + get.expectedContent, contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "300")
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(vanBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "Redirect user to the check your benefits page with no cya data" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "Redirect user to the tax overview page when in year" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(van = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(vanBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the van question page when benefits has carVanFuelQuestion set to true but van question empty" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
        urlGet(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(vanBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the mileage page when benefits has vanQuestion set to false when no prior benefits" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
        urlGet(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(mileageBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to check employment benefits page when benefits has carVanFuelQuestion set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = BenefitsViewModel(None, isUsingCustomerData = true)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
        urlGet(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "should render How much was your company van benefit? page with empty error text when there no input" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData, aUserRequest)
            urlPost(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
        }

        "should render How much was your company van benefit? page with wrong format text when input is in incorrect format" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData, aUserRequest)
            urlPost(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "|"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck(amountInputName, inputSelector, "|")
          errorSummaryCheck(get.wrongFormatErrorText, expectedErrorHref)
        }

        "should render How much was your company van benefit? page with max error ext when input > 100,000,000,000" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData, aUserRequest)
            urlPost(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)),
              body = Map("amount" -> "9999999999999999999999999999"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck(amountInputName, inputSelector, "9999999999999999999999999999")
          errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
        }
      }
    }

    "redirect to van fuel page when a valid form is submitted, and prior benefits exist" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
        urlPost(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has a redirect to the van fuel benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(vanFuelBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the company van amount to be 100" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.van)) shouldBe Some(100)
      }
    }

    "redirect to company van fuel page when a valid form is submitted, when no prior benefits" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
        urlPost(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has a redirect to the company van fuel page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(vanFuelBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the company van amount to be 100" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.van)) shouldBe Some(100)
      }
    }

    "redirect to check income overview page when the request is in year" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData, aUserRequest)
        urlPost(fullUrl(vanBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check your benefits page when no cya data" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlPost(fullUrl(vanBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
