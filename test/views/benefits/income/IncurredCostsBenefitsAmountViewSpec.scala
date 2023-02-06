/*
 * Copyright 2023 HM Revenue & Customs
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

package views.benefits.income

import controllers.benefits.income.routes.IncurredCostsBenefitsAmountController
import forms.AmountForm
import forms.benefits.income.IncomeFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.income.IncurredCostsBenefitsAmountView

class IncurredCostsBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"
  private val amount: BigDecimal = 255
  private val amountInputName = "amount"
  private val expectedErrorHref = "#amount"

  object Selectors {
    val hintTextSelector = "#amount-hint"
    val currencyPrefixSelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val inputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
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
    val expectedTitle = "How much of your incurred costs did your employer pay?"
    val expectedHeading = "How much of your incurred costs did your employer pay?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of incurred costs paid by your employer"
    val expectedErrorIncorrectFormat = "Enter the amount of incurred costs paid by your employer in the correct format"
    val expectedErrorOverMaximum = "The incurred costs paid by your employer must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint o’r costau yr ysgwyddwyd arnoch a dalodd eich cyflogwr?"
    val expectedHeading = "Faint o’r costau yr ysgwyddwyd arnoch a dalodd eich cyflogwr?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch swm y costau a ysgwyddwyd ac a dalwyd gan eich cyflogwr"
    val expectedErrorIncorrectFormat = "Nodwch swm y costau a ysgwyddwyd ac a dalwyd gan eich cyflogwr yn y fformat cywir"
    val expectedErrorOverMaximum = "Mae’n rhaid i’r costau a ysgwyddwyd ac a dalwyd gan eich cyflogwr fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much of your client’s incurred costs did their employer pay?"
    val expectedHeading = "How much of your client’s incurred costs did their employer pay?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of incurred costs paid by your client’s employer"
    val expectedErrorIncorrectFormat = "Enter the amount of incurred costs paid by your client’s employer in the correct format"
    val expectedErrorOverMaximum = "The incurred costs paid by your client’s employer must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint o’r costau yr ysgwyddwyd ar eich cleient a dalodd ei gyflogwr?"
    val expectedHeading = "Faint o’r costau yr ysgwyddwyd ar eich cleient a dalodd ei gyflogwr?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch swm y costau a ysgwyddwyd ac a dalwyd gan gyflogwr eich cleient"
    val expectedErrorIncorrectFormat = "Nodwch swm y costau a ysgwyddwyd ac a dalwyd gan gyflogwr eich cleient yn y fformat cywir"
    val expectedErrorOverMaximum = "Mae’n rhaid i’r costau a ysgwyddwyd ac a dalwyd gan gyflogwr eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new IncomeFormsProvider().incurredCostsAmountForm(isAgent)

  private lazy val underTest = inject[IncurredCostsBenefitsAmountView]

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
        textOnPageCheck(enterTotalText, paragraphSelector(1))
        textOnPageCheck(expectedHintText, hintTextSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(IncurredCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "255")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(enterTotalText, paragraphSelector(index = 1))
        textOnPageCheck(expectedHintText, hintTextSelector)
        inputFieldValueCheck(amountInputName, inputSelector, amount.toString())
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(IncurredCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with an error when theres no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(enterTotalText, paragraphSelector(1))
        hintTextCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, Some(amountInputName))
        formPostLinkCheck(IncurredCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

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
        textOnPageCheck(enterTotalText, paragraphSelector(index = 1))
        hintTextCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")
        buttonCheck(continueButtonText, continueButtonSelector)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountInputName))
        formPostLinkCheck(IncurredCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

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
        textOnPageCheck(enterTotalText, paragraphSelector(index = 1))
        hintTextCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "100,000,000,000")
        buttonCheck(continueButtonText, continueButtonSelector)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, Some(amountInputName))
        formPostLinkCheck(IncurredCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
