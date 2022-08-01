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

import controllers.benefits.medical.routes.ChildcareBenefitsAmountController
import forms.AmountForm
import forms.benefits.medical.MedicalFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.medical.ChildcareBenefitsAmountView

class ChildcareBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"
  private val amount: BigDecimal = 200
  private val amountInputName = "amount"
  private val expectedErrorHref = "#amount"

  object Selectors {
    val optionalParagraphTextSelector = "#main-content > div > div > p"
    val hintTextSelector = "#amount-hint"
    val currencyPrefixSelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val inputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String

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
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"

    val expectedHintText = "Er enghraifft, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your total childcare benefit?"
    val expectedHeading = "How much was your total childcare benefit?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter your childcare benefit amount"
    val expectedErrorIncorrectFormat = "Enter your childcare benefit amount in the correct format"
    val expectedErrorOverMaximum = "Your childcare benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint oedd cyfanswm eich buddiant gofal plant?"
    val expectedHeading = "Faint oedd cyfanswm eich buddiant gofal plant?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch swm eich buddiant gofal plant"
    val expectedErrorIncorrectFormat = "Nodwch swm eich buddiant gofal plant yn y fformat cywir"
    val expectedErrorOverMaximum = "Maeín rhaid iích buddiant gofal plant fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s total childcare benefit?"
    val expectedHeading = "How much was your client’s total childcare benefit?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter your client’s childcare benefit amount"
    val expectedErrorIncorrectFormat = "Enter your client’s childcare benefit amount in the correct format"
    val expectedErrorOverMaximum = "Your client’s childcare benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint oedd cyfanswm buddiant gofal plant eich cleient?"
    val expectedHeading = "Faint oedd cyfanswm buddiant gofal plant eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch swm buddiant gofal plant eich cleient"
    val expectedErrorIncorrectFormat = "Nodwch fuddiant gofal plant eich cleient yn y fformat cywir"
    val expectedErrorOverMaximum = "Maeín rhaid i fuddiant gofal plant eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new MedicalFormsProvider().childcareAmountForm(isAgent)

  private lazy val underTest = inject[ChildcareBenefitsAmountView]

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
        captionCheck(expectedCaption(taxYearEOY))
        elementNotOnPageCheck(optionalParagraphTextSelector)
        textOnPageCheck(expectedHintText, hintTextSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(ChildcareBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there is previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> amount.toString)), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(expectedHintText, hintTextSelector)
        inputFieldValueCheck(amountInputName, inputSelector, amount.toString())
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(ChildcareBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with an error when theres no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        hintTextCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        buttonCheck(continueButtonText, continueButtonSelector)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, Some(amountInputName))
        formPostLinkCheck(ChildcareBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted with incorrectly formatted amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        hintTextCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")
        buttonCheck(continueButtonText, continueButtonSelector)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountInputName))
        formPostLinkCheck(ChildcareBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted and the amount is over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        hintTextCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "100,000,000,000")
        buttonCheck(continueButtonText, continueButtonSelector)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, Some(amountInputName))
        formPostLinkCheck(ChildcareBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
