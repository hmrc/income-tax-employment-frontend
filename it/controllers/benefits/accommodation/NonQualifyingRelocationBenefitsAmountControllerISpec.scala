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

package controllers.benefits.accommodation

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import models.benefits.BenefitsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{checkYourBenefitsUrl, fullUrl, nonQualifyingRelocationBenefitsAmountUrl, nonQualifyingRelocationBenefitsUrl, overviewUrl, travelOrEntertainmentBenefitsUrl}

class NonQualifyingRelocationBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

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
    val expectedErrorTitle: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val amountHint: String = "For example, £193.52"
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
    val previousExpectedContent: String = "If it was not £300, tell us the correct amount."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "For example, £193.52"
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
    val previousExpectedContent: String = "If it was not £300, tell us the correct amount."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for non-qualifying relocation benefits?"
    val expectedHeading: String = "How much did you get in total for non-qualifying relocation benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter your non-qualifying relocation benefit amount"
    val invalidFormatErrorText: String = "Enter your non-qualifying relocation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your non-qualifying relocation benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for non-qualifying relocation benefits?"
    val expectedHeading: String = "How much did you get in total for non-qualifying relocation benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter your non-qualifying relocation benefit amount"
    val invalidFormatErrorText: String = "Enter your non-qualifying relocation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your non-qualifying relocation benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for non-qualifying relocation benefits?"
    val expectedHeading: String = "How much did your client get in total for non-qualifying relocation benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter your client’s non-qualifying relocation benefit amount"
    val invalidFormatErrorText: String = "Enter your client’s non-qualifying relocation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your client’s non-qualifying relocation benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for non-qualifying relocation benefits?"
    val expectedHeading: String = "How much did your client get in total for non-qualifying relocation benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter your client’s non-qualifying relocation benefit amount"
    val invalidFormatErrorText: String = "Enter your client’s non-qualifying relocation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your client’s non-qualifying relocation benefit must be less than £100,000,000,000"
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
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the non-qualifying relocation benefits amount page without pre-filled form and without replay text" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpenses = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          elementNotOnPageCheck(contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")

          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the non-qualifying relocation benefits amount page with pre-filled cy data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(anEmploymentUserData, aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "300")
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the non-qualifying relocation benefits amount page with the amount field pre-filled with prior benefits data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData, aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "300")
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpenses = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when benefits has nonQualifyingRelocationExpensesQuestion set to false and prior benefits exist" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpensesQuestion = Some(false))))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          urlGet(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect to the non qualifying relocation question page when benefits has accommodationRelocationQuestion set to true" +
        " but non qualifying relocation question empty" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpensesQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          urlGet(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the check employment benefits page when theres no benefits and no prior benefits" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = true)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
          urlGet(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "should render non qualifying relocation question amount page with empty error text when there no input" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData, aUserRequest)
            urlPost(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(get.emptyErrorText)
        }

        "should render the non qualifying relocation question amount page with invalid format text when input is in incorrect format" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData, aUserRequest)
            urlPost(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "abc"))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "abc")
          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(get.invalidFormatErrorText)
        }

        "should render the non qualifying relocation question page with max error when input > 100,000,000,000" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData, aUserRequest)
            urlPost(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "9999999999999999999999999999"))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "9999999999999999999999999999")
          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(get.maxAmountErrorText)
        }
      }
    }

    "redirect to another page when a valid request is made and then" should {
      "redirect to travel entertainment page when a valid form is submitted and prior benefits exist" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = None)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          urlPost(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.23"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(travelOrEntertainmentBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

        "updates the CYA model with the new value" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
          val nonQualifyingAmount = cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses))
          nonQualifyingAmount shouldBe Some(100.23)
        }
      }

      "redirect to the travel or entertainment next question page when a valid form is submitted when no prior benefits" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = None)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
          urlPost(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.11"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(travelOrEntertainmentBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

        "updates the CYA model with the new value" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
          val nonQualifyingRelocationAmount = cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses))
          nonQualifyingRelocationAmount shouldBe Some(100.11)
        }
      }

      "redirect user to the check your benefits page with no cya data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertCyaData(anEmploymentUserData, aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYear, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.22"))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when benefits has nonQualifyingRelocationExpensesQuestion set to false and prior benefits exist" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpensesQuestion = Some(false))))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          urlPost(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect to the non qualifying relocation question page when benefits has accommodationRelocationQuestion set to true" +
        " but non qualifying relocation question empty" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpensesQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
          urlPost(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the check employment benefits page when theres no benefits and no prior benefits" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = true)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)

          urlPost(fullUrl(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }
  }
}