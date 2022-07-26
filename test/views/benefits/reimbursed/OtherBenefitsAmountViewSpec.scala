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

import controllers.benefits.reimbursed.routes.OtherBenefitsAmountController
import forms.AmountForm
import forms.benefits.reimbursed.ReimbursedFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.reimbursed.OtherBenefitsAmountView

class OtherBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  object Selectors {
    val previousAmountTextSelector = "#previous-amount-text"
    val hintTextSelector = "#amount-hint"
    val inputSelector = "#amount"
    val poundPrefixSelector = ".govuk-input__prefix"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#amount"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val amountHint: String
    val continue: String
    val maxAmountErrorText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val amountHint: String = "For example, £193.52"
    val expectedCaption: String = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue: String = "Continue"
    val maxAmountErrorText: String = "The amount for other benefits must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "Er enghraifft, £193.52"
    val expectedCaption: String = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val continue: String = "Yn eich blaen"
    val maxAmountErrorText: String = "Maeín rhaid iír swm a nodwyd ar gyfer buddiannau eraill fod yn llai na £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for other benefits?"
    val expectedHeading: String = "How much did you get in total for other benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount you got for other benefits"
    val invalidFormatErrorText: String = "Enter the amount you got for other benefits in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y cawsoch i gyd ar gyfer buddiannau eraill?"
    val expectedHeading: String = "Faint y cawsoch i gyd ar gyfer buddiannau eraill?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch y swm a gawsoch ar gyfer buddiannau eraill"
    val invalidFormatErrorText: String = "Nodwch y swm a gawsoch ar gyfer buddiannau eraill yn y fformat cywir"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for other benefits?"
    val expectedHeading: String = "How much did your client get in total for other benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount your client got for other benefits"
    val invalidFormatErrorText: String = "Enter the amount your client got for other benefits in the correct format"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y cafodd eich cleient i gyd ar gyfer buddiannau eraill?"
    val expectedHeading: String = "Faint y cafodd eich cleient i gyd ar gyfer buddiannau eraill?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch y swm a gafodd eich cleient ar gyfer buddiannau eraill"
    val invalidFormatErrorText: String = "Nodwch y swm a gafodd eich cleient ar gyfer buddiannau eraill yn y fformat cywir"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new ReimbursedFormsProvider().otherBenefitsAmountForm(isAgent)

  private lazy val underTest = inject[OtherBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the other benefits amount page without pre-filled form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        elementNotOnPageCheck(previousAmountTextSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(OtherBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there is previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "500")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "500")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(OtherBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
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
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(OtherBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyErrorText)
      }

      "render page with error when a form is submitted with incorrectly formatted amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "123.33.33")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(OtherBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.invalidFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidFormatErrorText)
      }

      "render page with error when a form is submitted and the amount is over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "100,000,000,000")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(OtherBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(maxAmountErrorText)
      }
    }
  }
}
