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

package views.benefits.accommodation

import controllers.benefits.accommodation.routes.NonQualifyingRelocationBenefitsAmountController
import forms.AmountForm
import forms.benefits.accommodation.AccommodationFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.accommodation.NonQualifyingRelocationBenefitsAmountView

class NonQualifyingRelocationBenefitsAmountViewSpec extends ViewUnitTest {

  private val poundPrefixText = "£"
  private val amountInputName = "amount"
  private val employmentId = "employmentId"

  object Selectors {
    val contentSelector = "#main-content > div > div > p"
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
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "Er enghraifft, £193.52"
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val continue = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for non-qualifying relocation benefits?"
    val expectedHeading: String = "How much did you get in total for non-qualifying relocation benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter your non-qualifying relocation benefit amount"
    val invalidFormatErrorText: String = "Enter your non-qualifying relocation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your non-qualifying relocation benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y cawsoch i gyd ar gyfer buddiant adleoli anghymwys?"
    val expectedHeading: String = "Faint y cawsoch i gyd ar gyfer buddiant adleoli anghymwys?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch swm eich buddiant adleoli anghymwys"
    val invalidFormatErrorText: String = "Nodwch swm eich buddiant adleoli anghymwys yn y fformat cywir"
    val maxAmountErrorText: String = "Mae’n rhaid i’ch buddiant adleoli anghymwys fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for non-qualifying relocation benefits?"
    val expectedHeading: String = "How much did your client get in total for non-qualifying relocation benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter your client’s non-qualifying relocation benefit amount"
    val invalidFormatErrorText: String = "Enter your client’s non-qualifying relocation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your client’s non-qualifying relocation benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y cawsoch i gyd ar gyfer buddiannau adleoli anghymwys?"
    val expectedHeading: String = "Faint y cawsoch i gyd ar gyfer buddiannau adleoli anghymwys?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch swm buddiant adleoli anghymwys eich cleient"
    val invalidFormatErrorText: String = "Nodwch swm buddiant adleoli anghymwys eich cleient yn y fformat cywir"
    val maxAmountErrorText: String = "Mae’n rhaid i fuddiant adleoli anghymwys eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new AccommodationFormsProvider().nonQualifyingRelocationAmountForm(isAgent)

  private lazy val underTest = inject[NonQualifyingRelocationBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    import userScenario.specificExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the non-qualifying relocation benefits amount page without pre-filled form and without replay text" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        elementNotOnPageCheck(contentSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")

        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(NonQualifyingRelocationBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the non-qualifying relocation benefits amount page with pre-filled amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).fill(value = 300), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "300")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(NonQualifyingRelocationBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "should render non qualifying relocation question amount page with empty error text when there no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(NonQualifyingRelocationBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(get.emptyErrorText)
      }

      "should render the non qualifying relocation question amount page with invalid format text when input is in incorrect format" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(NonQualifyingRelocationBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(get.invalidFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(get.invalidFormatErrorText)
      }

      "should render the non qualifying relocation question page with max error when input > 100,000,000,000" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "9999999999999999999999999999")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "9999999999999999999999999999")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(NonQualifyingRelocationBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(get.maxAmountErrorText)
      }
    }
  }
}
