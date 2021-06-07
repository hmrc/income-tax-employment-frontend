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
      val tryAnother = "Try another client’s details."
      val authoriseAsAnAgentLink = "https://www.gov.uk/guidance/client-authorisation-an-overview"
    }

    object ContentCY {
      val heading: String = "There’s a problem"
      val title = "There’s a problem"
      val youCannotViewText: String = "You cannot view this client’s information. Your client needs to"
      val authoriseYouAsText = "authorise you as their agent (opens in new tab)"
      val beforeYouCanTryText = "before you can sign in to this service."
      val tryAnother = "Try another client’s details."
      val authoriseAsAnAgentLink = "https://www.gov.uk/guidance/client-authorisation-an-overview"
    }
  }

  object Selectors {
    val youCan = "#main-content > div > div > p:nth-child(2)"
    val tryAnother = "#main-content > div > div > p:nth-child(3)"
    val authoriseAsAnAgentLinkSelector = "#client_auth_link"
  }

  lazy val url = s"${appUrl(port)}/error/you-need-client-authorisation"

  "calling GET with english header" when {

    import ExpectedResults.ContentEN._

    "the user is an individual" should {
      "return the AgentAuthErrorPageView with the right content" which {
        implicit lazy val result: WSResponse = {
          authoriseIndividual()
          urlGet(url)(wsClient)
        }

        titleCheck(title)
        h1Check(heading,"xl")
        textOnPageCheck(s"$youCannotViewText $authoriseYouAsText $beforeYouCanTryText", Selectors.youCan)
        linkCheck(authoriseYouAsText, Selectors.authoriseAsAnAgentLinkSelector, authoriseAsAnAgentLink)
        textOnPageCheck(tryAnother, Selectors.tryAnother)
      }
    }
  }

  "calling GET with welsh header" when {

    import ExpectedResults.ContentCY._

    "the user is an individual" should {
      "return the AgentAuthErrorPageView with the right content" which {
        implicit lazy val result: WSResponse = {
          authoriseIndividual()
          urlGet(url, true)(wsClient)
        }

        titleCheck(title)
        h1Check(heading,"xl")
        textOnPageCheck(s"$youCannotViewText $authoriseYouAsText $beforeYouCanTryText", Selectors.youCan)
        linkCheck(authoriseYouAsText, Selectors.authoriseAsAnAgentLinkSelector, authoriseAsAnAgentLink)
        textOnPageCheck(tryAnother, Selectors.tryAnother)
      }
    }
  }
}

