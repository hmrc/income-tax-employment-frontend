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

package views.expenses

import controllers.expenses.routes._
import forms.AmountForm
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.UniformsOrToolsExpensesAmountView

class UniformsOrToolsExpensesAmountViewSpec extends ViewUnitTest {

  private val poundPrefixText = "£"
  private val newAmount: BigDecimal = 250
  private val amountField = "#amount"
  private val amountFieldName = "amount"

  object Selectors {
    val formSelector = "#main-content > div > div > form"
    val wantToClaimSelector: String = "#previous-amount"
    val cannotClaimParagraphSelector: String = "#cannot-claim"
    val totalAmountParagraphSelector: String = "#total-amount-text"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val continueButtonSelector: String = "#continue"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String

    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedInvalidFormatErrorMessage: String
    val expectedOverMaximumErrorMessage: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val totalAmountText: String
    val hintText: String
    val expectedCannotClaim: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for uniforms, work clothes, or tools?"
    val expectedHeading = "How much do you want to claim for uniforms, work clothes, or tools?"

    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you want to claim for uniforms, work clothes, or tools"
    val expectedInvalidFormatErrorMessage = "Enter the amount you want to claim for uniforms, work clothes, or tools in the correct format"
    val expectedOverMaximumErrorMessage = "The amount you want to claim for uniforms, work clothes, or tools must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer?"
    val expectedHeading = "Faint rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer?"

    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch y swm rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer"
    val expectedInvalidFormatErrorMessage = "Nodwch y swm rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer yn y format cywir"
    val expectedOverMaximumErrorMessage = "Maeín rhaid iír swm rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for uniforms, work clothes, or tools for your client?"
    val expectedHeading = "How much do you want to claim for uniforms, work clothes, or tools for your client?"

    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you want to claim for your client’s uniforms, work clothes, or tools"
    val expectedInvalidFormatErrorMessage = "Enter the amount you want to claim for your client’s uniforms, work clothes, or tools in the correct format"
    val expectedOverMaximumErrorMessage = "The amount you want to claim for your client’s uniforms, work clothes, or tools must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith neu offer ar gyfer eich cleient?"
    val expectedHeading = "Faint rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith neu offer ar gyfer eich cleient?"

    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch y swm rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith neu offer eich cleient"
    val expectedInvalidFormatErrorMessage = "Nodwch y swm rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith neu offer eich cleient yn y fformat cywir"
    val expectedOverMaximumErrorMessage = "Maeín rhaid iír swm rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer eich cleient fod yn llai na £100,000,000,000"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment expenses for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val totalAmountText = "Total amount for all employers"
    val hintText = "For example, £193.52"
    val expectedCannotClaim = "You cannot claim for the initial cost of buying small tools or clothing for work."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val continueButtonText = "Yn eich blaen"
    val totalAmountText = "Cyfanswm ar gyfer pob cyflogwr"
    val hintText = "Er enghraifft, £193.52"
    val expectedCannotClaim = "Ni allwch hawlio ar gyfer y gost gychwynnol o brynu m‚n offer neu ddillad ar gyfer gwaith."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new ExpensesFormsProvider().uniformsWorkClothesToolsAmountForm(isAgent)

  private lazy val underTest = inject[UniformsOrToolsExpensesAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the amount page with no prefilled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        elementsNotOnPageCheck(wantToClaimSelector)
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCannotClaim, cannotClaimParagraphSelector)
        textOnPageCheck(totalAmountText, totalAmountParagraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountFieldName, amountField, "")
        formPostLinkCheck(UniformsOrToolsExpensesAmountController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with a pre-filled amount field" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).fill(value = 250))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCannotClaim, cannotClaimParagraphSelector)
        textOnPageCheck(totalAmountText, totalAmountParagraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountFieldName, amountField, newAmount.toString())
        formPostLinkCheck(UniformsOrToolsExpensesAmountController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with no pre-filled cya data if amount has not changed" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCannotClaim, cannotClaimParagraphSelector)
        textOnPageCheck(totalAmountText, totalAmountParagraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountFieldName, amountField, "")
        formPostLinkCheck(UniformsOrToolsExpensesAmountController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when a form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCannotClaim, cannotClaimParagraphSelector)
        textOnPageCheck(totalAmountText, totalAmountParagraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountFieldName, amountField, "")
        formPostLinkCheck(UniformsOrToolsExpensesAmountController.submit(taxYearEOY).url, formSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, Some(amountFieldName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, amountField)
        welshToggleCheck(userScenario.isWelsh)

      }

      "a form is submitted with an incorrectly formatted amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCannotClaim, cannotClaimParagraphSelector)
        textOnPageCheck(totalAmountText, totalAmountParagraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountFieldName, amountField, "123.33.33")
        formPostLinkCheck(UniformsOrToolsExpensesAmountController.submit(taxYearEOY).url, formSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedInvalidFormatErrorMessage, Some(amountFieldName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedInvalidFormatErrorMessage, amountField)
        welshToggleCheck(userScenario.isWelsh)
      }

      "a form is submitted and the amount is over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCannotClaim, cannotClaimParagraphSelector)
        textOnPageCheck(totalAmountText, totalAmountParagraphSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountFieldName, amountField, value = "100,000,000,000")
        formPostLinkCheck(UniformsOrToolsExpensesAmountController.submit(taxYearEOY).url, formSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedOverMaximumErrorMessage, Some(amountFieldName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedOverMaximumErrorMessage, amountField)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
