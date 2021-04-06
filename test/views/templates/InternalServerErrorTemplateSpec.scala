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
import views.html.templates.InternalServerErrorTemplate

class InternalServerErrorTemplateSpec extends ViewTest {

  object Selectors {

    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p:nth-child(1)"
    val p2Selector = "#main-content > div > div > div.govuk-body > p:nth-child(2)"
    val bulletPoint1 = "#main-content > div > div > ul > li:nth-child(1)"
    val bulletPointLinkSelector1 = "#govuk-income-tax-link"
    val bulletPoint2 = "#main-content > div > div > ul > li:nth-child(2)"
    val bulletPointLinkSelector2 = "#govuk-self-assessment-link"

  }

  val h1Expected = "Sorry, there is a problem with the service"
  val p1Expected = "Try again later."
  val p2Expected = "You can also:"
  val bulletPoint1Expected = "go to the Income Tax home page (opens in new tab) for more information"
  val bulletPoint1Link = "https://www.gov.uk/income-tax"
  val bulletPoint1LinkText = "Income Tax home page (opens in new tab)"
  val bulletPoint2Expected = "use Self Assessment: general enquiries (opens in new tab) to speak to someone about your income tax"
  val bulletPoint2Link = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
  val bulletPoint2LinkText = "Self Assessment: general enquiries (opens in new tab)"


  lazy val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  lazy val appConfig: AppConfig = mockAppConfig

  "UnauthorisedTemplate in English" should {

    "render the page correctly" which {

      lazy val view: HtmlFormat.Appendable = internalServerErrorTemplate()(fakeRequest, messages, appConfig)
      implicit lazy val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected)
      welshToggleCheck("English")
      h1Check(h1Expected, "xl")
      textOnPageCheck(p1Expected, Selectors.p1Selector)
      textOnPageCheck(p2Expected, Selectors.p2Selector)

      textOnPageCheck(bulletPoint1Expected,Selectors.bulletPoint1)
      linkCheck(bulletPoint1LinkText, Selectors.bulletPointLinkSelector1, bulletPoint1Link)

      textOnPageCheck(bulletPoint2Expected,Selectors.bulletPoint2)
      linkCheck(bulletPoint2LinkText, Selectors.bulletPointLinkSelector2, bulletPoint2Link)

    }
  }

  "UnauthorisedTemplate in Welsh" should {

    "render the page correctly" which {

      lazy val view: HtmlFormat.Appendable = internalServerErrorTemplate()(fakeRequest, welshMessages, appConfig)
      implicit lazy val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected)
      welshToggleCheck("Welsh")
      h1Check(h1Expected, "xl")
      textOnPageCheck(p1Expected, Selectors.p1Selector)
      textOnPageCheck(p2Expected, Selectors.p2Selector)

      textOnPageCheck(bulletPoint1Expected,Selectors.bulletPoint1)
      linkCheck(bulletPoint1LinkText, Selectors.bulletPointLinkSelector1, bulletPoint1Link)

      textOnPageCheck(bulletPoint2Expected,Selectors.bulletPoint2)
      linkCheck(bulletPoint2LinkText, Selectors.bulletPointLinkSelector2, bulletPoint2Link)

    }
  }
}
