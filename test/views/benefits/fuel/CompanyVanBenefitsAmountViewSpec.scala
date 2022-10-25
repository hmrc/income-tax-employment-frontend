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

package views.benefits.fuel

import controllers.benefits.fuel.routes.CompanyVanBenefitsAmountController
import forms.AmountForm
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.fuel.CompanyVanBenefitsAmountView

class CompanyVanBenefitsAmountViewSpec extends ViewUnitTest {

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
    val expectedContent: String
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
    val expectedTitle: String = "How much was your total company van benefit?"
    val expectedHeading: String = "How much was your total company van benefit?"
    val expectedContent: String = "You can find this information on your P11D form in section G, box 9."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter your company van benefit amount in the correct format"
    val emptyErrorText: String = "Enter your company van benefit amount"
    val maxAmountErrorText: String = "Your company van benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint oedd cyfanswm eich buddiant fan cwmni?"
    val expectedHeading: String = "Faint oedd cyfanswm eich buddiant fan cwmni?"
    val expectedContent: String = "Mae’r wybodaeth hon ar gael yn adran G, blwch 9 ar eich ffurflen P11D."
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val wrongFormatErrorText: String = "Nodwch swm eich buddiant fan cwmni yn y fformat cywir"
    val emptyErrorText: String = "Nodwch swm eich buddiant fan cwmni"
    val maxAmountErrorText: String = "Mae’n rhaid i’ch buddiant fan cwmni’ch cleient fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much was your client’s total company van benefit?"
    val expectedHeading: String = "How much was your client’s total company van benefit?"
    val expectedContent: String = "You can find this information on your client’s P11D form in section G, box 9."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter your client’s company van benefit amount in the correct format"
    val emptyErrorText: String = "Enter your client’s company van benefit amount"
    val maxAmountErrorText: String = "Your client’s company van benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint oedd cyfanswm buddiant fan cwmni eich cleient?"
    val expectedHeading: String = "Faint oedd cyfanswm buddiant fan cwmni eich cleient?"
    val expectedContent: String = "Mae’r wybodaeth hon ar gael yn adran G, blwch 9 ar ffurflen P11D eich cleient."
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val wrongFormatErrorText: String = "Nodwch swm buddiant fan cwmni’ch cleient yn y fformat cywir"
    val emptyErrorText: String = "Nodwch swm buddiant fan cwmni eich cleient"
    val maxAmountErrorText: String = "Mae’n rhaid i fuddiant fan cwmni’ch cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new FuelFormsProvider().companyVanAmountForm(isAgent)

  private lazy val underTest = inject[CompanyVanBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    import userScenario.specificExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render amount page with not prefilled form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContent, contentSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(CompanyVanBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "300")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContent, contentSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "300")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(CompanyVanBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with an error when theres no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")),  employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted with incorrectly formatted amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")
        errorSummaryCheck(get.wrongFormatErrorText, expectedErrorHref)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted and the amount is over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        inputFieldValueCheck(amountInputName, inputSelector, "100,000,000,000")
        errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
