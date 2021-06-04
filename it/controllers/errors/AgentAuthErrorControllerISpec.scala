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
import utils.IntegrationTest
import org.jsoup.nodes.Document
import play.api.libs.ws.{WSClient, WSResponse}
import utils.ViewHelpers

class AgentAuthErrorControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  object ExpectedResults {
    val heading: String = "There’s a problem"
    val title = "There’s a problem"
    val youCan = "You cannot view this client’s information. Your client needs to authorise you as their agent (opens in new tab) before you can sign in to this service."
    val tryAnother = "Try another client’s details"
    val tryAnotherExpectedHref = "http://localhost:11111/report-quarterly/income-and-expenses/view/agents/client-utr"

  }
  object Selectors {
    val youCan = "#main-content > div > div > p:nth-child(2)"
    val tryAnother = "#main-content > div > div > a"

  }
     val url = s"http://localhost:$port/income-through-software/return/employment-income/error/you-need-client-authorisation"

  "calling GET" when {
    "an individual" should {
      "return a page" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url).get())
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)
        titleCheck(ExpectedResults.title)
        h1Check(ExpectedResults.heading,"xl")
        textOnPageCheck(ExpectedResults.youCan, Selectors.youCan)
        buttonCheck(ExpectedResults.tryAnother, Selectors.tryAnother, Some(ExpectedResults.tryAnotherExpectedHref))
      }
    }
  }

}

