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

package views.authErrorPages

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.twirl.api.Html
import utils.ViewTest
import views.html.authErrorPages.UnauthorisedUserErrorPageView

class UnauthorisedUserErrorPageViewSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ViewTest {

  val p1Selector = "#main-content > div > div > div.govuk-body > p"
  val p2Selector = "#main-content > div > div > ul > li:nth-child(1)"
  val p3Selector = "#main-content > div > div > ul > li:nth-child(2)"
  val incomeTaxHomePageLinkSelector = "#govuk-income-tax-link"
  val selfAssessmentLinkSelector = "#govuk-self-assessment-link"

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

  val unauthorisedUserErrorPageView: UnauthorisedUserErrorPageView = app.injector.instanceOf[UnauthorisedUserErrorPageView]

  "UnauthorisedUserErrorPageView in English" should {

    "Render page correctly" which {

      lazy val view: Html = unauthorisedUserErrorPageView()(fakeRequest, messages, mockAppConfig)
      lazy implicit val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected)
      welshToggleCheck("English")
      h1Check(h1Expected, "xl")
      textOnPageCheck(youCanText, p1Selector)
      textOnPageCheck(s"$goToTheText $incomeTaxHomePageText $forMoreInformationText",p2Selector)
      linkCheck(incomeTaxHomePageText, incomeTaxHomePageLinkSelector, incomeTaxHomePageLink)
      textOnPageCheck(s"$useText $selfAssessmentText $toSpeakText", p3Selector)
      linkCheck(selfAssessmentText, selfAssessmentLinkSelector, selfAssessmentLink)

    }

  }

  "UnauthorisedUserErrorPageView in Welsh" should {

    "Render page correctly" which {

      lazy val view: Html = unauthorisedUserErrorPageView()(fakeRequest, welshMessages, mockAppConfig)
      lazy implicit val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected)
      welshToggleCheck("Welsh")
      h1Check(h1Expected, "xl")
      textOnPageCheck(youCanText, p1Selector)
      textOnPageCheck(s"$goToTheText $incomeTaxHomePageText $forMoreInformationText",p2Selector)
      linkCheck(incomeTaxHomePageText, incomeTaxHomePageLinkSelector, incomeTaxHomePageLink)
      textOnPageCheck(s"$useText $selfAssessmentText $toSpeakText", p3Selector)
      linkCheck(selfAssessmentText, selfAssessmentLinkSelector, selfAssessmentLink)

    }

  }

}
