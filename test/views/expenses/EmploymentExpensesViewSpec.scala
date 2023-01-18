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

package views.expenses

import controllers.expenses.routes.EmploymentExpensesController
import forms.YesNoForm
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.EmploymentExpensesView

class EmploymentExpensesViewSpec extends ViewUnitTest {

  private val taxReliefLink = "https://www.gov.uk/tax-relief-for-employees"

  object Selectors {
    val thisIncludesExample1Selector: String = "#main-content > div > div > ul > li:nth-child(1)"
    val thisIncludesExample2Selector: String = "#main-content > div > div > ul > li:nth-child(2)"
    val thisIncludesExample3Selector: String = "#main-content > div > div > ul > li:nth-child(3)"
    val expensesLinkSelector: String = "#expenses-link"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedCanClaim: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedButtonText: String
    val expectedThisIncludes: String
    val expectedThisIncludesExample1: String
    val expectedThisIncludesExample2: String
    val expectedThisIncludesExample3: String
    val expectedFindOutMore: String
    val expectedFindOutMoreLink: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim employment expenses?"
    val expectedHeading = "Do you want to claim employment expenses?"
    val expectedCanClaim = "You can claim employment expenses you did not claim through your employer."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to claim employment expenses"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am hawlio treuliau cyflogaeth?"
    val expectedHeading = "A ydych am hawlio treuliau cyflogaeth?"
    val expectedCanClaim = "Gallwch hawlio treuliau cyflogaeth na wnaethoch eu hawlio drwy eich cyflogwr."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os ydych am hawlio treuliau cyflogaeth"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim employment expenses for your client?"
    val expectedHeading = "Do you want to claim employment expenses for your client?"
    val expectedCanClaim = "You can claim employment expenses your client did not claim through their employer."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to claim for your client’s employment expenses"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am hawlio treuliau cyflogaeth ar gyfer eich cleient?"
    val expectedHeading = "A ydych am hawlio treuliau cyflogaeth ar gyfer eich cleient?"
    val expectedCanClaim = "Gallwch hawlio treuliau cyflogaeth na wnaeth eich cleient eu hawlio drwy ei gyflogwr."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os ydych am hawlio treuliau cyflogaeth eich cleient"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment expenses for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedThisIncludes = "Employment expenses include things like:"
    val expectedThisIncludesExample1 = "business travel and hotels and meals"
    val expectedThisIncludesExample2 = "professional fees and subscriptions"
    val expectedThisIncludesExample3 = "uniforms, work clothes and tools"
    val expectedFindOutMore = "Find out more about claiming employment expenses (opens in new tab)."
    val expectedFindOutMoreLink = "claiming employment expenses (opens in new tab)."
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val expectedThisIncludes = "Mae treuliau cyflogaeth yn cynnwys pethau fel:"
    val expectedThisIncludesExample1 = "teithiau busnes a gwestai a phrydau bwyd"
    val expectedThisIncludesExample2 = "ffioedd a thanysgrifiadau proffesiynol"
    val expectedThisIncludesExample3 = "gwisgoedd unffurf, dillad gwaith ac offer"
    val expectedFindOutMore = "Dysgwch Ragor o wybodaeth ynghylch hawlio treuliau cyflogaeth (yn agor tab newydd)."
    val expectedFindOutMoreLink = "hawlio treuliau cyflogaeth (yn agor tab newydd)."
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new ExpensesFormsProvider().claimEmploymentExpensesForm(isAgent)

  private lazy val underTest = inject[EmploymentExpensesView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page without pre-filled radio buttons" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCanClaim, paragraphSelector(index = 2))
        textOnPageCheck(expectedThisIncludes, paragraphSelector(index = 3))
        textOnPageCheck(expectedThisIncludesExample1, thisIncludesExample1Selector)
        textOnPageCheck(expectedThisIncludesExample2, thisIncludesExample2Selector)
        textOnPageCheck(expectedThisIncludesExample3, thisIncludesExample3Selector)
        textOnPageCheck(expectedFindOutMore, paragraphSelector(index = 5))
        linkCheck(expectedFindOutMoreLink, expensesLinkSelector, taxReliefLink)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmploymentExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'Yes' pre-filled and CYA data exists" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = true), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCanClaim, paragraphSelector(index = 2))
        textOnPageCheck(expectedThisIncludes, paragraphSelector(index = 3))
        textOnPageCheck(expectedThisIncludesExample1, thisIncludesExample1Selector)
        textOnPageCheck(expectedThisIncludesExample2, thisIncludesExample2Selector)
        textOnPageCheck(expectedThisIncludesExample3, thisIncludesExample3Selector)
        textOnPageCheck(expectedFindOutMore, paragraphSelector(index = 5))
        linkCheck(expectedFindOutMoreLink, expensesLinkSelector, taxReliefLink)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmploymentExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'No' pre-filled and not a prior submission" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = false), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCanClaim, paragraphSelector(index = 2))
        textOnPageCheck(expectedThisIncludes, paragraphSelector(index = 3))
        textOnPageCheck(expectedThisIncludesExample1, thisIncludesExample1Selector)
        textOnPageCheck(expectedThisIncludesExample2, thisIncludesExample2Selector)
        textOnPageCheck(expectedThisIncludesExample3, thisIncludesExample3Selector)
        textOnPageCheck(expectedFindOutMore, paragraphSelector(index = 5))
        linkCheck(expectedFindOutMoreLink, expensesLinkSelector, taxReliefLink)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmploymentExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCanClaim, paragraphSelector(index = 3))
        textOnPageCheck(expectedThisIncludes, paragraphSelector(index = 4))
        textOnPageCheck(expectedThisIncludesExample1, thisIncludesExample1Selector)
        textOnPageCheck(expectedThisIncludesExample2, thisIncludesExample2Selector)
        textOnPageCheck(expectedThisIncludesExample3, thisIncludesExample3Selector)
        textOnPageCheck(expectedFindOutMore, paragraphSelector(index = 6))
        linkCheck(expectedFindOutMoreLink, expensesLinkSelector, taxReliefLink)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmploymentExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText, Some("value"))
      }
    }
  }
}
