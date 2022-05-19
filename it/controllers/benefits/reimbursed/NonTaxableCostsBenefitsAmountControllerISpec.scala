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

import forms.AmountForm
import models.benefits.ReimbursedCostsVouchersAndNonCashModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.ReimbursedCostsVouchersAndNonCashModelBuilder.aReimbursedCostsVouchersAndNonCashModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{assetsBenefitsUrl, checkYourBenefitsUrl, fullUrl, nonTaxableCostsBenefitsAmountUrl, overviewUrl, taxableCostsBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class NonTaxableCostsBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"
  private val amountInModel: BigDecimal = 100
  private val amountInputName = "amount"
  private val amountFieldHref = "#amount"
  private val newAmount: BigDecimal = 500.55

  object Selectors {
    val ifItWasNotTextSelector = "#previous-amount-text"
    val enterTotalSelector = "#enter-total-text"
    val hintTextSelector = "#amount-hint"
    val prefixedCurrencySelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val inputSelector = "#amount"
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
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"

    def ifItWasNotText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."

    val enterTotalText = "Enter the total."
    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"

    def ifItWasNotText(amount: BigDecimal): String = s"Rhowch wybod y swm cywir os nad oedd yn £$amount."

    val enterTotalText = "Nodwch y cyfanswm."
    val expectedHintText = "Er enghraifft, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Yn eich blaen"
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
    val expectedTitle = "Faint oích costau anhrethadwy a gafodd eu had-dalu gan eich cyflogwr?"
    val expectedHeading = "Faint oích costau anhrethadwy a gafodd eu had-dalu gan eich cyflogwr?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch swm y costau anhrethadwy a ad-dalwyd gan eich cyflogwr"
    val expectedIncorrectFormatErrorMessage = "Nodwch swm y costau anhrethadwy a ad-dalwyd gan eich cyflogwr yn y fformat cywir"
    val expectedOverMaximumErrorMessage = "Maeín rhaid iír costau anhrethadwy a ad-dalwyd gan eich cyflogwr fod yn llai na £100,000,000,000"
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
    val expectedTitle = "Faint o gostau anhrethadwy eich cleient a gafodd eu had-dalu gan ei gyflogwr?"
    val expectedHeading = "Faint o gostau anhrethadwy eich cleient a gafodd eu had-dalu gan ei gyflogwr?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch swm y costau anhrethadwy a ad-dalwyd gan gyflogwr eich cleient"
    val expectedIncorrectFormatErrorMessage = "Nodwch swm y costau anhrethadwy a ad-dalwyd gan gyflogwr eich cleient yn y fformat cywir"
    val expectedOverMaximumErrorMessage = "Maeín rhaid iír costau anhrethadwy a ad-dalwyd gan gyflogwr eich cleient fod yn llai na £100,000,000,000"
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
        "render the non-taxable costs benefits amount page with an empty amount field" when {
          "the prior amount and cya amount are the same" which {
            lazy val amount: BigDecimal = 22
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expenses = Some(22.00))))
              insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
              urlGet(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amount), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            welshToggleCheck(user.isWelsh)
          }

          "there is no prior value (expenses is None)" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expenses = None)))
              insertCyaData(anEmploymentUserDataWithBenefits(hasPriorBenefits = false, benefits = benefitsViewModel))
              urlGet(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            elementsNotOnPageCheck(ifItWasNotTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            welshToggleCheck(user.isWelsh)
          }
        }

        "render the non-taxable costs benefits amount page with a pre-filled amount field" when {
          "the cya amount and the prior data amount are different" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserData)
              urlGet(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountInModel.toString())
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            welshToggleCheck(user.isWelsh)
          }

          "the user has cya data and no prior benefits" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserDataWithBenefits(aBenefitsViewModel, hasPriorBenefits = false))
              urlGet(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, amountInModel.toString())
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

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
        insertCyaData(anEmploymentUserData)
        urlGet(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        urlGet(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to taxable expenses question when non-taxable expenses question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(hasPriorBenefits = false, benefits = benefitsViewModel))
        urlGet(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(taxableCostsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to Assets section question page when reimbursed costs, vouchers and non-cash question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(hasPriorBenefits = false, benefits = benefitsViewModel))
        urlGet(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
              insertCyaData(anEmploymentUserData)
              urlPost(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

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
              insertCyaData(anEmploymentUserData)
              urlPost(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, incorrectFormatAmount)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

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
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserData)
              urlPost(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
            inputFieldValueCheck(amountInputName, inputSelector, overMaximumAmount)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            errorAboveElementCheck(user.specificExpectedResults.get.expectedOverMaximumErrorMessage, Some(amountInputName))
            errorSummaryCheck(user.specificExpectedResults.get.expectedOverMaximumErrorMessage, amountFieldHref)

            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "update cya when a user submits a valid form and has prior benefits, redirects to the CYA page" which {

      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      s"update expenses in reimbursedCostsVouchersAndNonCash model" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expensesQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expenses)) shouldBe Some(newAmount)
      }
    }

    "update cya when a user submits a valid form and doesn't have prior benefits, redirects to the taxable costs page" which {
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(hasPriorBenefits = false, benefits = benefitsViewModel))
        urlPost(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the taxable costs page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(taxableCostsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      s"update expenses in reimbursedCostsVouchersAndNonCash model" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expensesQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expenses)) shouldBe Some(newAmount)
      }
    }

    "redirect to the overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        urlPost(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to taxable expenses question when non-taxable expenses question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(hasPriorBenefits = false, benefits = benefitsViewModel))
        urlPost(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(taxableCostsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to Assets section question page when reimbursed costs, vouchers and non-cash question is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(hasPriorBenefits = false, benefits = benefitsViewModel))
        urlPost(fullUrl(nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(assetsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
