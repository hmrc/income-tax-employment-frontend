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

class UnauthorisedUserErrorControllerISpec extends IntegrationTest with ViewHelpers {

  object Selectors {
    val p1Selector = "#main-content > div > div > div.govuk-body > p"
    val p2Selector = "#main-content > div > div > ul > li:nth-child(1)"
    val p3Selector = "#main-content > div > div > ul > li:nth-child(2)"
    val incomeTaxHomePageLinkSelector = "#govuk-income-tax-link"
    val selfAssessmentLinkSelector = "#govuk-self-assessment-link"
  }

  val url = s"$appUrl/error/not-authorised-to-use-service"

  trait CommonExpectedResults {
    val h1Expected: String
    val youCanText: String
    val goToTheText: String
    val incomeTaxHomePageText: String
    val forMoreInformationText: String
    val useText: String
    val selfAssessmentText: String
    val toSpeakText: String
    val incomeTaxHomePageLink: String
    val selfAssessmentLink: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val h1Expected = "You are not authorised to use this service"
    val youCanText = "You can:"
    val goToTheText = "go to the"
    val incomeTaxHomePageText = "Income Tax home page (opens in new tab)"
    val forMoreInformationText = "for more information"
    val useText = "use"
    val selfAssessmentText = "Self Assessment: general enquiries (opens in new tab)"
    val toSpeakText = "to speak to someone about your income tax"
    val incomeTaxHomePageLink = "https://www.gov.uk/income-tax"
    val selfAssessmentLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val h1Expected = "You are not authorised to use this service"
    val youCanText = "You can:"
    val goToTheText = "go to the"
    val incomeTaxHomePageText = "Income Tax home page (opens in new tab)"
    val forMoreInformationText = "for more information"
    val useText = "use"
    val selfAssessmentText = "Self Assessment: general enquiries (opens in new tab)"
    val toSpeakText = "to speak to someone about your income tax"
    val incomeTaxHomePageLink = "https://www.gov.uk/income-tax"
    val selfAssessmentLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
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

          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }

          import user.commonExpectedResults._

          titleCheck(h1Expected)
          welshToggleCheck(user.isWelsh)
          h1Check(h1Expected, "xl")
          textOnPageCheck(youCanText, p1Selector)
          textOnPageCheck(s"$goToTheText $incomeTaxHomePageText $forMoreInformationText",p2Selector)
          linkCheck(incomeTaxHomePageText, incomeTaxHomePageLinkSelector, incomeTaxHomePageLink)
          textOnPageCheck(s"$useText $selfAssessmentText $toSpeakText", p3Selector)
          linkCheck(selfAssessmentText, selfAssessmentLinkSelector, selfAssessmentLink)
        }
      }
    }
  }
}
