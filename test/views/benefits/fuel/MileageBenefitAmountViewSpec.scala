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

import controllers.benefits.fuel.routes.MileageBenefitAmountController
import forms.AmountForm
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.fuel.MileageBenefitAmountView

class MileageBenefitAmountViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  object Selectors {
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
    val contentSelector = "#main-content > div > div > p"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val continueButtonText: String
    val hintText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraph: String
    val expectedParagraphWithPrefill: String
    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedWrongFormatErrorMessage: String
    val expectedMaxErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val hintText = "For example, £193.52"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"

    val hintText = "Er enghraifft, £193.52"
    val continueButtonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much mileage benefit did you get in total for using your own car?"
    val expectedHeading = "How much mileage benefit did you get in total for using your own car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of mileage benefit you got for using your own car"
    val expectedParagraph: String = "You can find this information on your P11D form in section E, box 12."
    val expectedParagraphWithPrefill: String = "You can find this information on your P11D form in section E, box 12."
    val expectedWrongFormatErrorMessage: String = "Enter the amount of mileage benefit you got in the correct format"
    val expectedMaxErrorMessage: String = "Your mileage benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint o fuddiant milltiroedd a gawsoch i gyd am ddefnyddio eich car eich hun?"
    val expectedHeading = "Faint o fuddiant milltiroedd a gawsoch i gyd am ddefnyddio eich car eich hun?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch swm y buddiant milltiroedd a gawsoch am ddefnyddio eich car eich hun"
    val expectedParagraph: String = "Maeír wybodaeth hon ar gael yn adran E, blwch 12 ar eich ffurflen P11D."
    val expectedParagraphWithPrefill: String = "Maeír wybodaeth hon ar gael yn adran E, blwch 12 ar eich ffurflen P11D."
    val expectedWrongFormatErrorMessage: String = "Nodwch swm y buddiant milltiroedd a gawsoch chi yn y fformat cywir"
    val expectedMaxErrorMessage: String = "Maeín rhaid iích buddiant milltiroedd fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much mileage benefit did your client get in total for using their own car?"
    val expectedHeading = "How much mileage benefit did your client get in total for using their own car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of mileage benefit your client got for using their own car"
    val expectedParagraph: String = "You can find this information on your client’s P11D form in section E, box 12."
    val expectedParagraphWithPrefill: String = "You can find this information on your client’s P11D form in section E, box 12."
    val expectedWrongFormatErrorMessage: String = "Enter the amount of mileage benefit your client got in the correct format"
    val expectedMaxErrorMessage: String = "Your client’s mileage benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint o fuddiant milltiroedd a gafodd eich cleient i gyd am ddefnyddio ei gar ei hun?"
    val expectedHeading = "Faint o fuddiant milltiroedd a gafodd eich cleient i gyd am ddefnyddio ei gar ei hun?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch swm y buddiant milltiroedd a gafodd eich cleient am ddefnyddio ei gar ei hun"
    val expectedParagraph: String = "Maeír wybodaeth hon ar gael yn adran E, blwch 12 ar ffurflen P11D eich cleient."
    val expectedParagraphWithPrefill: String = "Maeír wybodaeth hon ar gael yn adran E, blwch 12 ar ffurflen P11D eich cleient."
    val expectedWrongFormatErrorMessage: String = "Nodwch swm y buddiant milltiroedd a gafodd eich cleient yn y fformat cywir"
    val expectedMaxErrorMessage: String = "Maeín rhaid i fuddiant milltiroedd eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new FuelFormsProvider().mileageAmountForm(isAgent)

  private lazy val underTest = inject[MileageBenefitAmountView]

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
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, contentSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, Selectors.inputSelector, value = "")
        formPostLinkCheck(MileageBenefitAmountController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "500")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphWithPrefill, contentSelector)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, Selectors.inputSelector, value = "500")
        formPostLinkCheck(MileageBenefitAmountController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with an error when theres no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, Selectors.inputSelector)
        inputFieldValueCheck(amountInputName, Selectors.inputSelector, value = "")
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted with incorrectly formatted amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedWrongFormatErrorMessage, Selectors.inputSelector)
        inputFieldValueCheck(amountInputName, Selectors.inputSelector, value = "123.33.33")
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedWrongFormatErrorMessage)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted and the amount is over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedMaxErrorMessage, Selectors.inputSelector)
        inputFieldValueCheck(amountInputName, Selectors.inputSelector, value = "100,000,000,000")
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedMaxErrorMessage)

        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
