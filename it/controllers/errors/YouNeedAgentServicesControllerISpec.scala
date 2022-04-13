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
import utils.PageUrls.{createAnAgentLink, fullUrl, youNeedAgentServicesUrl}
import utils.{IntegrationTest, ViewHelpers}

class YouNeedAgentServicesControllerISpec extends IntegrationTest with ViewHelpers {

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
    val h1Expected = "You cannot view this page"
    val youNeedText = "You need to"
    val createAnAgentText = "create an agent services account"
    val beforeYouCanText = "before you can view this page."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY))
  }

  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with the right content" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(youNeedAgentServicesUrl), welsh = user.isWelsh)
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }

          import user.commonExpectedResults._

          titleCheck(h1Expected, user.isWelsh)
          welshToggleCheck(user.isWelsh)
          h1Check(h1Expected, "xl")
          textOnPageCheck(s"$youNeedText $createAnAgentText $beforeYouCanText", p1Selector)
          linkCheck(createAnAgentText, createAnAgentLinkSelector, createAnAgentLink)
        }
      }
    }
  }
}
