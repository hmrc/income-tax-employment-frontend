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

package views.errors

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import support.ViewUnitTest
import views.html.errors.UnauthorisedUserErrorPageView

class UnauthorisedUserErrorPageViewSpec extends ViewUnitTest {

  private val incomeTaxHomePageLink = "https://www.gov.uk/income-tax"
  private val selfAssessmentLink: String = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"

  object Selectors {
    val p1Selector = "#main-content > div > div > div.govuk-body > p"
    val p2Selector = "#main-content > div > div > ul > li:nth-child(1)"
    val p3Selector = "#main-content > div > div > ul > li:nth-child(2)"
    val incomeTaxHomePageLinkSelector = "#govuk-income-tax-link"
    val selfAssessmentLinkSelector = "#govuk-self-assessment-link"
  }

  trait CommonExpectedResults {
    val h1Expected: String
    val youCanText: String
    val goToTheText: String
    val incomeTaxHomePageText: String
    val forMoreInformationText: String
    val useText: String
    val selfAssessmentText: String
    val toSpeakText: String
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
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val h1Expected = "Nid ydych wediích awdurdodi i ddefnyddioír gwasanaeth hwn"
    val youCanText = "Gallwch wneud y canlynol:"
    val goToTheText = "mynd iír"
    val incomeTaxHomePageText = "hafan Treth Incwm (yn agor tab newydd)"
    val forMoreInformationText = "am ragor o wybodaeth"
    val useText = "defnyddio"
    val selfAssessmentText = "Hunanasesiad: ymholiadau cyffredinol (yn agor tab newydd)"
    val toSpeakText = "i siarad ‚ rhywun am eich treth incwm"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  private lazy val underTest = inject[UnauthorisedUserErrorPageView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the page with the right content" which {
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/error/not-authorised-to-use-service")
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest()

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(h1Expected, userScenario.isWelsh)
        welshToggleCheck(userScenario.isWelsh)
        h1Check(h1Expected, "xl")
        textOnPageCheck(youCanText, p1Selector)
        textOnPageCheck(s"$goToTheText $incomeTaxHomePageText $forMoreInformationText", p2Selector)
        linkCheck(incomeTaxHomePageText, incomeTaxHomePageLinkSelector, incomeTaxHomePageLink)
        textOnPageCheck(s"$useText $selfAssessmentText $toSpeakText", p3Selector)
        linkCheck(selfAssessmentText, selfAssessmentLinkSelector, selfAssessmentLink)
      }
    }
  }
}
