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

package views.benefits.medical

import controllers.benefits.medical.routes.MedicalOrDentalBenefitsAmountController
import forms.AmountForm
import forms.benefits.medical.MedicalFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.medical.MedicalOrDentalBenefitsAmountView

class MedicalOrDentalBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"
  private val poundPrefixText = "£"
  private val amountField = "#amount"
  private val amountInputName = "amount"

  object Selectors {
    def paragraphTextSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"

    val hintTextSelector = "#amount-hint"
    val inputSelector = "#amount"
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
    val poundPrefixSelector = ".govuk-input__prefix"
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
    val expectedParagraphForForm: String
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
    val expectedTitle = "How much was your medical or dental benefit in total?"
    val expectedHeading = "How much was your medical or dental benefit in total?"
    val expectedParagraph = "This is the total sum of medical or dental insurance your employer paid for."
    val expectedParagraphForForm = "You can find this information on your P11D form in section I, box 11."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your medical or dental benefit amount"
    val expectedWrongFormatErrorMessage = "Enter your medical or dental benefit amount in the correct format"
    val expectedMaxErrorMessage = "Your medical or dental benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint oedd cyfanswm eich buddiant meddygol neu ddeintyddol?"
    val expectedHeading = "Faint oedd cyfanswm eich buddiant meddygol neu ddeintyddol?"
    val expectedParagraph = "Dyma gyfanswm yr yswiriant meddygol neu ddeintyddol y talodd eich cyflogwr amdano."
    val expectedParagraphForForm = "Mae’r wybodaeth hon ar gael yn adran I, blwch 11 ar eich ffurflen P11D."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch swm y buddiant meddygol neu ddeintyddol a gawsoch"
    val expectedWrongFormatErrorMessage = "Nodwch swm y buddiant meddygol neu ddeintyddol a gawsoch yn y fformat cywir"
    val expectedMaxErrorMessage = "Mae’n rhaid i’ch buddiant meddygol neu ddeintyddol fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s medical or dental benefit in total?"
    val expectedHeading = "How much was your client’s medical or dental benefit in total?"
    val expectedParagraph = "This is the total sum of medical or dental insurance your client’s employer paid for."
    val expectedParagraphForForm = "You can find this information on your client’s P11D form in section I, box 11."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your client’s medical or dental benefit amount"
    val expectedWrongFormatErrorMessage = "Enter your client’s medical or dental benefit amount in the correct format"
    val expectedMaxErrorMessage = "Your client’s medical or dental benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint oedd cyfanswm buddiant meddygol neu ddeintyddol eich cleient?"
    val expectedHeading = "Faint oedd cyfanswm buddiant meddygol neu ddeintyddol eich cleient?"
    val expectedParagraph = "Dyma gyfanswm yr yswiriant meddygol neu ddeintyddol y talodd cyflogwr eich cleient amdano."
    val expectedParagraphForForm = "Mae’r wybodaeth hon ar gael yn adran I, blwch 11 ar ffurflen P11D eich cleient."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch swm y buddiant meddygol neu ddeintyddol a gafodd eich cleient"
    val expectedWrongFormatErrorMessage = "Nodwch swm y buddiant meddygol neu ddeintyddol a gafodd eich cleient yn y fformat cywir"
    val expectedMaxErrorMessage = "Mae’n rhaid i fuddiant meddygol neu ddeintyddol fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new MedicalFormsProvider().medicalOrAmountForm(isAgent)

  private lazy val underTest = inject[MedicalOrDentalBenefitsAmountView]

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
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        formPostLinkCheck(MedicalOrDentalBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there is previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "100")
        formPostLinkCheck(MedicalOrDentalBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "should render the amount page with empty value error text when there is no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(2))
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, Some(amountInputName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, amountField)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        formPostLinkCheck(MedicalOrDentalBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)
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
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(index = 1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(index = 2))
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedWrongFormatErrorMessage, Some(amountInputName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedWrongFormatErrorMessage, amountField)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")
        formPostLinkCheck(MedicalOrDentalBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)
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
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(2))
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedMaxErrorMessage, Some(amountInputName))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedMaxErrorMessage, amountField)
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "100,000,000,000")
        formPostLinkCheck(MedicalOrDentalBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
