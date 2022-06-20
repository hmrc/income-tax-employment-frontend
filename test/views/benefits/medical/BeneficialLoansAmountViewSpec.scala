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

package views.benefits.medical

import controllers.benefits.medical.routes.BeneficialLoansAmountController
import forms.AmountForm
import forms.benefits.medical.MedicalFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.medical.BeneficialLoansAmountView

class BeneficialLoansAmountViewSpec extends ViewUnitTest {

  private val amountInputName = "amount"
  private val amountFieldHref = "#amount"
  private val employmentId: String = "employmentId"

  object Selectors {
    val paragraphTextSelector: Int => String = (i: Int) => s"#main-content > div > div > p:nth-child($i)"
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
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"

    def ifItWasNotText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."

    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"

    def ifItWasNotText(amount: BigDecimal): String = s"Rhowch wybod y swm cywir os nad oedd yn £$amount."

    val expectedHintText = "Er enghraifft, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Yn eich blaen"
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
    val expectedTitle = "Faint oedd eich benthyciadau buddiannol i gyd?"
    val expectedHeading = "Faint oedd eich benthyciadau buddiannol i gyd?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val youCanFindText = "Maeír wybodaeth hon ar gael yn adran H, blwch 15 ar eich ffurflen P11D."
    val expectedNoEntryErrorMessage = "Nodwch swm eich benthyciadau buddiannol"
    val expectedIncorrectFormatErrorMessage = "Nodwch swm eich benthyciadau buddiannol yn y fformat cywir"
    val expectedOverMaximumErrorMessage = "Maeín rhaid iích benthyciadau buddiannol fod yn llai na £100,000,000,000"
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
    val expectedTitle = "Faint oedd benthyciadau buddiannol eich cleient i gyd?"
    val expectedHeading = "Faint oedd benthyciadau buddiannol eich cleient i gyd?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val youCanFindText = "Maeír wybodaeth hon ar gael yn adran H, blwch 15 ar ffurflen P11D eich cleient."
    val expectedNoEntryErrorMessage = "Nodwch swm benthyciadau buddiannol eich cleient"
    val expectedIncorrectFormatErrorMessage = "Nodwch swm benthyciadau buddiannol eich cleient yn y fformat cywir"
    val expectedOverMaximumErrorMessage = "Maeín rhaid i fenthyciadau buddiannol eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new MedicalFormsProvider().beneficialLoansAmountForm(isAgent)

  private lazy val underTest = inject[BeneficialLoansAmountView]

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
        textOnPageCheck(userScenario.specificExpectedResults.get.youCanFindText, paragraphTextSelector(2))
        elementNotOnPageCheck(paragraphTextSelector(3))
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(BeneficialLoansAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there is previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), Some(18.00), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(ifItWasNotText(amount = 18), paragraphTextSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.youCanFindText, paragraphTextSelector(3))
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(BeneficialLoansAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with an error when theres no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), Some(400), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(ifItWasNotText(amount = 400), paragraphTextSelector(3))
        textOnPageCheck(userScenario.specificExpectedResults.get.youCanFindText, paragraphTextSelector(4))
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(BeneficialLoansAmountController.submit(taxYearEOY, employmentId).url, formSelector)

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
        textOnPageCheck(ifItWasNotText(amount = 400), paragraphTextSelector(3))
        textOnPageCheck(userScenario.specificExpectedResults.get.youCanFindText, paragraphTextSelector(4))
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(BeneficialLoansAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedIncorrectFormatErrorMessage, Some(amountInputName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedIncorrectFormatErrorMessage, amountFieldHref)

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
        textOnPageCheck(ifItWasNotText(amount = 400), paragraphTextSelector(3))
        textOnPageCheck(userScenario.specificExpectedResults.get.youCanFindText, paragraphTextSelector(4))
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, prefixedCurrencySelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "100,000,000,000")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(BeneficialLoansAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedOverMaximumErrorMessage, Some(amountInputName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedOverMaximumErrorMessage, amountFieldHref)

        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
