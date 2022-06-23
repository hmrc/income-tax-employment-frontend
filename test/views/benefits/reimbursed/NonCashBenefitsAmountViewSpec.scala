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

import controllers.benefits.reimbursed.routes.NonCashBenefitsAmountController
import forms.AmountForm
import forms.benefits.reimbursed.ReimbursedFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.reimbursed.NonCashBenefitsAmountView

class NonCashBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"
  private val amountInModel: BigDecimal = 400
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
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"

    def ifItWasNotText(amount: BigDecimal): String = s"Rhowch wybod y swm cywir os nad oedd yn £$amount."

    val expectedHintText = "Er enghraifft, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Yn eich blaen"
    val expectedIncorrectFormatErrorMessage = "Nodwch y swm ar gyfer buddiannau nad ydynt yn arian parod yn y fformat cywir"
    val expectedOverMaximumErrorMessage = "Maeín rhaid iír swm a nodwyd ar gyfer buddiannau sydd ddim yn arian parod fod yn llai na £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much did you get in total for non-cash benefits?"
    val expectedHeading = "How much did you get in total for non-cash benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you got for non-cash benefits"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint y cawsoch i gyd ar gyfer buddiannau sydd ddim yn arian parod?"
    val expectedHeading = "Faint y cawsoch i gyd ar gyfer buddiannau sydd ddim yn arian parod?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch y swm a gawsoch ar gyfer buddiannau sydd ddim yn arian parod"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much did your client get in total for non-cash benefits?"
    val expectedHeading = "How much did your client get in total for non-cash benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount your client got for non-cash benefits"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint y cafodd eich cleient i gyd ar gyfer buddiannau sydd ddim yn arian parod?"
    val expectedHeading = "Faint y cafodd eich cleient i gyd ar gyfer buddiannau sydd ddim yn arian parod?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch y swm a gafodd eich cleient ar gyfer buddiannau sydd ddim yn arian parod"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new ReimbursedFormsProvider().nonCashAmountForm(isAgent)

  private lazy val underTest = inject[NonCashBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render amount page with not prefilled form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), None, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        elementNotOnPageCheck(ifItWasNotTextSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonCashBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there is previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "400")), Some(400), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, amountInModel.toString())
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonCashBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "should render the amount page with empty value error text when there is no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), Some(400), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonCashBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, Some(amountInputName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, amountFieldHref)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted with incorrectly formatted amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), Some(400), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonCashBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        errorAboveElementCheck(expectedIncorrectFormatErrorMessage, Some(amountInputName))
        errorSummaryCheck(expectedIncorrectFormatErrorMessage, amountFieldHref)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted and the amount is over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), Some(400), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(ifItWasNotText(amountInModel), ifItWasNotTextSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "100,000,000,000")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonCashBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        errorAboveElementCheck(expectedOverMaximumErrorMessage, Some(amountInputName))
        errorSummaryCheck(expectedOverMaximumErrorMessage, amountFieldHref)

        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
