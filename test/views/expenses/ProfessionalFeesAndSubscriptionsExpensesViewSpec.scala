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

import controllers.expenses.routes.ProfessionalFeesAndSubscriptionsExpensesController
import forms.AmountForm
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.ProfessionalFeesAndSubscriptionsExpensesView

class ProfessionalFeesAndSubscriptionsExpensesViewSpec extends ViewUnitTest {

  private val professionalFeesLink = "https://www.gov.uk/tax-relief-for-employees/professional-fees-and-subscriptions"

  object Selectors {
    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-child($index)"

    def bulletListSelector(index: Int): String = s"#main-content > div > div > ul > li:nth-child($index)"

    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val professionFeesLinkSelector = "#professional-fees-link"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedParagraphText: String
    val yesText: String
    val noText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
    val expectedExample1: String
    val expectedExample2: String
    val checkIfYouCanClaim: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedParagraphText = "This includes things like:"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedParagraphText = "Mae hyn yn cynnwys pethau fel:"
    val yesText = "Iawn"
    val noText = "Na"
    val buttonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for professional fees and subscriptions?"
    val expectedHeading = "Do you want to claim for professional fees and subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for professional fees and subscriptions"
    val expectedExample1 = "professional membership fees, if you have to pay the fees to do your job"
    val expectedExample2 = "yearly subscriptions to approved professional bodies or learned societies relevant to your job"
    val checkIfYouCanClaim = "Check if you can claim for professional fees and subscriptions (opens in new tab)."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol?"
    val expectedHeading = "A ydych am hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ i hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol"
    val expectedExample1 = "ffioedd aelodaeth broffesiynol, os oes rhaid i chi dalu’r ffioedd i wneud eich gwaith"
    val expectedExample2 = "tanysgrifiadau blynyddol i gyrff proffesiynol cymeradwy neu gymdeithasau dysgedig sy’n berthnasol i’ch swydd"
    val checkIfYouCanClaim = "Gwiriwch os gallwch hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol (yn agor tab newydd)."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for professional fees and subscriptions for your client?"
    val expectedHeading = "Do you want to claim for professional fees and subscriptions for your client?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for your client’s professional fees and subscriptions"
    val expectedExample1 = "professional membership fees, if your client has to pay the fees to do their job"
    val expectedExample2 = "yearly subscriptions to approved professional bodies or learned societies relevant to your client’s job"
    val checkIfYouCanClaim = "Check if your client can claim for professional fees and subscriptions (opens in new tab)."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol ar gyfer eich cleient?"
    val expectedHeading = "A ydych am hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol ar gyfer eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ i hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol eich cleient"
    val expectedExample1 = "ffioedd aelodaeth broffesiynol, os oes rhaid i’ch cleient dalu’r ffioedd i wneud ei waith"
    val expectedExample2 = "tanysgrifiadau blynyddol i gyrff proffesiynol cymeradwy neu gymdeithasau dysgedig sy’n berthnasol i swydd eich cleient"
    val checkIfYouCanClaim = "Gwiriwch a all eich cleient hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol (yn agor tab newydd)."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new ExpensesFormsProvider().professionalFeesAndSubscriptionsForm(isAgent)

  private lazy val underTest = inject[ProfessionalFeesAndSubscriptionsExpensesView]

  ".show" should {
    userScenarios.foreach { userScenario =>
      import Selectors._
      import userScenario.commonExpectedResults._
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render professional fees and subscriptions expenses question page with no pre-filled radio buttons" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample1, bulletListSelector(1))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample2, bulletListSelector(2))
          linkCheck(userScenario.specificExpectedResults.get.checkIfYouCanClaim, professionFeesLinkSelector, professionalFeesLink)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(ProfessionalFeesAndSubscriptionsExpensesController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "render professional fees and subscriptions expenses question page with 'No' pre-filled and not a prior submission" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).fill(value = false), taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample1, bulletListSelector(1))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample2, bulletListSelector(2))
          linkCheck(userScenario.specificExpectedResults.get.checkIfYouCanClaim, professionFeesLinkSelector, professionalFeesLink)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(ProfessionalFeesAndSubscriptionsExpensesController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "return an error when form is submitted with no entry" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraphText, paragraphSelector(index = 3))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample1, bulletListSelector(index = 1))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample2, bulletListSelector(index = 2))
          linkCheck(userScenario.specificExpectedResults.get.checkIfYouCanClaim, professionFeesLinkSelector, professionalFeesLink)
          radioButtonCheck(yesText, radioNumber = 1, checked = false)
          radioButtonCheck(noText, radioNumber = 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(ProfessionalFeesAndSubscriptionsExpensesController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Some("value"))
        }
      }
    }
  }
}
