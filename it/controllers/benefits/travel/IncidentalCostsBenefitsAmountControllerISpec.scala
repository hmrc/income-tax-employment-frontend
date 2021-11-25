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

package controllers.benefits.travel

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.benefits.TravelEntertainmentModelBuilder.aTravelEntertainmentModel
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import controllers.benefits.travel.routes.EntertainingBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.AmountForm
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class IncidentalCostsBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId: String = "employmentId"
  private val prefilledAmount: BigDecimal = 200
  private val amountInputName = "amount"
  private val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/incidental-overnight-costs-amount?employmentId=$employmentId"

  private def incidentalOvernightCostsAmountPageUrl(taxYear: Int): String =
    s"$appUrl/$taxYear/benefits/incidental-overnight-costs-amount?employmentId=$employmentId"


  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    anEmploymentUserData.copy(isPriorSubmission = hasPriorBenefits, hasPriorBenefits = hasPriorBenefits, employment = employmentCyaModel)

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > label > header > p"
    val optionalParagraphSelector = "#main-content > div > div > form > div > label > p"
    val hintTextSelector = "#amount-hint"
    val currencyPrefixSelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val inputSelector = "#amount"
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
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpenses = None)))
            insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
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
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the 'incidental overnight expenses amount' page with the amount field pre-filled when there's cya data and prior benefits exist" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
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
          inputFieldValueCheck(amountInputName, inputSelector, prefilledAmount.toString())
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the 'incidental overnight expenses amount' page with the amount field pre-filled when there's cya data and no prior benefits exist" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
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
          inputFieldValueCheck(amountInputName, inputSelector, prefilledAmount.toString())
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
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
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
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = None)))
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        urlGet(incidentalOvernightCostsAmountPageUrl(taxYearEOY), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/incidental-overnight-costs?employmentId=$employmentId")
      }
    }

    "redirect to the entertaining question when the personalIncidentalExpensesQuestion is Some(false) and no prior benefits exist" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false))))
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
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
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false))))
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
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
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
              urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = "", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, inputSelector)
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
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
              urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
            inputFieldValueCheck(amountInputName, inputSelector, incorrectAmount)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, inputSelector)
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
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
              urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
            inputFieldValueCheck(amountInputName, inputSelector, overMaxAmount)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, inputSelector)
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
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val model = aBenefitsViewModel.copy(utilitiesAndServicesModel = None)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(model))), aUserRequest)
        urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to the entertaining page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EntertainingBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpensesQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpenses)) shouldBe Some(newAmount)
      }
    }

    "update cya when a valid form is submitted and no prior benefits exist" which {
      val newAmount: BigDecimal = 534.21
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel
          .copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpenses = None)))
          .copy(utilitiesAndServicesModel = None)
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to the entertaining question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EntertainingBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpensesQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpenses)) shouldBe Some(newAmount)
      }
    }

    "redirect to the overview page when it's not end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
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
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = None)))
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = "", follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/incidental-overnight-costs?employmentId=$employmentId")
      }
    }

    "redirect to the entertaining question when the personalIncidentalExpensesQuestion is Some(false) and no prior benefits exist" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false))))
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false))))
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        urlPost(incidentalOvernightCostsAmountPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }
}
