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

package views.templates

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utils.ViewTest
import views.html.templates.TaxYearErrorTemplate

class TaxYearErrorTemplateSpec extends ViewTest {

  object Selectors {

    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p:nth-child(1)"
    val p2Selector = "#main-content > div > div > div.govuk-body > p:nth-child(2)"
    val p3Selector = "#main-content > div > div > div.govuk-body > p:nth-child(3)"
    val linkSelector = "#govuk-self-assessment-link"
  }

  val h1Expected = "Page not found"
  val p1Expected = "You can only enter information for the 2021 to 2022 tax year."
  val p2Expected = "Check that you’ve entered the correct web address."
  val p3Expected: String = "If the web address is correct or you selected a link or button, you can use Self Assessment: " +
    "general enquiries (opens in new tab) to speak to someone about your income tax."
  val p3ExpectedLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
  val p3ExpectedLinkText = "Self Assessment: general enquiries (opens in new tab)"

  val taxYearErrorTemplate: TaxYearErrorTemplate = app.injector.instanceOf[TaxYearErrorTemplate]
  val appConfig: AppConfig = mockAppConfig


  "TaxYearErrorTemplate in English" should {

    "render the page correctly" which {

      lazy val view: HtmlFormat.Appendable = taxYearErrorTemplate()(fakeRequest, messages, mockAppConfig)
      implicit lazy val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected)
      welshToggleCheck("English")
      h1Check(h1Expected, "xl")

      textOnPageCheck(p1Expected,Selectors.p1Selector)
      textOnPageCheck(p2Expected,Selectors.p2Selector)
      textOnPageCheck(p3Expected,Selectors.p3Selector)
      linkCheck(p3ExpectedLinkText, Selectors.linkSelector, p3ExpectedLink)
    }
  }

  "TaxYearErrorTemplate in Welsh" should {

    "render the page correctly" which {

      lazy val view: HtmlFormat.Appendable = taxYearErrorTemplate()(fakeRequest, welshMessages, mockAppConfig)
      implicit lazy val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected)
      welshToggleCheck("Welsh")
      h1Check(h1Expected, "xl")

      textOnPageCheck(p1Expected,Selectors.p1Selector)
      textOnPageCheck(p2Expected,Selectors.p2Selector)
      textOnPageCheck(p3Expected,Selectors.p3Selector)
      linkCheck(p3ExpectedLinkText, Selectors.linkSelector, p3ExpectedLink)
    }
  }

}
