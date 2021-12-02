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

package controllers.benefits.medical

import controllers.employment.routes.CheckYourBenefitsController
import controllers.benefits.income.routes._
import forms.AmountForm
import models.User
import models.benefits.{BenefitsViewModel, MedicalChildcareEducationModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class BeneficialLoansAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId: String = "001"

  def beneficialLoansAmountPageUrl(taxYear: Int): String = s"$appUrl/$taxYear/benefits/beneficial-loans-amount?employmentId=$employmentId"

  val formPostLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/beneficial-loans-amount?employmentId=$employmentId"

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > label > header > p"
    val paragraphTextSelector = "#main-content > div > div > form > div > label > p:nth-child(2)"
    val paragraphTextSelector2 = "#main-content > div > div > form > div > label > p:nth-child(3)"
    val hintTextSelector = "#amount-hint"
    val prefixedCurrencySelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val inputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  val amountInModel: BigDecimal = 400
  val amountInputName = "amount"
  val amountFieldHref = "#amount"

  trait CommonExpectedResults {
    val expectedCaption: String
    def ifItWasNotText(amount: BigDecimal): String
    val expectedHintText: String
    val currencyPrefix: String
    val continueButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val youCanFindText: String
    val expectedNoEntryErrorMessage: String
    val expectedIncorrectFormatErrorMessage: String
    val expectedOverMaximumErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    def ifItWasNotText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    def ifItWasNotText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much were your beneficial loans in total?"
    val expectedHeading = "How much were your beneficial loans in total?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youCanFindText = "You can find this information on your P11D form in section H, box 15."
    val expectedNoEntryErrorMessage = "Enter your beneficial loans amount"
    val expectedIncorrectFormatErrorMessage = "Enter your beneficial loans amount in the correct format"
    val expectedOverMaximumErrorMessage = "Your beneficial loans must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much were your beneficial loans in total?"
    val expectedHeading = "How much were your beneficial loans in total?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youCanFindText = "You can find this information on your P11D form in section H, box 15."
    val expectedNoEntryErrorMessage = "Enter your beneficial loans amount"
    val expectedIncorrectFormatErrorMessage = "Enter your beneficial loans amount in the correct format"
    val expectedOverMaximumErrorMessage = "Your beneficial loans must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much were your client’s beneficial loans in total?"
    val expectedHeading = "How much were your client’s beneficial loans in total?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youCanFindText = "You can find this information on your client’s P11D form in section H, box 15."
    val expectedNoEntryErrorMessage = "Enter your client’s beneficial loans amount"
    val expectedIncorrectFormatErrorMessage = "Enter your client’s beneficial loans amount in the correct format"
    val expectedOverMaximumErrorMessage = "Your client’s beneficial loans must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much were your client’s beneficial loans in total?"
    val expectedHeading = "How much were your client’s beneficial loans in total?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youCanFindText = "You can find this information on your client’s P11D form in section H, box 15."
    val expectedNoEntryErrorMessage = "Enter your client’s beneficial loans amount"
    val expectedIncorrectFormatErrorMessage = "Enter your client’s beneficial loans amount in the correct format"
    val expectedOverMaximumErrorMessage = "Your client’s beneficial loans must be less than £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = hasPriorBenefits, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(medicalChildcareEducationModel: MedicalChildcareEducationModel): BenefitsViewModel =
    BenefitsViewModel(carVanFuelModel = Some(fullCarVanFuelModel), accommodationRelocationModel = Some(fullAccommodationRelocationModel),
      travelEntertainmentModel = Some(fullTravelOrEntertainmentModel), utilitiesAndServicesModel = Some(fullUtilitiesAndServicesModel),
      isUsingCustomerData = true, isBenefitsReceived = true, medicalChildcareEducationModel = Some(medicalChildcareEducationModel))

  ".show" should {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the beneficial loans amount page with an empty amount field" when {
          "the prior amount and cya amount are the same" which {

            lazy val amount: BigDecimal = 18

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
                Some(benefits(fullMedicalChildcareEducationModel.copy(beneficialLoan = Some(18.00)))))), userRequest)
              urlGet(beneficialLoansAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amount), paragraphTextSelector)
            textOnPageCheck(user.specificExpectedResults.get.youCanFindText, paragraphTextSelector2)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            welshToggleCheck(user.isWelsh)

          }

          "beneficial loans is None" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
                Some(benefits(fullMedicalChildcareEducationModel.copy(beneficialLoan = None))))), userRequest)
              urlGet(beneficialLoansAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.youCanFindText, paragraphTextSelector)
            elementNotOnPageCheck(paragraphTextSelector2)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            welshToggleCheck(user.isWelsh)

          }
        }

        "render the beneficial loans amount page with a pre-filled amount field" when {
          "the cya amount and the prior data amount are different" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
                Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
              urlGet(beneficialLoansAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), paragraphTextSelector)
            textOnPageCheck(user.specificExpectedResults.get.youCanFindText, paragraphTextSelector2)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountInModel.toString())
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
                Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
              urlGet(beneficialLoansAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), paragraphTextSelector)
            textOnPageCheck(user.specificExpectedResults.get.youCanFindText, paragraphTextSelector2)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountInModel.toString())
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
          Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
        urlGet(beneficialLoansAmountPageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        urlGet(beneficialLoansAmountPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
                fullMedicalChildcareEducationModel)))), userRequest)
              urlPost(beneficialLoansAmountPageUrl(taxYearEOY), body = "", welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), paragraphTextSelector)
            textOnPageCheck(user.specificExpectedResults.get.youCanFindText, paragraphTextSelector2)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, Some(amountInputName))
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
                fullMedicalChildcareEducationModel)))), userRequest)
              urlPost(beneficialLoansAmountPageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), paragraphTextSelector)
            textOnPageCheck(user.specificExpectedResults.get.youCanFindText, paragraphTextSelector2)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldValueCheck(amountInputName, inputSelector, incorrectFormatAmount)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            errorAboveElementCheck(user.specificExpectedResults.get.expectedIncorrectFormatErrorMessage, Some(amountInputName))
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
                fullMedicalChildcareEducationModel)))), userRequest)
              urlPost(beneficialLoansAmountPageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption, captionSelector)
            textOnPageCheck(ifItWasNotText(amountInModel), paragraphTextSelector)
            textOnPageCheck(user.specificExpectedResults.get.youCanFindText, paragraphTextSelector2)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldValueCheck(amountInputName, inputSelector, overMaximumAmount)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(formPostLink, formSelector)

            errorAboveElementCheck(user.specificExpectedResults.get.expectedOverMaximumErrorMessage, Some(amountInputName))
            errorSummaryCheck(user.specificExpectedResults.get.expectedOverMaximumErrorMessage, amountFieldHref)

            welshToggleCheck(user.isWelsh)

          }
        }
      }
    }

    "update cya when a user submits a valid form and has prior benefits" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
        urlPost(beneficialLoansAmountPageUrl(taxYearEOY), follow = false, body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"update medicalChildcareEducationModel and redirect to income tax section" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(IncomeTaxOrIncurredCostsBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoanQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoan)) shouldBe Some(newAmount)
      }
    }

    "update cya when a user submits a valid form and doesn't have prior benefits" which {
      val newAmount: BigDecimal = 500.55
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
          Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
        urlPost(beneficialLoansAmountPageUrl(taxYearEOY), follow = false, body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"update medicalChildcareEducationModel and redirect to income tax and costs yes no page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(IncomeTaxOrIncurredCostsBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoanQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoan)) shouldBe Some(newAmount)
      }
    }

    "redirect to the overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
        urlPost(beneficialLoansAmountPageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        urlPost(beneficialLoansAmountPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }
}
