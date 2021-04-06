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
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import utils.ViewTest
import views.html.templates.TimeoutPage

class TimeoutPageSpec extends ViewTest {

  val taxYear = 2021

  object Selectors {
    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p"
    val buttonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  val h1Expected = "For your security, we signed you out"
  val p1Expected = "We did not save your answers."
  val buttonExpectedText = "Sign in"
  val buttonExpectedUrl: String = "buttonUrl"


  val timeoutPage: TimeoutPage = app.injector.instanceOf[TimeoutPage]
  val appConfig: AppConfig = mockAppConfig


  "Timeout Page when called in English" should {

    "render the page correct" which {

      lazy val view: HtmlFormat.Appendable = timeoutPage(Call("GET", buttonExpectedUrl))(fakeRequest, messages, mockAppConfig)
      implicit lazy val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected)
      welshToggleCheck("English")
      h1Check(h1Expected, "xl")

      textOnPageCheck(p1Expected,Selectors.p1Selector)
      buttonCheck(buttonExpectedText, Selectors.buttonSelector)
      formGetLinkCheck(buttonExpectedUrl, Selectors.formSelector)

    }
  }

  "Timeout Page when called in Welsh" should {

    "render the page correct" which {

      lazy val view: HtmlFormat.Appendable = timeoutPage(Call("GET", buttonExpectedUrl))(fakeRequest, welshMessages, mockAppConfig)
      implicit lazy val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected)
      welshToggleCheck("Welsh")
      h1Check(h1Expected, "xl")

      textOnPageCheck(p1Expected,Selectors.p1Selector)
      buttonCheck(buttonExpectedText, Selectors.buttonSelector)
      formGetLinkCheck(buttonExpectedUrl, Selectors.formSelector)

    }
  }
}
