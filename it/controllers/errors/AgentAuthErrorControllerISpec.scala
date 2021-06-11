/*
 * Copyright 2021 HM Revenue & Customs
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

  object ExpectedResults {
    object ContentEN {
      val heading: String = "There’s a problem"
      val title = "There’s a problem"
      val youCannotViewText: String = "You cannot view this client’s information. Your client needs to"
      val authoriseYouAsText = "authorise you as their agent (opens in new tab)"
      val beforeYouCanTryText = "before you can sign in to this service."
      val tryAnother = "Try another client’s details"
      val authoriseAsAnAgentLink = "https://www.gov.uk/guidance/client-authorisation-an-overview"
      val tryAnotherExpectedHref = "http://localhost:11111/report-quarterly/income-and-expenses/view/agents/client-utr"
    }

    object ContentCY {
      val heading: String = "There’s a problem"
      val title = "There’s a problem"
      val youCannotViewText: String = "You cannot view this client’s information. Your client needs to"
      val authoriseYouAsText = "authorise you as their agent (opens in new tab)"
      val beforeYouCanTryText = "before you can sign in to this service."
      val tryAnother = "Try another client’s details"
      val authoriseAsAnAgentLink = "https://www.gov.uk/guidance/client-authorisation-an-overview"
      val tryAnotherExpectedHref = "http://localhost:11111/report-quarterly/income-and-expenses/view/agents/client-utr"
    }
  }

  object Selectors {
    val youCan = "#main-content > div > div > p:nth-child(2)"
    val authoriseAsAnAgentLinkSelector = "#client_auth_link"
    val tryAnother = "#main-content > div > div > a"
  }

  val url = s"$appUrl/error/you-need-client-authorisation"

  "calling GET with english header" when {

    import ExpectedResults.ContentEN._

    "the user is an individual" should {
      "return the AgentAuthErrorPageView with the right content" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          urlGet(url)
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }

        titleCheck(title)
        h1Check(heading,"xl")
        textOnPageCheck(s"$youCannotViewText $authoriseYouAsText $beforeYouCanTryText", Selectors.youCan)
        linkCheck(authoriseYouAsText, Selectors.authoriseAsAnAgentLinkSelector, authoriseAsAnAgentLink)
        buttonCheck(tryAnother, Selectors.tryAnother, Some(tryAnotherExpectedHref))
      }
    }
  }

  "calling GET with welsh header" when {

    import ExpectedResults.ContentCY._

    "the user is an individual" should {
      "return the AgentAuthErrorPageView with the right content" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          urlGet(url, true)
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }

        titleCheck(title)
        h1Check(heading,"xl")
        textOnPageCheck(s"$youCannotViewText $authoriseYouAsText $beforeYouCanTryText", Selectors.youCan)
        linkCheck(authoriseYouAsText, Selectors.authoriseAsAnAgentLinkSelector, authoriseAsAnAgentLink)
        buttonCheck(tryAnother, Selectors.tryAnother, Some(tryAnotherExpectedHref))
      }
    }
  }
}

