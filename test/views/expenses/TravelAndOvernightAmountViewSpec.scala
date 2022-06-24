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

import controllers.expenses.routes.TravelAndOvernightAmountController
import forms.AmountForm
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.TravelAndOvernightAmountView

class TravelAndOvernightAmountViewSpec extends ViewUnitTest {

  private val newAmount = 25
  private val amountInputName = "amount"

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val amountSelector = "#amount"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val totalAmountText: String
    val hintText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedDoNotClaim: String
    val expectedReplay: Int => String
    val expectedNoEntryError: String
    val expectedFormatError: String
    val expectedOverMaxError: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val totalAmountText = "Total amount for all employers"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val totalAmountText = "Cyfanswm ar gyfer pob cyflogwr"
    val hintText = "Er enghraifft, £193.52"
    val buttonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for business travel and overnight stays?"
    val expectedHeading = "How much do you want to claim for business travel and overnight stays?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedDoNotClaim = "Do not claim any amount your employer has paid you for."
    val expectedReplay: Int => String = amount =>
      s"You told us you want to claim £$amount for other business travel and overnight stays. Tell us if this has changed."
    val expectedNoEntryError = "Enter the amount you want to claim for business travel and overnight stays"
    val expectedFormatError = "Enter the amount you want to claim for business travel and overnight stays in the correct format"
    val expectedOverMaxError = "The amount you want to claim for business travel and overnight stays must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos?"
    val expectedHeading = "Faint rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedDoNotClaim = "Peidiwch ‚ hawlio unrhyw swm y mae eich cyflogwr wedi’i dalu i chi."
    val expectedReplay: Int => String = amount =>
      s"Dywedoch wrthym eich bod am hawlio £$amount ar gyfer costau teithio busnes ac aros dros nos. Rhowch wybod i ni os yw hyn wedi newid."
    val expectedNoEntryError = "Nodwch y swm rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos"
    val expectedFormatError = "Nodwch y swm rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos yn y fformat cywir"
    val expectedOverMaxError = "Maeín rhaid iír swm rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for your client’s business travel and overnight stays?"
    val expectedHeading = "How much do you want to claim for your client’s business travel and overnight stays?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for your client’s travel and overnight stays"
    val expectedDoNotClaim = "Do not claim any amount your client’s employer has paid them for."
    val expectedReplay: Int => String = amount =>
      s"You told us you want to claim £$amount for your client’s other business travel and overnight stays. Tell us if this has changed."
    val expectedNoEntryError = "Enter the amount you want to claim for your client’s business travel and overnight stays"
    val expectedFormatError = "Enter the amount you want to claim for business travel and overnight stays for your client in the correct format"
    val expectedOverMaxError = "The amount you want to claim for your client’s business travel and overnight stays must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos eich cleient?"
    val expectedHeading = "Faint rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ i hawlio ar gyfer costau teithio ac aros dros nos eich cleient"
    val expectedDoNotClaim = "Peidiwch ‚ hawlio unrhyw swm y mae cyflogwr eich cleient wedi’i dalu iddo."
    val expectedReplay: Int => String = amount =>
      s"Dywedoch wrthym eich bod am hawlio £$amount ar gyfer costau teithio busnes ac aros dros nos eich cleient . Rhowch wybod i ni os yw hyn wedi newid."
    val expectedNoEntryError = "Nodwch y swm rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos eich cleient"
    val expectedFormatError = "Nodwch y swm rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos ar gyfer eich cleient yn y fformat cywir"
    val expectedOverMaxError = "Maeín rhaid iír swm rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new ExpensesFormsProvider().businessTravelAndOvernightAmountForm(isAgent)

  private lazy val underTest = inject[TravelAndOvernightAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the amount page with no prefilled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), None)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDoNotClaim, paragraphSelector(index = 2))
        textOnPageCheck(totalAmountText, paragraphSelector(index = 3))
        hintTextCheck(hintText)
        inputFieldValueCheck(amountInputName, Selectors.amountSelector, "")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(TravelAndOvernightAmountController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with a pre-filled amount field" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).fill(value = 25), Some(25))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplay(newAmount), paragraphSelector(index = 2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDoNotClaim, paragraphSelector(index = 3))
        textOnPageCheck(totalAmountText, paragraphSelector(index = 4))
        hintTextCheck(hintText)
        inputFieldValueCheck(amountInputName, Selectors.amountSelector, newAmount.toString)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(TravelAndOvernightAmountController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when a form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), Some(200))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplay(200), paragraphSelector(index = 3))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDoNotClaim, paragraphSelector(index = 4))
        textOnPageCheck(totalAmountText, paragraphSelector(index = 5))
        hintTextCheck(hintText)
        inputFieldValueCheck(amountInputName, Selectors.amountSelector, "")
        buttonCheck(buttonText, continueButtonSelector)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoEntryError, Some("amount"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryError, Selectors.amountSelector)
        formPostLinkCheck(TravelAndOvernightAmountController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "a form is submitted with an incorrectly formatted amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), Some(200))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplay(200), paragraphSelector(index = 3))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDoNotClaim, paragraphSelector(index = 4))
        textOnPageCheck(totalAmountText, paragraphSelector(index = 5))
        hintTextCheck(hintText)
        inputFieldValueCheck(amountInputName, Selectors.amountSelector, "123.33.33")
        buttonCheck(buttonText, continueButtonSelector)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedFormatError, Some("amount"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedFormatError, Selectors.amountSelector)
        formPostLinkCheck(TravelAndOvernightAmountController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "a form is submitted and the amount is over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), Some(200))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplay(200), paragraphSelector(index = 3))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDoNotClaim, paragraphSelector(index = 4))
        textOnPageCheck(totalAmountText, paragraphSelector(index = 5))
        hintTextCheck(hintText)
        inputFieldValueCheck(amountInputName, Selectors.amountSelector, "100,000,000,000")
        buttonCheck(buttonText, continueButtonSelector)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedOverMaxError, Some("amount"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedOverMaxError, Selectors.amountSelector)
        formPostLinkCheck(TravelAndOvernightAmountController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
