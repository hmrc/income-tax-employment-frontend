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

package views.benefits.utilities

import forms.AmountForm
import forms.benefits.utilities.UtilitiesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.utilities.ProfessionalSubscriptionsBenefitsAmountView

class ProfessionalSubscriptionsBenefitsAmountViewSpec extends ViewUnitTest {

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
    val wrongFormatErrorText: String
    val emptyErrorText: String
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
    val expectedTitle: String = "How much did your employer pay to cover professional fees or subscriptions?"
    val expectedHeading: String = "How much did your employer pay to cover professional fees or subscriptions?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter the amount your employer paid to cover professional fees or subscriptions in the correct format"
    val emptyErrorText: String = "Enter the amount your employer paid to cover professional fees or subscriptions"
    val maxAmountErrorText: String = "The amount your employer paid to cover professional fees or subscriptions must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y gwnaeth eich cyflogwr ei dalu am danysgrifiadau neu ffioedd proffesiynol?"
    val expectedHeading: String = "Faint y gwnaeth eich cyflogwr ei dalu am danysgrifiadau neu ffioedd proffesiynol?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val wrongFormatErrorText: String = "Nodwch y swm a dalwyd gan eich cyflogwr ar gyfer ffioedd neu danysgrifiadau proffesiynol yn y fformat cywir"
    val emptyErrorText: String = "Nodwch y swm a dalwyd gan eich cyflogwr ar gyfer ffioedd neu danysgrifiadau proffesiynol"
    val maxAmountErrorText: String = "Mae’n rhaid i’r swm a dalwyd gan eich cyflogwr ar gyfer ffioedd neu danysgrifiadau proffesiynol fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client’s employer pay to cover professional fees or subscriptions?"
    val expectedHeading: String = "How much did your client’s employer pay to cover professional fees or subscriptions?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter the amount your client’s employer paid to cover professional fees or subscriptions in the correct format"
    val emptyErrorText: String = "Enter the amount your client’s employer paid to cover professional fees or subscriptions"
    val maxAmountErrorText: String = "The amount your client’s employer paid to cover professional fees or subscriptions must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y gwnaeth cyflogwr eich cleient ei dalu am ffioedd neu danysgrifiadau proffesiynol?"
    val expectedHeading: String = "Faint y gwnaeth cyflogwr eich cleient ei dalu am ffioedd neu danysgrifiadau proffesiynol?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val wrongFormatErrorText: String = "Nodwch y swm a dalwyd gan gyflogwr eich cleient ar gyfer ffioedd neu danysgrifiadau proffesiynol yn y fformat cywir"
    val emptyErrorText: String = "Nodwch y swm a dalwyd gan gyflogwr eich cleient ar gyfer ffioedd neu danysgrifiadau proffesiynol"
    val maxAmountErrorText: String = "Mae’n rhaid i’r swm a dalodd cyflogwr eich cleient ar gyfer ffioedd neu danysgrifiadau proffesiynol fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new UtilitiesFormsProvider().professionalSubscriptionsBenefitsAmountForm(isAgent)

  private lazy val underTest = inject[ProfessionalSubscriptionsBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the professional subscriptions benefits amount page with no pre-filled form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck("", contentSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.utilities.routes.ProfessionalSubscriptionsBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the professional subscriptions benefits amount page with pre-filled form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).fill(300), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "300")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.utilities.routes.ProfessionalSubscriptionsBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the professional subscriptions benefits amount page with empty input error when no input is submitted" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.utilities.routes.ProfessionalSubscriptionsBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyErrorText)
      }

      "render the professional subscriptions benefits amount page with wrong format error when input is in incorrect format" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "abc")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "abc")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.utilities.routes.ProfessionalSubscriptionsBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.wrongFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.wrongFormatErrorText)
      }

      "render the professional subscriptions benefits amount page with max error when input > 100,000,000,000" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "9999999999999999999999999999")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "9999999999999999999999999999")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.utilities.routes.ProfessionalSubscriptionsBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.maxAmountErrorText)
      }
    }
  }
}


