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

package controllers.errors

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class AgentAuthErrorControllerISpec extends IntegrationTest with ViewHelpers {

  object Selectors {
    val youCan = "#main-content > div > div > p:nth-child(2)"
    val authoriseAsAnAgentLinkSelector = "#client_auth_link"
    val tryAnother = "#main-content > div > div > a"
  }

  val url = s"$appUrl/error/you-need-client-authorisation"

  trait CommonExpectedResults {
    val heading: String
    val title: String
    val youCannotViewText: String
    val authoriseYouAsText: String
    val beforeYouCanTryText: String
    val tryAnother: String
    val authoriseAsAnAgentLink: String
    val tryAnotherExpectedHref: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val heading: String = "There’s a problem"
    val title = "There’s a problem"
    val youCannotViewText: String = "You cannot view this client’s information. Your client needs to"
    val authoriseYouAsText = "authorise you as their agent (opens in new tab)"
    val beforeYouCanTryText = "before you can sign in to this service."
    val tryAnother = "Try another client’s details"
    val authoriseAsAnAgentLink = "https://www.gov.uk/guidance/client-authorisation-an-overview"
    val tryAnotherExpectedHref = "http://localhost:11111/report-quarterly/income-and-expenses/view/agents/client-utr"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val heading: String = "There’s a problem"
    val title = "There’s a problem"
    val youCannotViewText: String = "You cannot view this client’s information. Your client needs to"
    val authoriseYouAsText = "authorise you as their agent (opens in new tab)"
    val beforeYouCanTryText = "before you can sign in to this service."
    val tryAnother = "Try another client’s details"
    val authoriseAsAnAgentLink = "https://www.gov.uk/guidance/client-authorisation-an-overview"
    val tryAnotherExpectedHref = "http://localhost:11111/report-quarterly/income-and-expenses/view/agents/client-utr"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY))
  }

  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return the AgentAuthErrorPageView with the right content" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh)
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }

          import user.commonExpectedResults._

          titleCheck(title)
          h1Check(heading,"xl")
          textOnPageCheck(s"$youCannotViewText $authoriseYouAsText $beforeYouCanTryText", youCan)
          linkCheck(authoriseYouAsText, authoriseAsAnAgentLinkSelector, authoriseAsAnAgentLink)
          buttonCheck(tryAnother, Selectors.tryAnother, Some(tryAnotherExpectedHref))
          welshToggleCheck(user.isWelsh)
        }
      }
    }
  }
}

