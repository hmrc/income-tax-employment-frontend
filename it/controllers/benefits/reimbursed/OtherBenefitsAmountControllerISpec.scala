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

package controllers.benefits.reimbursed

import controllers.employment.routes.CheckYourBenefitsController
import models.User
import models.benefits.{BenefitsViewModel, ReimbursedCostsVouchersAndNonCashModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class OtherBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId = "001"
  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)
  private val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/other-benefits-amount?employmentId=$employmentId"

  private def pageUrl(taxYear: Int) = s"$appUrl/$taxYear/benefits/other-benefits-amount?employmentId=$employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel) =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  private def cyaModel(benefits: Option[BenefitsViewModel] = None) =
    EmploymentCYAModel(EmploymentDetails("some-name", currentDataIsHmrcHeld = true), benefits)

  object Selectors {
    val previousAmountTextSelector = "#previous-amount-text"
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
    val maxAmountErrorText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val amountHint: String = "For example, £600 or £193.54"
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue: String = "Continue"
    val previousExpectedContent: String = "If it was not £500, tell us the correct amount."
    val maxAmountErrorText: String = "The amount for other benefits must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "For example, £600 or £193.54"
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue: String = "Continue"
    val previousExpectedContent: String = "If it was not £500, tell us the correct amount."
    val maxAmountErrorText: String = "The amount for other benefits must be less than £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for other benefits?"
    val expectedHeading: String = "How much did you get in total for other benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount you got for other benefits"
    val invalidFormatErrorText: String = "Enter the amount you got for other benefits in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for other benefits?"
    val expectedHeading: String = "How much did you get in total for other benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount you got for other benefits"
    val invalidFormatErrorText: String = "Enter the amount you got for other benefits in the correct format"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for other benefits?"
    val expectedHeading: String = "How much did your client get in total for other benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount your client got for other benefits"
    val invalidFormatErrorText: String = "Enter the amount your client got for other benefits in the correct format"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for other benefits?"
    val expectedHeading: String = "How much did your client get in total for other benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount your client got for other benefits"
    val invalidFormatErrorText: String = "Enter the amount your client got for other benefits in the correct format"
  }

  private def benefits(reimbursedCostsVouchersAndNonCashModel: ReimbursedCostsVouchersAndNonCashModel): BenefitsViewModel =
    BenefitsViewModel(carVanFuelModel = Some(fullCarVanFuelModel), accommodationRelocationModel = Some(fullAccommodationRelocationModel),
      travelEntertainmentModel = Some(fullTravelOrEntertainmentModel), utilitiesAndServicesModel = Some(fullUtilitiesAndServicesModel),
      medicalChildcareEducationModel = Some(fullMedicalChildcareEducationModel), incomeTaxAndCostsModel = Some(fullIncomeTaxAndCostsModel),
      isUsingCustomerData = true, isBenefitsReceived = true, reimbursedCostsVouchersAndNonCashModel = Some(reimbursedCostsVouchersAndNonCashModel))

  private val benefitsWithNoBenefitsReceived: Option[BenefitsViewModel] = Some(BenefitsViewModel(isUsingCustomerData = true))

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        import user.commonExpectedResults._
        "render the other benefits amount page without pre-filled form" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val model = fullReimbursedCostsVouchersAndNonCashModel.copy(otherItems = None)
            insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(model)))), userRequest)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          elementNotOnPageCheck(previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("", inputSelector)
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the other benefits amount page with pre-filled form and no prior submitted data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("500", inputSelector)
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the other benefits amount page with pre-filled form and prior submitted data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("500", inputSelector)
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

      "redirect user to the check your benefits page with no cya data in session" in {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "redirect user to the tax overview page when in year" in {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = false, cyaModel(benefitsWithNoBenefitsReceived)), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(pageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }

      "redirect to cya page when the otherItemsQuestion value is false" in {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val model = fullReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = Some(false))
          insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(model)))), userRequest)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "redirect to cya page when the reimbursedCostsVouchersAndNonCashQuestion value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(emptyReimbursedCostsVouchersAndNonCashModel)))), userRequest)
        urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
    }

      "redirect to cya page when the benefitsReceived value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel(benefitsWithNoBenefitsReceived)), userRequest)
        urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
    }

  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        import user.commonExpectedResults._

        "should render the amount page with empty value error text when there is no input" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
            urlPost(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("", inputSelector)

          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(user.specificExpectedResults.get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(user.specificExpectedResults.get.emptyErrorText)
        }

        "should render the amount page with invalid format text when input is in incorrect format" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
            urlPost(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "abc"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("abc", inputSelector)

          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(user.specificExpectedResults.get.invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(user.specificExpectedResults.get.invalidFormatErrorText)
        }

        "should render the amount page with max error when input > 99,999,999,999" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
            urlPost(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)),
              body = Map("amount" -> "100,000,000,000"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("100,000,000,000", inputSelector)

          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(maxAmountErrorText)
        }
      }
    }

      "redirect to check employment benefits page when a valid form is submitted and a prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          urlPost(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "123.45"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

        "updates the CYA model with the new value" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          val otherBenefits = cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.otherItems))
          otherBenefits shouldBe Some(123.45)
        }
      }

      "redirect to the first assets section question page when a valid form is submitted and not prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          urlPost(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "234.56"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          // TODO: Should go to the first assets section question page
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

        "updates the CYA model with the new value" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          val otherBenefits = cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.otherItems))
          otherBenefits shouldBe Some(234.56)
        }
      }

      "redirect user to the check your benefits page with no cya data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "345.67"))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.50"))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }

      "redirect to cya page when the otherItemsQuestion value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val model = fullReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = Some(false))
        insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(model)))), userRequest)
        urlPost(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "123.45"))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
    }

      "redirect to cya page when the reimbursedCostsVouchersAndNonCashQuestion value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(emptyReimbursedCostsVouchersAndNonCashModel)))), userRequest)
        urlPost(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "123.45"))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
    }

      "redirect to cya page when the benefitsReceived value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel(benefitsWithNoBenefitsReceived)), userRequest)
        urlPost(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "123.45"))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
    }

  }
}