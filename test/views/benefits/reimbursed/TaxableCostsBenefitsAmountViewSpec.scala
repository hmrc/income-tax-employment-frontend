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

import controllers.benefits.reimbursed.routes.TaxableCostsBenefitsAmountController
import forms.AmountForm
import forms.benefits.reimbursed.ReimbursedFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.reimbursed.TaxableCostsBenefitsAmountView

class TaxableCostsBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  object Selectors {
    val enterTotalTextSelector = "#enter-total-text"
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
    val enterTotalText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val amountHint: String = "For example, £193.52"
    val expectedCaption: String = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue: String = "Continue"
    val enterTotalText: String = "Enter the total."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "Er enghraifft, £193.52"
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val continue = "Yn eich blaen"
    val enterTotalText: String = "Nodwch y cyfanswm."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much of your taxable costs were reimbursed by your employer?"
    val expectedHeading: String = "How much of your taxable costs were reimbursed by your employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount of taxable costs reimbursed by your employer"
    val invalidFormatErrorText: String = "Enter the amount of taxable costs reimbursed by your employer in the correct format"
    val maxAmountErrorText: String = "The taxable costs reimbursed by your employer must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint o’ch costau trethadwy a gafodd eu had-dalu gan eich cyflogwr?"
    val expectedHeading: String = "Faint o’ch costau trethadwy a gafodd eu had-dalu gan eich cyflogwr?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch swm y costau trethadwy a ad-dalwyd gan eich cyflogwr"
    val invalidFormatErrorText: String = "Nodwch swm y costau trethadwy a ad-dalwyd gan eich cyflogwr yn y fformat cywir"
    val maxAmountErrorText: String = "Mae’n rhaid i’r costau trethadwy a ad-dalwyd gan eich cyflogwr fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much of your client’s taxable costs were reimbursed by their employer?"
    val expectedHeading: String = "How much of your client’s taxable costs were reimbursed by their employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount of taxable costs reimbursed by your client’s employer"
    val invalidFormatErrorText: String = "Enter the amount of taxable costs reimbursed by your client’s employer in the correct format"
    val maxAmountErrorText: String = "The taxable costs reimbursed by your client’s employer must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint o gostau trethadwy eich cleient a gafodd eu had-dalu gan ei gyflogwr?"
    val expectedHeading: String = "Faint o gostau trethadwy eich cleient a gafodd eu had-dalu gan ei gyflogwr?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch swm y costau trethadwy a ad-dalwyd gan gyflogwr eich cleient"
    val invalidFormatErrorText: String = "Nodwch swm y costau trethadwy a ad-dalwyd gan gyflogwr eich cleient yn y fformat cywir"
    val maxAmountErrorText: String = "Mae’n rhaid i’r costau trethadwy a ad-dalwyd gan gyflogwr eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new ReimbursedFormsProvider().taxableCostsAmountForm(isAgent)

  private lazy val underTest = inject[TaxableCostsBenefitsAmountView]

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
        elementNotOnPageCheck(previousAmountTextSelector)
        textOnPageCheck(enterTotalText, enterTotalTextSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(TaxableCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there is previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "200")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(enterTotalText, enterTotalTextSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "200")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(TaxableCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
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
        textOnPageCheck(enterTotalText, enterTotalTextSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(TaxableCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyErrorText)
      }

      "should render the amount page with invalid format text when input is in incorrect format" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(enterTotalText, enterTotalTextSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "123.33.33")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(TaxableCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
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
        textOnPageCheck(enterTotalText, enterTotalTextSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "100,000,000,000")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(TaxableCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.maxAmountErrorText)
      }
    }
  }
}
