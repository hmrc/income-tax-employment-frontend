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

import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.ExpensesInterruptPageView

class ExpensesInterruptPageViewSpec extends ViewUnitTest {

  object Selectors {
    def paragraphSelector(index: Int): String = s"#main-content > div > div > div.govuk-panel.govuk-panel--interruption > form > p:nth-child($index)"

    val continueButtonSelector: String = "button.govuk-button"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val buttonText: String
    val expectedTitle: String
    val expectedHeading: String
    val expectedExample2: String
  }

  trait SpecificExpectedResults {
    val expectedExample1: String
    val expectedExample3: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val expectedTitle = "Employment expenses"
    val expectedHeading = "Employment expenses"
    val expectedExample2 = "You must add expenses as a total for all employment."

  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText = "Yn eich blaen"
    val expectedTitle = "Treuliau cyflogaeth"
    val expectedHeading = "Treuliau cyflogaeth"
    val expectedExample2 = "Mae’n rhaid i chi ychwanegu treuliau fel cyfanswm ar gyfer pob cyflogaeth."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedExample1 = "Use this section to update your employment expenses."
    val expectedExample3 = "Tell us about expenses you did not claim through your employers."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedExample1 = "Defnyddiwch yr adran hon i ddiweddaru eich treuliau cyflogaeth."
    val expectedExample3 = "Rhowch wybod i ni am dreuliau na wnaethoch eu hawlio drwy’ch cyflogwyr."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedExample1 = "Use this section to update your client’s employment expenses."
    val expectedExample3 = "Tell us about expenses your client did not claim through their employers."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedExample1 = "Defnyddiwch yr adran hon i ddiweddaru treuliau cyflogaeth eich cleient."
    val expectedExample3 = "Rhowch wybod i ni am dreuliau na wnaeth eich cleient eu hawlio drwy ei gyflogwyr."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[ExpensesInterruptPageView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render expenses interrupt page page with the correct content" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedTitle)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample1, paragraphSelector(2))
        textOnPageCheck(expectedExample2, paragraphSelector(3))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedExample3, paragraphSelector(4))
        buttonCheck(buttonText, continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
