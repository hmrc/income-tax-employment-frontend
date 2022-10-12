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

import controllers.expenses.routes.OtherEquipmentController
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.OtherEquipmentView

class OtherEquipmentViewSpec extends ViewUnitTest {

  object Selectors {
    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-child($index)"

    def bulletListSelector(index: Int): String = s"#main-content > div > div > ul > li:nth-child($index)"

    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedParagraphText: String
    val expectedExample1: String
    val expectedExample2: String
    val yesText: String
    val noText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedParagraphText = "This includes things like:"
    val expectedExample1 = "the cost of buying small items - like electrical drills and protective clothing"
    val expectedExample2 = "capital allowances for larger items - like machinery and computers"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedParagraphText = "Mae hyn yn cynnwys pethau fel:"
    val expectedExample1 = "cost prynu mân eitemau - fel driliau trydanol a dillad amddiffynnol"
    val expectedExample2 = "lwfansau cyfalaf ar gyfer eitemau mwy - fel peiriannau a chyfrifiaduron"
    val yesText = "Iawn"
    val noText = "Na"
    val buttonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for buying other equipment?"
    val expectedHeading = "Do you want to claim for buying other equipment?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for buying other equipment"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am hawlio ar gyfer prynu offer eraill?"
    val expectedHeading = "A ydych am hawlio ar gyfer prynu offer eraill?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ i hawlio ar gyfer prynu offer eraill"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for buying other equipment for your client?"
    val expectedHeading = "Do you want to claim for buying other equipment for your client?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for your client buying other equipment"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am hawlio ar gyfer prynu offer eraill ar gyfer eich cleient?"
    val expectedHeading = "A ydych am hawlio ar gyfer prynu offer eraill ar gyfer eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ i hawlio ar gyfer offer eraill a brynwyd gan eich cleient"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new ExpensesFormsProvider().otherEquipmentForm(isAgent)

  private lazy val underTest = inject[OtherEquipmentView]

  ".show" should {
    userScenarios.foreach { userScenario =>
      import Selectors._
      import userScenario.commonExpectedResults._
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render other equipment question page with no pre-filled radio buttons" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(expectedExample1, bulletListSelector(1))
          textOnPageCheck(expectedExample2, bulletListSelector(2))
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(OtherEquipmentController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "render other equipment question page with 'Yes' pre-filled and CYA data exists" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).fill(value = true), taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(expectedExample1, bulletListSelector(1))
          textOnPageCheck(expectedExample2, bulletListSelector(2))
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(OtherEquipmentController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "render other equipment question page with 'No' pre-filled and not a prior submission" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).fill(value = false), taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(expectedExample1, bulletListSelector(1))
          textOnPageCheck(expectedExample2, bulletListSelector(2))
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(OtherEquipmentController.submit(taxYearEOY).url, formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }
      }
    }
  }
}

