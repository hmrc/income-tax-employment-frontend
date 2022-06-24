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

import controllers.expenses.routes.ProfFeesAndSubscriptionsExpensesAmountController
import forms.AmountForm
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.ProfFeesAndSubscriptionsExpensesAmountView

class ProfFeesAndSubscriptionsExpensesAmountViewSpec extends ViewUnitTest {

  private val amount: BigDecimal = 400
  private val newAmount: BigDecimal = 100
  private val amountFieldName = "amount"
  private val expectedErrorHref = "#amount"
  private val poundPrefixText = "£"
  private val maxLimit: String = "100,000,000,000"

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val replayTextSelector: String = "#previous-amount-text"
    val totalAmountTextSelector: String = "#total-amount-text"
    val hintTextSelector = "#amount-hint"
    val amountFieldSelector = "#amount"
    val poundPrefixSelector = ".govuk-input__prefix"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTotalAmountParagraph: String
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

    def expectedReplayText(amount: BigDecimal): String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTotalAmountParagraph = "Total amount for all employers"
    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTotalAmountParagraph = "Cyfanswm ar gyfer pob cyflogwr"
    val expectedHintText = "Er enghraifft, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for professional fees and subscriptions?"
    val expectedHeading = "How much do you want to claim for professional fees and subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount you want to claim for professional fees and subscriptions"
    val expectedErrorIncorrectFormat = "Enter the amount you want to claim for professional fees and subscriptions in the correct format"
    val expectedErrorOverMaximum = "The amount you want to claim for professional fees and subscriptions must be less than £100,000,000,000"

    def expectedReplayText(amount: BigDecimal): String = s"You told us you want to claim £$amount for professional fees and subscriptions. Tell us if this has changed."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol?"
    val expectedHeading = "Faint rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch y swm rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol"
    val expectedErrorIncorrectFormat = "Nodwch y swm rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol yn y fformat cywir"
    val expectedErrorOverMaximum = "Maeín rhaid iír swm rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol fod yn llai na £100,000,000,000"

    def expectedReplayText(amount: BigDecimal): String = s"Dywedoch wrthym eich bod am hawlio £$amount ar gyfer ffioedd a thanysgrifiadau proffesiynol. " +
      "Rhowch wybod i ni os yw hyn wedi newid."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for professional fees and subscriptions for your client?"
    val expectedHeading = "How much do you want to claim for professional fees and subscriptions for your client?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount you want to claim for your client’s professional fees and subscriptions"
    val expectedErrorIncorrectFormat = "Enter the amount you want to claim for your client’s professional fees and subscriptions in the correct format"
    val expectedErrorOverMaximum = "The amount you want to claim for your client’s professional fees and subscriptions must be less than £100,000,000,000"

    def expectedReplayText(amount: BigDecimal): String = s"You told us you want to claim £$amount for your client’s professional fees and subscriptions. Tell us if this has changed."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol ar gyfer eich cleient?"
    val expectedHeading = "Faint rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol ar gyfer eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch y swm rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol eich cleient"
    val expectedErrorIncorrectFormat = "Nodwch y swm rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol eich cleient yn y fformat cywir"
    val expectedErrorOverMaximum = "Maeín rhaid iír swm rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol eich cleient fod yn llai na £100,000,000,000"

    def expectedReplayText(amount: BigDecimal): String = s"Dywedoch wrthym eich bod chi am hawlio £$amount ar gyfer ffioedd a thanysgrifiadau " +
      "proffesiynol eich cleient. Rhowch wybod i ni os yw hyn wedi newid."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new ExpensesFormsProvider().professionalFeesAndSubscriptionsAmountForm(isAgent)

  private lazy val underTest = inject[ProfFeesAndSubscriptionsExpensesAmountView]

  ".show" should {
    userScenarios.foreach { userScenario =>
      import Selectors._
      import userScenario.commonExpectedResults._
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render the professional fees and subscriptions expenses amount page with an empty amount field" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), None)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          elementsNotOnPageCheck(replayTextSelector)
          textOnPageCheck(expectedTotalAmountParagraph, totalAmountTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, "")
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(ProfFeesAndSubscriptionsExpensesAmountController.submit(taxYearEOY).url, formSelector)

          welshToggleCheck(userScenario.isWelsh)
        }

        "render the professional fees and subscriptions expenses amount page with pre-filled cya data" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100")), Some(100))

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplayText(newAmount), replayTextSelector)
          textOnPageCheck(expectedTotalAmountParagraph, totalAmountTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, newAmount.toString())
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(ProfFeesAndSubscriptionsExpensesAmountController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "render the professional fees and subscriptions expenses amount page with pre-filled data from prior submission" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "400")), Some(400))

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
          textOnPageCheck(expectedTotalAmountParagraph, totalAmountTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, amount.toString())
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(ProfFeesAndSubscriptionsExpensesAmountController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "return an error when a form is submitted with no entry" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), Some(400))

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
          textOnPageCheck(expectedTotalAmountParagraph, totalAmountTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, "")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(ProfFeesAndSubscriptionsExpensesAmountController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, expectedErrorHref)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, Some(amountFieldName))
        }

        "return an error when a form is submitted with an incorrect format" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "abc")), Some(400))

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
          textOnPageCheck(expectedTotalAmountParagraph, totalAmountTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, "abc")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(ProfFeesAndSubscriptionsExpensesAmountController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, expectedErrorHref)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountFieldName))
        }

        "return an error when a form is submitted with an amount over the maximum limit" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), Some(400))

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
          textOnPageCheck(expectedTotalAmountParagraph, totalAmountTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, maxLimit)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(ProfFeesAndSubscriptionsExpensesAmountController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, expectedErrorHref)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, Some(amountFieldName))
        }
      }
    }
  }
}
