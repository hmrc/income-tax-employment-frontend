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

package controllers.benefits.income

import controllers.employment.routes.CheckYourBenefitsController
import forms.AmountForm
import models.User
import models.benefits.{BenefitsViewModel, IncomeTaxAndCostsModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class IncurredCostsBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId: String = "001"
  val amount: BigDecimal = 200
  val amountFieldName = "amount"
  val expectedErrorHref = "#amount"

  def pageUrl(taxYear: Int): String =
    s"$appUrl/$taxYear/benefits/incurred-costs-amount?employmentId=$employmentId"

  val formLink: String =
    s"/income-through-software/return/employment-income/$taxYearEOY/benefits/incurred-costs-amount?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = hasPriorBenefits, employmentCyaModel)

  def cyaModel(benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails("employerName", currentDataIsHmrcHeld = true), benefits)

  def benefits(incomeTaxModel: IncomeTaxAndCostsModel): BenefitsViewModel =
    BenefitsViewModel(
      carVanFuelModel = Some(fullCarVanFuelModel), accommodationRelocationModel = Some(fullAccommodationRelocationModel),
      travelEntertainmentModel = Some(fullTravelOrEntertainmentModel), utilitiesAndServicesModel = Some(fullUtilitiesAndServicesModel),
      medicalChildcareEducationModel = Some(fullMedicalChildcareEducationModel),
      isUsingCustomerData = true, isBenefitsReceived = true, incomeTaxAndCostsModel = Some(incomeTaxModel))

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > label > header > p"
    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > label > p:nth-child($index)"
    val hintTextSelector = "#amount-hint"
    val currencyPrefixSelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val amountFieldSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    def optionalParagraphText(amount: BigDecimal): String
    val expectedHintText: String
    val currencyPrefix: String
    val continueButtonText: String
    val enterTotalText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val expectedErrorIncorrectFormat: String
    val expectedErrorOverMaximum: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    def optionalParagraphText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
    val enterTotalText = "Enter the total."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    def optionalParagraphText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
    val enterTotalText = "Enter the total."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much of your incurred costs did your employer pay?"
    val expectedHeading = "How much of your incurred costs did your employer pay?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of incurred costs paid by your employer"
    val expectedErrorIncorrectFormat = "Enter the amount of incurred costs paid by your employer in the correct format"
    val expectedErrorOverMaximum = "The incurred costs paid by your employer must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much of your incurred costs did your employer pay?"
    val expectedHeading = "How much of your incurred costs did your employer pay?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of incurred costs paid by your employer"
    val expectedErrorIncorrectFormat = "Enter the amount of incurred costs paid by your employer in the correct format"
    val expectedErrorOverMaximum = "The incurred costs paid by your employer must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much of your client’s incurred costs did their employer pay?"
    val expectedHeading = "How much of your client’s incurred costs did their employer pay?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of incurred costs paid by your client’s employer"
    val expectedErrorIncorrectFormat = "Enter the amount of incurred costs paid by your client’s employer in the correct format"
    val expectedErrorOverMaximum = "The incurred costs paid by your client’s employer must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much of your client’s incurred costs did their employer pay?"
    val expectedHeading = "How much of your client’s incurred costs did their employer pay?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of incurred costs paid by your client’s employer"
    val expectedErrorIncorrectFormat = "Enter the amount of incurred costs paid by your client’s employer in the correct format"
    val expectedErrorOverMaximum = "The incurred costs paid by your client’s employer must be less than £100,000,000,000"
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
        "render the incurred costs benefits amount page with an empty amount field" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(
              Some(benefits(fullIncomeTaxOrIncurredCostsModel.copy(paymentsOnEmployeesBehalf = None))))), userRequest)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(enterTotalText, paragraphSelector(2))
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldCheck(amountFieldName, amountFieldSelector)
          inputFieldValueCheck("", amountFieldSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(formLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the incurred costs benefits amount page with a pre-filled amount field when there's cya data and prior benefits" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(
              Some(benefits(fullIncomeTaxOrIncurredCostsModel)))), userRequest)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(optionalParagraphText(amount), paragraphSelector(2))
          textOnPageCheck(enterTotalText, paragraphSelector(3))
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldCheck(amountFieldName, amountFieldSelector)
          inputFieldValueCheck(amount.toString(), amountFieldSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(formLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the incurred costs benefits amount page with a pre-filled amount field when there's cya data and no prior benefits" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(
              Some(benefits(fullIncomeTaxOrIncurredCostsModel)))), userRequest)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(optionalParagraphText(amount), paragraphSelector(2))
          textOnPageCheck(enterTotalText, paragraphSelector(3))
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldCheck(amountFieldName, amountFieldSelector)
          inputFieldValueCheck(amount.toString(), amountFieldSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(formLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the overview page when the tax year isn't end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(
          Some(benefits(fullIncomeTaxOrIncurredCostsModel)))), userRequest)
        urlGet(pageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect to the check your benefits page when there is no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when the incurred costs question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(
          Some(benefits(fullIncomeTaxOrIncurredCostsModel.copy(paymentsOnEmployeesBehalfQuestion = Some(false), paymentsOnEmployeesBehalf = None))))), userRequest)
        urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when the income tax or incurred costs question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(
          Some(benefits(emptyIncomeTaxOrIncurredCostsModel)))), userRequest)
        urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when the benefits received question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(
          Some(BenefitsViewModel(isUsingCustomerData = true)))), userRequest)
        urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        "render the incurred costs amount page with an error" when {
          "a form is submitted with no entry" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(
                fullIncomeTaxOrIncurredCostsModel)))), userRequest)
              urlPost(pageUrl(taxYearEOY), body = "", welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(optionalParagraphText(amount), paragraphSelector(2))
            textOnPageCheck(enterTotalText, paragraphSelector(3))
            hintTextCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck("", amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Some(amountFieldName))
            formPostLinkCheck(formLink, formSelector)

            welshToggleCheck(user.isWelsh)
          }

          "a form is submitted with an incorrectly formatted amount" which {
            val incorrectFormatAmount = "abc"
            val form: Map[String, String] = Map(AmountForm.amount -> incorrectFormatAmount)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(
                fullIncomeTaxOrIncurredCostsModel)))), userRequest)
              urlPost(pageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(optionalParagraphText(amount), paragraphSelector(2))
            textOnPageCheck(enterTotalText, paragraphSelector(3))
            hintTextCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(incorrectFormatAmount, amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountFieldName))
            formPostLinkCheck(formLink, formSelector)

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
                fullIncomeTaxOrIncurredCostsModel)))), userRequest)
              urlPost(pageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(optionalParagraphText(amount), paragraphSelector(2))
            textOnPageCheck(enterTotalText, paragraphSelector(3))
            hintTextCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(overMaximumAmount, amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, Some(amountFieldName))
            formPostLinkCheck(formLink, formSelector)

            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "update the paymentsOnEmployeesBehalf value when a user submits a valid form and has prior benefits, redirects to CYA" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(fullIncomeTaxOrIncurredCostsModel)))), userRequest)
        urlPost(pageUrl(taxYearEOY), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "update the paymentsOnEmployeesBehalf value to the new amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.paymentsOnEmployeesBehalf)) shouldBe Some(newAmount)
      }
    }

    "update the paymentsOnEmployeesBehalf value when a user submits a valid form and has no prior benefits, redirects to reimbursed section" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(fullIncomeTaxOrIncurredCostsModel)))), userRequest)
        urlPost(pageUrl(taxYearEOY), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the reimbursed costs section page" in {
        result.status shouldBe SEE_OTHER
//        TODO: This will update to the reimbursed costs section page when created
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "update the paymentsOnEmployeesBehalf value to the new amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.paymentsOnEmployeesBehalf)) shouldBe Some(newAmount)
      }
    }

    "redirect to the overview page when the tax year isn't end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(fullIncomeTaxOrIncurredCostsModel)))), userRequest)
        urlPost(pageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect to the check your benefits page when there is no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlPost(pageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when the incurred costs question is set to false" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(
          Some(benefits(fullIncomeTaxOrIncurredCostsModel.copy(paymentsOnEmployeesBehalfQuestion = Some(false), paymentsOnEmployeesBehalf = None))))), userRequest)
        urlPost(pageUrl(taxYearEOY), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when the income tax or incurred costs question is set to false" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(emptyIncomeTaxOrIncurredCostsModel)))), userRequest)
        urlPost(pageUrl(taxYearEOY), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when the benefits received question is set to false" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(BenefitsViewModel(isUsingCustomerData = true)))), userRequest)
        urlPost(pageUrl(taxYearEOY), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }

}