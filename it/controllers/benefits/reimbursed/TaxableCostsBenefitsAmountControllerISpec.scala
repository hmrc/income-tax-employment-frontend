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
import controllers.benefits.reimbursed.routes.TaxableCostsBenefitsController
import models.User
import models.benefits.{BenefitsViewModel, ReimbursedCostsVouchersAndNonCashModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class TaxableCostsBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId = "001"
  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)
  private val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/taxable-costs-amount?employmentId=$employmentId"

  private def pageUrl(taxYear: Int) = s"$appUrl/$taxYear/benefits/taxable-costs-amount?employmentId=$employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel) =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  private def cyaModel(benefits: Option[BenefitsViewModel] = None) =
    EmploymentCYAModel(EmploymentDetails("some-name", currentDataIsHmrcHeld = true), benefits)

  object Selectors {
    val enterTotalTextSelector = "#enter-total-text"
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
    val enterTotalText: String
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
    override val amountHint: String = "For example, £600 or £193.54"
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue: String = "Continue"
    val previousExpectedContent: String = "If it was not £200, tell us the correct amount."
    val enterTotalText: String = "Enter the total."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "For example, £600 or £193.54"
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
    val previousExpectedContent: String = "If it was not £200, tell us the correct amount."
    val enterTotalText: String = "Enter the total."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much of your taxable costs were reimbursed by your employer?"
    val expectedHeading: String = "How much of your taxable costs were reimbursed by your employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount of taxable costs reimbursed by your employer"
    val invalidFormatErrorText: String = "Enter the amount of taxable costs reimbursed by your employer in the correct format"
    val maxAmountErrorText: String = "The taxable costs reimbursed by your employer must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much of your taxable costs were reimbursed by your employer?"
    val expectedHeading: String = "How much of your taxable costs were reimbursed by your employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount of taxable costs reimbursed by your employer"
    val invalidFormatErrorText: String = "Enter the amount of taxable costs reimbursed by your employer in the correct format"
    val maxAmountErrorText: String = "The taxable costs reimbursed by your employer must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much of your client’s taxable costs were reimbursed by their employer?"
    val expectedHeading: String = "How much of your client’s taxable costs were reimbursed by their employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount of taxable costs reimbursed by your client’s employer"
    val invalidFormatErrorText: String = "Enter the amount of taxable costs reimbursed by your client’s employer in the correct format"
    val maxAmountErrorText: String = "The taxable costs reimbursed by your client’s employer must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much of your client’s taxable costs were reimbursed by their employer?"
    val expectedHeading: String = "How much of your client’s taxable costs were reimbursed by their employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount of taxable costs reimbursed by your client’s employer"
    val invalidFormatErrorText: String = "Enter the amount of taxable costs reimbursed by your client’s employer in the correct format"
    val maxAmountErrorText: String = "The taxable costs reimbursed by your client’s employer must be less than £100,000,000,000"
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
        "render the 'Amount for taxable costs reimbursed by employer' page without pre-filled form" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val model = fullReimbursedCostsVouchersAndNonCashModel.copy(taxableExpenses = None)
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
          textOnPageCheck(enterTotalText, enterTotalTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("", inputSelector)
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Amount for taxable costs reimbursed by employer' page with pre-filled form and no prior submitted data" which {
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
          textOnPageCheck(enterTotalText, enterTotalTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("200", inputSelector)
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Amount for taxable costs reimbursed by employer' page with pre-filled form and prior submitted data" which {
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
          textOnPageCheck(enterTotalText, enterTotalTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("200", inputSelector)
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "Redirect user to the check your benefits page with no cya data in session" in {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "Redirect user to the tax overview page when in year" in {
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

      "Redirect to 'Taxable cost' question page when there is taxableExpenses amount but has no taxableExpensesQuestion" in {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val model = fullReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = None, taxableExpenses = Some(11.0))
          insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(model)))), userRequest)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(TaxableCostsBenefitsController.show(taxYearEOY, employmentId).url)
      }
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
          textOnPageCheck(enterTotalText, enterTotalTextSelector)
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
          textOnPageCheck(enterTotalText, enterTotalTextSelector)
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
          textOnPageCheck(enterTotalText, enterTotalTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("100,000,000,000", inputSelector)

          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(user.specificExpectedResults.get.maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(user.specificExpectedResults.get.maxAmountErrorText)
        }
      }
    }

    "redirect to another page when a valid request is made and then" should {
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
          val taxableExpenses = cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.taxableExpenses))
          taxableExpenses shouldBe Some(123.45)
        }
      }

      "redirect to to Vouchers yes/no page when a valid form is submitted and not prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          urlPost(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "234.56"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          // TODO: Should go to Vouchers question page
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

        "updates the CYA model with the new value" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          val taxableExpenses = cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.taxableExpenses))
          taxableExpenses shouldBe Some(234.56)
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

      "redirect to taxable expenses question page when there is taxableExpenses amount but has no taxableExpensesQuestion" in {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val model = fullReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = None)
          insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(model)))), userRequest)
          urlPost(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(TaxableCostsBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }
}
