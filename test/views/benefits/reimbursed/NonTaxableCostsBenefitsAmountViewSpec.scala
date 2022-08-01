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

package views.benefits.reimbursed

import controllers.benefits.reimbursed.routes.NonTaxableCostsBenefitsAmountController
import forms.AmountForm
import forms.benefits.reimbursed.ReimbursedFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.reimbursed.NonTaxableCostsBenefitsAmountView

class NonTaxableCostsBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"
  private val amountInModel: BigDecimal = 100
  private val amountInputName = "amount"
  private val amountFieldHref = "#amount"

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

    val enterTotalText = "Enter the total."
    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"

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

  private def form(isAgent: Boolean): Form[BigDecimal] = new ReimbursedFormsProvider().nonTaxableCostsAmountForm(isAgent)

  private lazy val underTest = inject[NonTaxableCostsBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render amount page with not prefilled form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(enterTotalText, enterTotalSelector)
        elementNotOnPageCheck(ifItWasNotTextSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonTaxableCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there is previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(enterTotalText, enterTotalSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, amountInModel.toString())
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonTaxableCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "should render the amount page with empty value error text when there is no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(enterTotalText, enterTotalSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonTaxableCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, Some(amountInputName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, amountFieldHref)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted with incorrectly formatted amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(enterTotalText, enterTotalSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonTaxableCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedIncorrectFormatErrorMessage, Some(amountInputName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedIncorrectFormatErrorMessage, amountFieldHref)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted and the amount is over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(enterTotalText, enterTotalSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "100,000,000,000")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonTaxableCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedOverMaximumErrorMessage, Some(amountInputName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedOverMaximumErrorMessage, amountFieldHref)

        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
