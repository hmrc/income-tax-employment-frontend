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

class IndividualAuthErrorControllerISpec extends IntegrationTest with ViewHelpers {

  object Selectors {
    val paragraphSelector: String = ".govuk-body"
    val linkSelector: String = paragraphSelector + " > a"
  }

  val url = s"$appUrl/error/you-need-to-sign-up"

  trait CommonExpectedResults {
    val validTitle: String
    val pageContent: String
    val linkContent: String
    val linkHref: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val validTitle: String = "You cannot view this page"
    val pageContent: String = "You need to sign up for Making Tax Digital for Income Tax before you can view this page."
    val linkContent: String = "sign up for Making Tax Digital for Income Tax"
    val linkHref: String = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val validTitle: String = "You cannot view this page"
    val pageContent: String = "You need to sign up for Making Tax Digital for Income Tax before you can view this page."
    val linkContent: String = "sign up for Making Tax Digital for Income Tax"
    val linkHref: String = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY))
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

          titleCheck(validTitle)
          welshToggleCheck(user.isWelsh)
          h1Check(validTitle, "xl")
          textOnPageCheck(pageContent, paragraphSelector)
          linkCheck(linkContent, linkSelector, linkHref)
        }
      }
    }
  }
}
