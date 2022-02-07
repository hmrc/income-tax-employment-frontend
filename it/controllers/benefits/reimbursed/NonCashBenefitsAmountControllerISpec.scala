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

package controllers.benefits.reimbursed

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.benefits.ReimbursedCostsVouchersAndNonCashModelBuilder.aReimbursedCostsVouchersAndNonCashModel
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import forms.AmountForm
import models.benefits.ReimbursedCostsVouchersAndNonCashModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{assetsBenefitsUrl, checkYourBenefitsUrl, fullUrl, nonCashBenefitsAmountUrl, otherBenefitsUrl, overviewUrl}

class NonCashBenefitsAmountControllerISpec extends IntegrationTest with EmploymentDatabaseHelper with ViewHelpers {

  private val taxYearEOY: Int = 2021
  private val employmentId: String = "employmentId"
  private val amountInModel: BigDecimal = 400
  private val newAmount: BigDecimal = 19.99
  private val amountInputName = "amount"
  private val amountFieldHref = "#amount"

  object Selectors {
    val ifItWasNotTextSelector = "#previous-amount-text"
    val hintTextSelector = "#amount-hint"
    val prefixedCurrencySelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val inputSelector = "#amount"
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
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"

    def ifItWasNotText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."

    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
    val expectedIncorrectFormatErrorMessage = "Enter the amount for non-cash benefits in the correct format"
    val expectedOverMaximumErrorMessage = "The amount for non-cash benefits must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"

    def ifItWasNotText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."

    val expectedHintText = "For example, £193.52"
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
        "render the non-cash benefits amount page with an empty amount field" when {
          "the prior amount and cya amount are the same" which {
            lazy val amount: BigDecimal = 25
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCash = Some(25.00))))
              insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
              urlGet(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amount), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonCashBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            welshToggleCheck(user.isWelsh)
          }

          "there is no prior value (nonCash is None)" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCash = None)))
              insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false), aUserRequest)
              urlGet(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            elementNotOnPageCheck(ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonCashBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            welshToggleCheck(user.isWelsh)
          }
        }

        "render the non-cash benefits amount page with a pre-filled amount field" when {
          "the cya amount and the prior data amount are different" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserData, aUserRequest)
              urlGet(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountInModel.toString())
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonCashBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            welshToggleCheck(user.isWelsh)
          }

          "the user has cya data and no prior benefits" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserDataWithBenefits(aBenefitsViewModel, hasPriorBenefits = false), aUserRequest)
              urlGet(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountInModel.toString())
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonCashBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "redirect to the overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserDataWithBenefits(aBenefitsViewModel), aUserRequest)
        urlGet(fullUrl(nonCashBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check your benefits page when there's no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to other items yes no question page when non-cash question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, hasPriorBenefits = false), aUserRequest)
        urlGet(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(otherBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to Assets section question page page when reimbursed costs, vouchers and non-cash question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, hasPriorBenefits = false), aUserRequest)
        urlGet(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(assetsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
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
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserDataWithBenefits(aBenefitsViewModel), aUserRequest)
              urlPost(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonCashBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

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
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserDataWithBenefits(aBenefitsViewModel), aUserRequest)
              urlPost(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, incorrectFormatAmount)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonCashBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            errorAboveElementCheck(expectedIncorrectFormatErrorMessage, Some(amountInputName))
            errorSummaryCheck(expectedIncorrectFormatErrorMessage, amountFieldHref)

            welshToggleCheck(user.isWelsh)
          }

          "a form is submitted and the amount is over the maximum limit" which {
            val overMaximumAmount = "100,000,000,000,000,000,000"
            val form: Map[String, String] = Map(AmountForm.amount -> overMaximumAmount)
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserDataWithBenefits(aBenefitsViewModel), aUserRequest)
              urlPost(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, overMaximumAmount)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonCashBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            errorAboveElementCheck(expectedOverMaximumErrorMessage, Some(amountInputName))
            errorSummaryCheck(expectedOverMaximumErrorMessage, amountFieldHref)

            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "update cya when a user submits a valid form and has prior benefits, redirects to CYA page" which {
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserDataWithBenefits(aBenefitsViewModel), aUserRequest)
        urlPost(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      s"update nonCash in reimbursedCostsVouchersAndNonCash model" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.nonCashQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.nonCash)) shouldBe Some(newAmount)
      }
    }

    "update cya when a user submits a valid form and doesn't have prior benefits, redirects to the other benefits question page" which {
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, hasPriorBenefits = false), aUserRequest)
        urlPost(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to other benefits question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(otherBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      s"update expenses in reimbursedCostsVouchersAndNonCash model" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.nonCashQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.nonCash)) shouldBe Some(newAmount)
      }
    }

    "redirect to the overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserDataWithBenefits(aBenefitsViewModel), aUserRequest)
        urlPost(fullUrl(nonCashBenefitsAmountUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check your benefits page when there's no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlPost(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to other items yes no question page when non-cash question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, hasPriorBenefits = false), aUserRequest)
        urlPost(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(otherBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to Assets section question page when reimbursed costs, vouchers and non-cash question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, hasPriorBenefits = false), aUserRequest)
        urlPost(fullUrl(nonCashBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(assetsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}