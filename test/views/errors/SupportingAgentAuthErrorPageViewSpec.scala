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

package views.errors

import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.errors.SupportingAgentAuthErrorView

class SupportingAgentAuthErrorPageViewSpec extends ViewUnitTest {

  object Selectors {
    val p1Selector = "#main-content > div > div > p:nth-child(2)"
    val authoriseAsAnAgentLinkSelector = "#account_home_link"
  }

  trait CommonExpectedResults {
    val h1Expected: String
    val accessServiceText: String
    val linkText: String
    val tryAnotherClientExpectedHref: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val h1Expected = "You are not authorised to use this service"
    val accessServiceText: String = "You’re a supporting agent for this client. Only your client or their main agent, if they have one, can access and submit their tax return."
    val linkText = "Go back to account home"
    val tryAnotherClientExpectedHref = "/report-quarterly/income-and-expenses/view/agents"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val h1Expected = "Nid ydych wedi’ch awdurdodi i ddefnyddio’r gwasanaeth hwn"
    val accessServiceText: String = "Rydych yn asiant ategol ar gyfer y cleient hwn. Dim ond eich cleient neu ei brif asiant, os oes ganddo un, sy’n gallu cael at a chyflwyno ei Ffurflen Dreth."
    val linkText = "Yn ôl i hafan y cyfrif"
    val tryAnotherClientExpectedHref = "/report-quarterly/income-and-expenses/view/agents"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  private lazy val underTest = inject[SupportingAgentAuthErrorView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "Render correctly" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest()

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(h1Expected, isWelsh = userScenario.isWelsh)
        h1Check(h1Expected, size = "xl")
        textOnPageCheck(s"$accessServiceText", p1Selector)
        linkCheck(linkText, authoriseAsAnAgentLinkSelector, tryAnotherClientExpectedHref)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
