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

package views.benefits.assets

import controllers.benefits.assets.routes.AssetsTransfersBenefitsAmountController
import forms.AmountForm
import forms.benefits.assets.AssetsFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.assets.AssetsTransfersBenefitsAmountView

class AssetsTransfersBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"
  private val amount: BigDecimal = 200
  private val amountFieldName = "amount"
  private val expectedErrorHref = "#amount"

  object Selectors {
    val hintTextSelector = "#amount-hint"
    val currencyPrefixSelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val amountFieldSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
    val youCanSelector = "#you-can-text"
    val enterTotalSelector = "#enter-total-text"
    val previousAmountSelector = "#previous-amount-text"
  }

  trait CommonExpectedResults {
    val expectedCaption: String

    val expectedHintText: String
    val currencyPrefix: String
    val continueButtonText: String
    val enterTotalText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedYouCanText: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val expectedErrorIncorrectFormat: String
    val expectedErrorOverMaximum: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"

    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
    val enterTotalText = "Enter the total."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"

    val expectedHintText = "Er enghraifft, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Yn eich blaen"
    val enterTotalText = "Nodwch y cyfanswm."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much were the assets your employer gave you to keep?"
    val expectedHeading = "How much were the assets your employer gave you to keep?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedYouCanText: String = "You can find this information on your P11D form in section A, box 13."
    val expectedErrorNoEntry = "Enter the amount for assets your employer gave you to keep"
    val expectedErrorIncorrectFormat = "Enter the amount for assets your employer gave you to keep in the correct format"
    val expectedErrorOverMaximum = "The total amount for assets your employer gave you to keep must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Beth oedd gwerth yr asedion a roddodd eich cyflogwr i chi eu cadw?"
    val expectedHeading = "Beth oedd gwerth yr asedion a roddodd eich cyflogwr i chi eu cadw?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedYouCanText: String = "Maeír wybodaeth hon ar gael yn adran A, blwch 13 ar eich ffurflen P11D."
    val expectedErrorNoEntry = "Nodwch swm yr asedion a roddodd eich cyflogwr i chi eu cadw"
    val expectedErrorIncorrectFormat = "Nodwch swm yr asedion a roddodd eich cyflogwr i chi eu cadw yn y fformat cywir"
    val expectedErrorOverMaximum = "Maeín rhaid i gyfanswm yr asedion a roddodd eich cyflogwr i chi eu cadw fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much were the assets your client’s employer gave them to keep?"
    val expectedHeading = "How much were the assets your client’s employer gave them to keep?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedYouCanText: String = "You can find this information on your client’s P11D form in section A, box 13."
    val expectedErrorNoEntry = "Enter the amount for assets your client’s employer gave them to keep"
    val expectedErrorIncorrectFormat = "Enter the amount for assets your client’s employer gave them to keep in the correct format"
    val expectedErrorOverMaximum = "The total amount for assets your client’s employer gave them to keep must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Beth oedd gwerth yr asedion a roddodd cyflogwr eich cleient iddo eu cadw?"
    val expectedHeading = "Beth oedd gwerth yr asedion a roddodd cyflogwr eich cleient iddo eu cadw?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedYouCanText: String = "Maeír wybodaeth hon ar gael yn adran A, blwch 13 ar ffurflen P11D eich cleient."
    val expectedErrorNoEntry = "Nodwch swm yr asedion a roddodd cyflogwr eich cleient iddo eu cadw"
    val expectedErrorIncorrectFormat = "Nodwch swm yr asedion a roddodd cyflogwr eich cleient iddo eu cadw yn y fformat cywir"
    val expectedErrorOverMaximum = "Maeín rhaid i gyfanswm yr asedion a roddodd eich cyflogwr iddo eu cadw fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new AssetsFormsProvider().assetTransfersAmountForm(isAgent)

  private lazy val underTest = inject[AssetsTransfersBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the assets amount page with no prefilled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        elementNotOnPageCheck(previousAmountSelector)
        textOnPageCheck(enterTotalText, enterTotalSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanText, youCanSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountFieldName, amountFieldSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(AssetsTransfersBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with a pre-filled amount field" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).fill(value = 200), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(enterTotalText, enterTotalSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanText, youCanSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountFieldName, amountFieldSelector, amount.toString())
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(AssetsTransfersBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render the assets amount page with an error" when {
        "a form is submitted with no entry" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(enterTotalText, enterTotalSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanText, youCanSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, "")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(AssetsTransfersBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, expectedErrorHref)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, Some(amountFieldName))

          welshToggleCheck(userScenario.isWelsh)
        }

        "a form is submitted with an incorrectly formatted amount" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(enterTotalText, enterTotalSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanText, youCanSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, value = "123.33.33")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(AssetsTransfersBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, expectedErrorHref)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountFieldName))

          welshToggleCheck(userScenario.isWelsh)
        }

        "a form is submitted and the amount is over the maximum limit" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(enterTotalText, enterTotalSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedYouCanText, youCanSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, value = "100,000,000,000")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(AssetsTransfersBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, expectedErrorHref)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, Some(amountFieldName))

          welshToggleCheck(userScenario.isWelsh)
        }
      }
    }
  }
}
