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

package views.expenses

import controllers.expenses.routes.OtherEquipmentAmountController
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.OtherEquipmentAmountView

class OtherEquipmentAmountViewSpec extends ViewUnitTest {

  private val poundPrefixText = "£"
  private val newAmount: BigDecimal = 250
  private val amountField = "#amount"
  private val amountFieldName = "amount"

  object Selectors {
    val formSelector = "#main-content > div > div > form"
    val wantToClaimSelector: String = "#previous-amount"
    val totalAmountParagraphSelector: String = "#total-amount-paragraph"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val continueButtonSelector: String = "#continue"
  }

  trait SpecificExpectedResults {

    val expectedTitle: String
    val expectedHeading: String

    def expectedPreAmountParagraph(amount: BigDecimal): String

    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedInvalidFormatErrorMessage: String
    val expectedOverMaximumErrorMessage: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val totalAmountParagraph: String
    val hintText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for buying other equipment?"
    val expectedHeading = "How much do you want to claim for buying other equipment?"

    def expectedPreAmountParagraph(amount: BigDecimal): String = s"You told us you want to claim £$amount for buying other equipment. Tell us if this has changed."

    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you want to claim for buying other equipment"
    val expectedInvalidFormatErrorMessage = "Enter the amount you want to claim for buying other equipment in the correct format"
    val expectedOverMaximumErrorMessage = "The amount you want to claim for buying other equipment must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint rydych am ei hawlio ar gyfer prynu offer eraill?"
    val expectedHeading = "Faint rydych am ei hawlio ar gyfer prynu offer eraill?"

    def expectedPreAmountParagraph(amount: BigDecimal): String = s"Dywedoch wrthym eich bod am hawlio £$amount ar gyfer prynu offer eraill. Rhowch wybod i ni os yw hyn wedi newid."

    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch y swm rydych am ei hawlio ar gyfer prynu offer eraill"
    val expectedInvalidFormatErrorMessage = "Nodwch y swm rydych am ei hawlio ar gyfer prynu offer eraill yn y fformat cywir"
    val expectedOverMaximumErrorMessage = "Maeín rhaid iír swm rydych am ei hawlio ar gyfer prynu offer eraill fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for buying other equipment for your client?"
    val expectedHeading = "How much do you want to claim for buying other equipment for your client?"

    def expectedPreAmountParagraph(amount: BigDecimal): String = s"You told us you want to claim £$amount for buying other equipment for your client. Tell us if this has changed."

    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you want to claim for your client buying other equipment"
    val expectedInvalidFormatErrorMessage = "Enter the amount you want to claim for your client buying other equipment in the correct format"
    val expectedOverMaximumErrorMessage = "The amount you want to claim for your client buying other equipment must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint rydych am ei hawlio ar gyfer prynu offer eraill ar gyfer eich cleient?"
    val expectedHeading = "Faint rydych am ei hawlio ar gyfer prynu offer eraill ar gyfer eich cleient?"

    def expectedPreAmountParagraph(amount: BigDecimal): String = s"Dywedoch wrthym eich bod am hawlio £$amount ar gyfer prynu " +
      "offer eraill ar gyfer eich cleient. Rhowch wybod i ni os yw hyn wedi newid."

    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch y swm rydych am ei hawlio ar gyfer offer eraill a brynwyd gan eich cleient"
    val expectedInvalidFormatErrorMessage = "Nodwch y swm rydych am ei hawlio ar gyfer offer eraill a brynwyd gan eich cleient yn y fformat cywir"
    val expectedOverMaximumErrorMessage = "Maeín rhaid iír swm rydych am ei hawlio ar gyfer offer eraill a brynwyd gan eich cleient fod yn llai na £100,000,000,000"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment expenses for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val totalAmountParagraph = "Total amount for all employers"
    val hintText = "For example, £193.52"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val continueButtonText = "Yn eich blaen"
    val totalAmountParagraph = "Cyfanswm ar gyfer pob cyflogwr"
    val hintText = "Er enghraifft, £193.52"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new ExpensesFormsProvider().otherEquipmentAmountForm(isAgent)

  private lazy val underTest = inject[OtherEquipmentAmountView]

  ".show" should {
    userScenarios.foreach { userScenario =>
      import Selectors._
      import userScenario.commonExpectedResults._
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render 'How much do you want to claim for buying other equipment?' page with the correct content and" +
          " no pre-filled amount when no user data" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), None)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(userScenario.commonExpectedResults.expectedCaption)
          elementsNotOnPageCheck(wantToClaimSelector)
          buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(totalAmountParagraph, totalAmountParagraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountField, "")
          formPostLinkCheck(OtherEquipmentAmountController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "render 'How much do you want to claim for buying other equipment?' page with  pre-filled amount if it has changed" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), Some(250))

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(userScenario.commonExpectedResults.expectedCaption)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedPreAmountParagraph(newAmount), wantToClaimSelector)
          buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(totalAmountParagraph, totalAmountParagraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountField, "")
          formPostLinkCheck(OtherEquipmentAmountController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "render 'How much do you want to claim for buying other equipment?' page with with no pre-filled amount if the amount value has not changed" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), Some(600))

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(userScenario.commonExpectedResults.expectedCaption)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedPreAmountParagraph(600), wantToClaimSelector)
          buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(totalAmountParagraph, totalAmountParagraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountField, "")
          formPostLinkCheck(OtherEquipmentAmountController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }
      }
    }
  }
}
