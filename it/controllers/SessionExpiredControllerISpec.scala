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

package controllers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class SessionExpiredControllerISpec extends IntegrationTest with ViewHelpers {

  object Selectors {
    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p"
    val buttonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  val url = s"$appUrl/timeout"

  trait CommonExpectedResults {
    val h1Expected: String
    val p1Expected: String
    val buttonExpectedText: String
    val buttonExpectedUrl: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val h1Expected = "For your security, we signed you out"
    val p1Expected = "We did not save your answers."
    val buttonExpectedText = "Sign in"
    val buttonExpectedUrl: String = "http://localhost:11111/update-and-submit-income-tax-return/2022/start"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val h1Expected = "For your security, we signed you out"
    val p1Expected = "We did not save your answers."
    val buttonExpectedText = "Sign in"
    val buttonExpectedUrl: String = "http://localhost:11111/update-and-submit-income-tax-return/2022/start"
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
            urlGet(url, welsh = user.isWelsh)
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          import user.commonExpectedResults._

          titleCheck(h1Expected)
          welshToggleCheck(user.isWelsh)
          h1Check(h1Expected, "xl")

          textOnPageCheck(p1Expected,p1Selector)
          buttonCheck(buttonExpectedText, buttonSelector)
          formGetLinkCheck(buttonExpectedUrl, formSelector)
        }
      }
    }
  }
}
