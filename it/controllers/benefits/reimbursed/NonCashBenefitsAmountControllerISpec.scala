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
import forms.AmountForm
import models.User
import models.benefits.{BenefitsViewModel, ReimbursedCostsVouchersAndNonCashModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class NonCashBenefitsAmountControllerISpec extends  IntegrationTest with EmploymentDatabaseHelper with ViewHelpers {

  val taxYearEOY: Int = 2021
  val employmentId: String = "001"

  def nonCashBenefitsAmountPageUrl(taxYear: Int): String = s"$appUrl/$taxYear/benefits/non-cash-benefits-amount?employmentId=$employmentId"

  val formPostLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/non-cash-benefits-amount?employmentId=$employmentId"

  val amountInModel: BigDecimal = 400
  val amountFieldName = "amount"
  val amountFieldHref = "#amount"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = hasPriorBenefits, employmentCyaModel)

  def cyaModel(benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName = "employerName", currentDataIsHmrcHeld = true), benefits)

  def benefits(reimbursedModel: Option[ReimbursedCostsVouchersAndNonCashModel]): BenefitsViewModel =
    BenefitsViewModel(carVanFuelModel = Some(fullCarVanFuelModel), accommodationRelocationModel = Some(fullAccommodationRelocationModel),
      travelEntertainmentModel = Some(fullTravelOrEntertainmentModel), utilitiesAndServicesModel = Some(fullUtilitiesAndServicesModel),
      medicalChildcareEducationModel = Some(fullMedicalChildcareEducationModel),
      incomeTaxAndCostsModel = Some(fullIncomeTaxOrIncurredCostsModel),
      reimbursedCostsVouchersAndNonCashModel = reimbursedModel,
      isBenefitsReceived = true, isUsingCustomerData = true)

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > label > header > p"
    val ifItWasNotTextSelector = "#previous-amount-text"
    val hintTextSelector = "#amount-hint"
    val prefixedCurrencySelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val amountFieldSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    def ifItWasNotText(amount: BigDecimal): String
    val expectedHintText: String
    val currencyPrefix: String
    val continueButtonText: String
    val expectedIncorrectFormatErrorMessage: String
    val expectedOverMaximumErrorMessage: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    def ifItWasNotText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
    val expectedIncorrectFormatErrorMessage = "Enter the amount for non-cash benefits in the correct format"
    val expectedOverMaximumErrorMessage = "The amount for non-cash benefits must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    def ifItWasNotText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
    val expectedIncorrectFormatErrorMessage = "Enter the amount for non-cash benefits in the correct format"
    val expectedOverMaximumErrorMessage = "The amount for non-cash benefits must be less than £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much did you get in total for non-cash benefits?"
    val expectedHeading = "How much did you get in total for non-cash benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you got for non-cash benefits"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much did you get in total for non-cash benefits?"
    val expectedHeading = "How much did you get in total for non-cash benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you got for non-cash benefits"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much did your client get in total for non-cash benefits?"
    val expectedHeading = "How much did your client get in total for non-cash benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount your client got for non-cash benefits"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much did your client get in total for non-cash benefits?"
    val expectedHeading = "How much did your client get in total for non-cash benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount your client got for non-cash benefits"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the non-cash benefits amount page with an empty amount field" when {
          "the prior amount and cya amount are the same" which {

            lazy val amount: BigDecimal = 25

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(
                Some(fullReimbursedCostsVouchersAndNonCashModel.copy(nonCash = Some(25.00))))))), userRequest)
              urlGet(nonCashBenefitsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amount), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck("", amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            welshToggleCheck(user.isWelsh)

          }

          "there is no prior value (nonCash is None)" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(
                Some(fullReimbursedCostsVouchersAndNonCashModel.copy(nonCash = None)))))), userRequest)
              urlGet(nonCashBenefitsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            elementNotOnPageCheck(ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck("", amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            welshToggleCheck(user.isWelsh)

          }
        }

        "render the non-cash benefits amount page with a pre-filled amount field" when {
          "the cya amount and the prior data amount are different" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(
                Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              urlGet(nonCashBenefitsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(amountInModel.toString(), amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            welshToggleCheck(user.isWelsh)

          }

          "the user has cya data and no prior benefits" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(
                Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              urlGet(nonCashBenefitsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(amountInModel.toString(), amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            welshToggleCheck(user.isWelsh)

          }
        }
      }
    }

    "redirect to the overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(
          Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
        urlGet(nonCashBenefitsAmountPageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect to the check your benefits page when there's no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(nonCashBenefitsAmountPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to other items yes no question page when non-cash question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(
          Some(fullReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = Some(false))))))), userRequest)
        urlGet(nonCashBenefitsAmountPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to check your benefits page when reimbursed costs, vouchers and non-cash question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(
          Some(emptyReimbursedCostsVouchersAndNonCashModel))))), userRequest)
        urlGet(nonCashBenefitsAmountPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

  }

  ".submit" should {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return an error" when {
          "a form is submitted with an empty amount field" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(
                Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              urlPost(nonCashBenefitsAmountPageUrl(taxYearEOY), body = "", welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck("", amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, Some(amountFieldName))
            errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, amountFieldHref)

            welshToggleCheck(user.isWelsh)

          }

          "a form is submitted in the incorrect format" which {
            val incorrectFormatAmount = "abc"
            val form: Map[String, String] = Map(AmountForm.amount -> incorrectFormatAmount)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(
                Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              urlPost(nonCashBenefitsAmountPageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(incorrectFormatAmount, amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            errorAboveElementCheck(expectedIncorrectFormatErrorMessage, Some(amountFieldName))
            errorSummaryCheck(expectedIncorrectFormatErrorMessage, amountFieldHref)

            welshToggleCheck(user.isWelsh)
          }

          "a form is submitted and the amount is over the maximum limit" which {
            val overMaximumAmount = "100,000,000,000,000,000,000"
            val form: Map[String, String] = Map(AmountForm.amount -> overMaximumAmount)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(
                Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              urlPost(nonCashBenefitsAmountPageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(overMaximumAmount, amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            errorAboveElementCheck(expectedOverMaximumErrorMessage, Some(amountFieldName))
            errorSummaryCheck(expectedOverMaximumErrorMessage, amountFieldHref)

            welshToggleCheck(user.isWelsh)

          }
        }
      }
    }

    val newAmount: BigDecimal = 19.99

    "update cya when a user submits a valid form and has prior benefits" which {

      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(
          Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
        urlPost(nonCashBenefitsAmountPageUrl(taxYearEOY), follow = false, body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      s"update nonCash in reimbursedCostsVouchersAndNonCash model" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.reimbursedCostsVouchersAndNonCashQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.nonCashQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.nonCash)) shouldBe Some(newAmount)
      }

    }

    "update cya when a user submits a valid form and doesn't have prior benefits" which {

      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(
          Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
        urlPost(nonCashBenefitsAmountPageUrl(taxYearEOY), follow = false, body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to other items yes/no question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      s"update expenses in reimbursedCostsVouchersAndNonCash model" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.reimbursedCostsVouchersAndNonCashQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.nonCashQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.nonCash)) shouldBe Some(newAmount)
      }

    }

    "redirect to the overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(
          Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
        urlPost(nonCashBenefitsAmountPageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect to the check your benefits page when there's no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlPost(nonCashBenefitsAmountPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to other items yes no question page when non-cash question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(
          Some(fullReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = Some(false))))))), userRequest)
        urlPost(nonCashBenefitsAmountPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to check your benefits page when reimbursed costs, vouchers and non-cash question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(
          Some(emptyReimbursedCostsVouchersAndNonCashModel))))), userRequest)
        urlPost(nonCashBenefitsAmountPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

  }

}
