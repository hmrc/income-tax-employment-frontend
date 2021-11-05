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

package controllers.benefits.travelAndEntertainment

import controllers.benefits.travelAndEntertainment.routes.EntertainingBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.AmountForm
import models.User
import models.benefits.{BenefitsViewModel, TravelEntertainmentModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class IncidentalCostsBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId: String = "001"
  val prefilledAmount: BigDecimal = 200
  val amountInputName = "amount"

  def incidentalOvernightCostsAmountPageUrl(taxYear: Int): String =
    s"$appUrl/$taxYear/benefits/incidental-overnight-costs-amount?employmentId=$employmentId"

  val continueLink: String =
    s"/income-through-software/return/employment-income/$taxYearEOY/benefits/incidental-overnight-costs-amount?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = hasPriorBenefits,
      hasPriorBenefits = hasPriorBenefits, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(travelEntertainmentModel: TravelEntertainmentModel): BenefitsViewModel =
    BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true, travelEntertainmentModel = Some(travelEntertainmentModel))

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > label > header > p"
    val optionalParagraphSelector = "#main-content > div > div > form > div > label > p"
    val hintTextSelector = "#amount-hint"
    val currencyPrefixSelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val amountInputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String

    def optionalParagraphText(amount: BigDecimal): String

    val expectedHintText: String
    val currencyPrefix: String
    val continueButtonText: String
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
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"

    def optionalParagraphText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."

    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"

    def optionalParagraphText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."

    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much did you get in total for incidental overnight costs?"
    val expectedHeading = "How much did you get in total for incidental overnight costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount you got for incidental overnight costs"
    val expectedErrorIncorrectFormat = "Enter the amount you got for incidental overnight costs in the correct format"
    val expectedErrorOverMaximum = "Your incidental overnight costs must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much did you get in total for incidental overnight costs?"
    val expectedHeading = "How much did you get in total for incidental overnight costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount you got for incidental overnight costs"
    val expectedErrorIncorrectFormat = "Enter the amount you got for incidental overnight costs in the correct format"
    val expectedErrorOverMaximum = "Your incidental overnight costs must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much did your client get in total for incidental overnight costs?"
    val expectedHeading = "How much did your client get in total for incidental overnight costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount your client got for incidental overnight costs"
    val expectedErrorIncorrectFormat = "Enter the amount your client got for incidental overnight costs in the correct format"
    val expectedErrorOverMaximum = "Your client’s incidental overnight costs must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much did your client get in total for incidental overnight costs?"
    val expectedHeading = "How much did your client get in total for incidental overnight costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount your client got for incidental overnight costs"
    val expectedErrorIncorrectFormat = "Enter the amount your client got for incidental overnight costs in the correct format"
    val expectedErrorOverMaximum = "Your client’s incidental overnight costs must be less than £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'incidental overnight expenses amount' page with no pre-filled amount field" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
              Some(benefits(fullTravelOrEntertainmentModel.copy(personalIncidentalExpenses = None))))), userRequest)
            urlGet(incidentalOvernightCostsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          elementNotOnPageCheck(optionalParagraphSelector)
          hintTextCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldCheck(amountInputName, amountInputSelector)
          inputFieldValueCheck("", amountInputSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)

          welshToggleCheck(user.isWelsh)

        }

        "render the 'incidental overnight expenses amount' page with the amount field pre-filled when there's cya data and prior benefits exist" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
              Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
            urlGet(incidentalOvernightCostsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(optionalParagraphText(prefilledAmount), optionalParagraphSelector)
          hintTextCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldCheck(amountInputName, amountInputSelector)
          inputFieldValueCheck(prefilledAmount.toString(), amountInputSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)

          welshToggleCheck(user.isWelsh)

        }

        "render the 'incidental overnight expenses amount' page with the amount field pre-filled when there's cya data and no prior benefits exist" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
              Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
            urlGet(incidentalOvernightCostsAmountPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(optionalParagraphText(prefilledAmount), optionalParagraphSelector)
          hintTextCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldCheck(amountInputName, amountInputSelector)
          inputFieldValueCheck(prefilledAmount.toString(), amountInputSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)

          welshToggleCheck(user.isWelsh)

        }

      }
    }

    "redirect to the overview page when it's not end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
        urlGet(incidentalOvernightCostsAmountPageUrl(taxYear), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect to the incidental overnight expenses radio button page when the personalIncidentalExpensesQuestion is None " which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
          Some(benefits(fullTravelOrEntertainmentModel.copy(personalIncidentalExpensesQuestion = None))))), userRequest)
        urlGet(incidentalOvernightCostsAmountPageUrl(taxYearEOY), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/incidental-overnight-costs?employmentId=$employmentId")
      }
    }

    "redirect to the entertaining question when the personalIncidentalExpensesQuestion is Some(false) and no prior benefits exist" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
          Some(benefits(fullTravelOrEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false)))))), userRequest)
        urlGet(incidentalOvernightCostsAmountPageUrl(taxYearEOY), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EntertainingBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when personalIncidentalExpensesQuestion is Some(false) and prior benefits exist" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(fullTravelOrEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false)))))), userRequest)
        urlGet(incidentalOvernightCostsAmountPageUrl(taxYearEOY), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
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

          "a form is submitted with no entry" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
                fullTravelOrEntertainmentModel)))), userRequest)
              urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = "", welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(optionalParagraphText(prefilledAmount), optionalParagraphSelector)
            hintTextCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldCheck(amountInputName, amountInputSelector)
            inputFieldValueCheck("", amountInputSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, amountInputSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Some(amountInputName))
            formPostLinkCheck(continueLink, formSelector)

            welshToggleCheck(user.isWelsh)
          }

          "a form is submitted with an incorrect format" which {

            val incorrectAmount = "abc"
            val form: Map[String, String] = Map(AmountForm.amount -> incorrectAmount)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
                fullTravelOrEntertainmentModel)))), userRequest)
              urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(optionalParagraphText(prefilledAmount), optionalParagraphSelector)
            hintTextCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldCheck(amountInputName, amountInputSelector)
            inputFieldValueCheck(incorrectAmount, amountInputSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, amountInputSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountInputName))
            formPostLinkCheck(continueLink, formSelector)

            welshToggleCheck(user.isWelsh)
          }

          "a form is submitted and the amount is over the maximum limit" which {

            val overMaxAmount = "100,000,000,000,000,000,000"
            val form: Map[String, String] = Map(AmountForm.amount -> overMaxAmount)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
                fullTravelOrEntertainmentModel)))), userRequest)
              urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(optionalParagraphText(prefilledAmount), optionalParagraphSelector)
            hintTextCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldCheck(amountInputName, amountInputSelector)
            inputFieldValueCheck(overMaxAmount, amountInputSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, amountInputSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, Some(amountInputName))
            formPostLinkCheck(continueLink, formSelector)

            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "update cya when a valid form is submitted and prior benefits exist" which {

      val newAmount: BigDecimal = 280.35
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
        urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), follow = false, body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to the entertaining page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EntertainingBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelEntertainmentQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpensesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpenses)) shouldBe Some(newAmount)
      }

    }

    "update cya when a valid form is submitted and no prior benefits exist" which {

      val newAmount: BigDecimal = 534.21
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
          Some(benefits(fullTravelOrEntertainmentModel.copy(personalIncidentalExpenses = None))))), userRequest)
        urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), follow = false, body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to the entertaining question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EntertainingBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelEntertainmentQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpensesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpenses)) shouldBe Some(newAmount)
      }

    }

    "redirect to the overview page when it's not end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(fullTravelOrEntertainmentModel)))), userRequest)
        urlGet(incidentalOvernightCostsAmountPageUrl(taxYear), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect to the incidental overnight expenses radio button page when the personalIncidentalExpensesQuestion is None " which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
          Some(benefits(fullTravelOrEntertainmentModel.copy(personalIncidentalExpensesQuestion = None))))), userRequest)
        urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = "", follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/incidental-overnight-costs?employmentId=$employmentId")
      }
    }

    "redirect to the entertaining question when the personalIncidentalExpensesQuestion is Some(false) and no prior benefits exist" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
          Some(benefits(fullTravelOrEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false)))))), userRequest)
        urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = "", follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EntertainingBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when personalIncidentalExpensesQuestion is Some(false) and prior benefits exist" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(fullTravelOrEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false)))))), userRequest)
        urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = "", follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }
  }

}
