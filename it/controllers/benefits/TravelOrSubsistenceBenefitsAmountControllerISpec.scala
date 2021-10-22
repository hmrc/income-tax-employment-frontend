/*
 * Copyright 2021 HM Revenue & Customs
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

import controllers.employment.routes.CheckYourBenefitsController
import models.User
import models.employment.{BenefitsViewModel, TravelEntertainmentModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class TravelOrSubsistenceBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId = "001"

  private def url(taxYear: Int): String = s"$appUrl/$taxYear/benefits/travel-subsistence-amount?employmentId=$employmentId"

  private val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/travel-subsistence-amount?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, employmentCyaModel)

  private def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

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

  private val poundPrefixText = "£"
  private val amountInputName = "amount"

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
    val previousExpectedContent: String = "If it was not £100, tell us the correct amount."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "For example, £600 or £193.54"
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
    val previousExpectedContent: String = "If it was not £100, tell us the correct amount."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for travel and subsistence?"
    val expectedHeading: String = "How much did you get in total for travel and subsistence?"
    val expectedContent: String = "You can find this information in section N of your P11D form."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter the amount you got for travel and subsistence in the correct format"
    val emptyErrorText: String = "Enter the amount you got for travel and subsistence"
    val maxAmountErrorText: String = "Your travel and subsistence benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for travel and subsistence?"
    val expectedHeading: String = "How much did you get in total for travel and subsistence?"
    val expectedContent: String = "You can find this information in section N of your P11D form."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter the amount you got for travel and subsistence in the correct format"
    val emptyErrorText: String = "Enter the amount you got for travel and subsistence"
    val maxAmountErrorText: String = "Your travel and subsistence benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for travel and subsistence?"
    val expectedHeading: String = "How much did your client get in total for travel and subsistence?"
    val expectedContent: String = "You can find this information in section N of your client’s P11D form."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter the amount your client got for travel and subsistence in the correct format"
    val emptyErrorText: String = "Enter the amount your client got for travel and subsistence"
    val maxAmountErrorText: String = "Your client’s travel and subsistence benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for travel and subsistence?"
    val expectedHeading: String = "How much did your client get in total for travel and subsistence?"
    val expectedContent: String = "You can find this information in section N of your client’s P11D form."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter the amount your client got for travel and subsistence in the correct format"
    val emptyErrorText: String = "Enter the amount your client got for travel and subsistence"
    val maxAmountErrorText: String = "Your client’s travel and subsistence benefit must be less than £100,000,000,000"
  }

  private def benefits(travelEntertainmentModel: TravelEntertainmentModel) =
    BenefitsViewModel(travelEntertainmentModel = Some(travelEntertainmentModel), isUsingCustomerData = true, isBenefitsReceived = true)

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the travel or subsistence benefits amount page without pre-filled form" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            val travelEntertainmentModel = fullTravelOrEntertainmentModel.copy(travelAndSubsistence = None)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(travelEntertainmentModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("", inputSelector)

          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the travel or subsistence benefits amount page with pre-filled form" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          textOnPageCheck(previousExpectedContent + " " + get.expectedContent, contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("100", inputSelector)
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect user to the check your benefits page when there is no cya data" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        authoriseAgentOrIndividual(false)
        urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to overview page if the user tries to hit this page with current taxYear" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        val travelEntertainmentModel = fullTravelOrEntertainmentModel.copy(travelAndSubsistence = None)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(travelEntertainmentModel)))), userRequest)
        urlGet(url(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "should render the travel or subsistence benefits amount page with empty error text when there no input" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
            urlPost(url(taxYearEOY), follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent + " " + get.expectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("", inputSelector)

          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(get.emptyErrorText)
        }

        "should render the travel or subsistence benefits amount page with wrong format text when input is in incorrect format" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
            urlPost(url(taxYearEOY), follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "abc"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent + " " + get.expectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("abc", inputSelector)

          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.wrongFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(get.wrongFormatErrorText)
        }

        "should render the travel or subsistence benefits amount page with max error when input > 100,000,000,000" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
            urlPost(url(taxYearEOY), follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "9999999999999999999999999999"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent + " " + get.expectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("9999999999999999999999999999", inputSelector)

          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(get.maxAmountErrorText)
        }
      }
    }

    "redirect to check employments benefits page when a valid form is submitted and a prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
        urlPost(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "200"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001")
      }

      "updates the CYA model with the new value" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        val travelAndSubsistenceAmount = cyamodel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelAndSubsistence))
        travelAndSubsistenceAmount shouldBe Some(200.00)
      }
    }

    "redirect to check employments benefits page when a valid form is submitted and without prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
        urlPost(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "200"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001")
      }

      "updates the CYA model with the new value" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        val travelAndSubsistenceAmount = cyamodel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelAndSubsistence))
        travelAndSubsistenceAmount shouldBe Some(200.00)
      }
    }

    "redirect to the check your benefits page when there is no cya data" when {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        authoriseAgentOrIndividual(false)
        urlPost(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(false)
        dropEmploymentDB()
        urlPost(url(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }
    }
  }
}
