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

class NonTaxableCostsBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = 2021
  val employmentId: String = "001"

  def nonTaxableCostsBenefitsAmountPageUrl(taxYear: Int): String = s"$appUrl/$taxYear/benefits/non-taxable-costs-amount?employmentId=$employmentId"

  val formPostLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/non-taxable-costs-amount?employmentId=$employmentId"

  val amountInModel: BigDecimal = 100
  val amountFieldName = "amount"
  val amountFieldHref = "#amount"

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > label > header > p"
    val ifItWasNotTextSelector = "#previous-amount-text"
    val enterTotalSelector = "#enter-total-text"
    val hintTextSelector = "#amount-hint"
    val prefixedCurrencySelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val amountFieldSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    def ifItWasNotText(amount: BigDecimal): String
    val enterTotalText: String
    val expectedHintText: String
    val currencyPrefix: String
    val continueButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedIncorrectFormatErrorMessage: String
    val expectedOverMaximumErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    def ifItWasNotText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val enterTotalText = "Enter the total."
    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    def ifItWasNotText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val enterTotalText = "Enter the total."
    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much of your non-taxable costs were reimbursed by your employer?"
    val expectedHeading = "How much of your non-taxable costs were reimbursed by your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of non-taxable costs reimbursed by your employer"
    val expectedIncorrectFormatErrorMessage = "Enter the amount of non-taxable costs reimbursed by your employer in the correct format"
    val expectedOverMaximumErrorMessage = "The non-taxable costs reimbursed by your employer must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much of your non-taxable costs were reimbursed by your employer?"
    val expectedHeading = "How much of your non-taxable costs were reimbursed by your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of non-taxable costs reimbursed by your employer"
    val expectedIncorrectFormatErrorMessage = "Enter the amount of non-taxable costs reimbursed by your employer in the correct format"
    val expectedOverMaximumErrorMessage = "The non-taxable costs reimbursed by your employer must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much of your client’s non-taxable costs were reimbursed by their employer?"
    val expectedHeading = "How much of your client’s non-taxable costs were reimbursed by their employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of non-taxable costs reimbursed by your client’s employer"
    val expectedIncorrectFormatErrorMessage = "Enter the amount of non-taxable costs reimbursed by your client’s employer in the correct format"
    val expectedOverMaximumErrorMessage = "The non-taxable costs reimbursed by your client’s employer must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much of your client’s non-taxable costs were reimbursed by their employer?"
    val expectedHeading = "How much of your client’s non-taxable costs were reimbursed by their employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of non-taxable costs reimbursed by your client’s employer"
    val expectedIncorrectFormatErrorMessage = "Enter the amount of non-taxable costs reimbursed by your client’s employer in the correct format"
    val expectedOverMaximumErrorMessage = "The non-taxable costs reimbursed by your client’s employer must be less than £100,000,000,000"
  }

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = hasPriorBenefits, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(reimbursedModel: Option[ReimbursedCostsVouchersAndNonCashModel]): BenefitsViewModel =
    BenefitsViewModel(carVanFuelModel = Some(fullCarVanFuelModel), accommodationRelocationModel = Some(fullAccommodationRelocationModel),
      travelEntertainmentModel = Some(fullTravelOrEntertainmentModel), utilitiesAndServicesModel = Some(fullUtilitiesAndServicesModel),
      medicalChildcareEducationModel = Some(fullMedicalChildcareEducationModel),
      incomeTaxAndCostsModel = Some(fullIncomeTaxOrIncurredCostsModel),
      reimbursedCostsVouchersAndNonCashModel = reimbursedModel,
      isBenefitsReceived = true, isUsingCustomerData = true)

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

        "render the non-taxable costs benefits amount page with an empty amount field" when {
          "the prior amount and cya amount are the same" which {

            lazy val amount: BigDecimal = 22

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
                Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel.copy(expenses = Some(22.00))))))), userRequest)
              urlGet(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amount), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck("", amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            welshToggleCheck(user.isWelsh)

          }

          "expenses is None" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
                Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel.copy(expenses = None)))))), userRequest)
              urlGet(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            elementNotOnPageCheck(ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck("", amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            welshToggleCheck(user.isWelsh)

          }
        }

        "render the non-taxable costs benefits amount page with a pre-filled amount field" when {
          "the cya amount and the prior data amount are different" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
                Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              urlGet(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
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
              insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
                Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              urlGet(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
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
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
        urlGet(nonTaxableCostsBenefitsAmountPageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        urlGet(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
                Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              urlPost(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), body = "", welsh = user.isWelsh,
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
            textOnPageCheck(enterTotalText, enterTotalSelector)
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
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
                Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              urlPost(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(incorrectFormatAmount, amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            errorAboveElementCheck(user.specificExpectedResults.get.expectedIncorrectFormatErrorMessage, Some(amountFieldName))
            errorSummaryCheck(user.specificExpectedResults.get.expectedIncorrectFormatErrorMessage, amountFieldHref)

            welshToggleCheck(user.isWelsh)
          }

          "a form is submitted and the amount is over the maximum limit" which {
            val overMaximumAmount = "100,000,000,000,000,000,000"
            val form: Map[String, String] = Map(AmountForm.amount -> overMaximumAmount)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
                Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              urlPost(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(overMaximumAmount, amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            errorAboveElementCheck(user.specificExpectedResults.get.expectedOverMaximumErrorMessage, Some(amountFieldName))
            errorSummaryCheck(user.specificExpectedResults.get.expectedOverMaximumErrorMessage, amountFieldHref)

            welshToggleCheck(user.isWelsh)

          }
        }
      }
    }

    val newAmount: BigDecimal = 500.55

    "update cya when a user submits a valid form and has prior benefits" which {

      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
        urlPost(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), follow = false, body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      s"update expenses in reimbursedCostsVouchersAndNonCash model" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.reimbursedCostsVouchersAndNonCashQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expensesQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expenses)) shouldBe Some(newAmount)
      }

    }

    "update cya when a user submits a valid form and doesn't have prior benefits" which {

      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
          Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
        urlPost(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), follow = false, body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to taxable expenses yes/no question page" in {
        result.status shouldBe SEE_OTHER
        //TODO - redirect to taxable expenses yes no question page when available
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      s"update expenses in reimbursedCostsVouchersAndNonCash model" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.reimbursedCostsVouchersAndNonCashQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expensesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expenses)) shouldBe Some(newAmount)
      }


    }


    "redirect to the overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
        urlPost(nonTaxableCostsBenefitsAmountPageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        urlPost(nonTaxableCostsBenefitsAmountPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

  }

}