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

class YouNeedAgentServicesControllerISpec extends IntegrationTest with ViewHelpers {

  object ExpectedResults {
    object ContentEN {
      lazy val h1Expected = "You cannot view this page"
      lazy val youNeedText = "You need to"
      lazy val createAnAgentText = "create an agent services account"
      lazy val beforeYouCanText = "before you can view this page."
      lazy val createAnAgentLink = "https://www.gov.uk/guidance/get-an-hmrc-agent-services-account"
    }

    object ContentCY {
      lazy val h1Expected = "You cannot view this page"
      lazy val youNeedText = "You need to"
      lazy val createAnAgentText = "create an agent services account"
      lazy val beforeYouCanText = "before you can view this page."
      lazy val createAnAgentLink = "https://www.gov.uk/guidance/get-an-hmrc-agent-services-account"
    }
  }

  object Selectors {
    val p1Selector = "#main-content > div > div > p"
    val createAnAgentLinkSelector = "#create_agent_services_link"
  }

  val url = s"$appUrl/error/you-need-agent-services-account"

  "When set to english" when {

    import ExpectedResults.ContentEN._

    "the page is requested" should {

      "render the page" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          urlGet(url)
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }

        titleCheck(h1Expected)
        welshToggleCheck("English")
        h1Check(h1Expected, "xl")
        textOnPageCheck(s"$youNeedText $createAnAgentText $beforeYouCanText", Selectors.p1Selector)
        linkCheck(createAnAgentText, Selectors.createAnAgentLinkSelector, createAnAgentLink)
      }
    }
  }

  "When set to welsh" when {

    import ExpectedResults.ContentCY._

    "the page is requested" should {

      "render the page" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          urlGet(url, true)
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }

        titleCheck(h1Expected)
        welshToggleCheck("Welsh")
        h1Check(h1Expected, "xl")
        textOnPageCheck(s"$youNeedText $createAnAgentText $beforeYouCanText", Selectors.p1Selector)
        linkCheck(createAnAgentText, Selectors.createAnAgentLinkSelector, createAnAgentLink)
      }
    }
  }
}
