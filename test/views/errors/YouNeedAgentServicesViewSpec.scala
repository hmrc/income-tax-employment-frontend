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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import support.ViewUnitTest
import views.html.errors.YouNeedAgentServicesView

class YouNeedAgentServicesViewSpec extends ViewUnitTest {

  private val createAnAgentLink = "https://www.gov.uk/guidance/get-an-hmrc-agent-services-account"

  object Selectors {
    val p1Selector = "#main-content > div > div > p"
    val createAnAgentLinkSelector = "#create_agent_services_link"
  }

  trait CommonExpectedResults {
    val h1Expected: String
    val youNeedText: String
    val createAnAgentText: String
    val beforeYouCanText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val h1Expected = "You cannot view this page"
    val youNeedText = "You need to"
    val createAnAgentText = "create an agent services account"
    val beforeYouCanText = "before you can view this page."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val h1Expected = "Ni allwch fwrw golwg dros y dudalen hon"
    val youNeedText = "Mae angen"
    val createAnAgentText = "creu cyfrif gwasanaethau asiant"
    val beforeYouCanText = "cyn i chi allu bwrw golwg dros y dudalen hon."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  private lazy val underTest = inject[YouNeedAgentServicesView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the page with the right content" which {
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/error/you-need-agent-services-account")
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest()

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(h1Expected, userScenario.isWelsh)
        welshToggleCheck(userScenario.isWelsh)
        h1Check(h1Expected, size = "xl")
        textOnPageCheck(s"$youNeedText $createAnAgentText $beforeYouCanText", p1Selector)
        linkCheck(createAnAgentText, createAnAgentLinkSelector, createAnAgentLink)
      }
    }
  }
}
