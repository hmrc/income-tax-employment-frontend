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

package views.errors

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import utils.ViewTest
import views.html.authErrorPages.IndividualUnauthorisedView

class IndividualUnauthorisedViewSpec extends ViewTest {

  def view: IndividualUnauthorisedView = app.injector.instanceOf[IndividualUnauthorisedView]

  val validTitle: String = "You cannot view this page"
  val pageContent: String = "You need to sign up for Making Tax Digital for Income Tax before you can view this page."
  val linkContent: String = "sign up for Making Tax Digital for Income Tax"
  val linkHref: String = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax"

  val paragraphSelector: String = ".govuk-body"
  val linkSelector: String = paragraphSelector + " > a"

  val individualUnauthorisedView: IndividualUnauthorisedView = app.injector.instanceOf[IndividualUnauthorisedView]

  "IndividualUnauthorisedView in English" should {

    "render the page" which {
      lazy val view: Html = individualUnauthorisedView()(fakeRequest, messages, mockAppConfig)
      implicit lazy val document: Document = Jsoup.parse(view.body)

      titleCheck(validTitle)
      welshToggleCheck("English")
      h1Check(validTitle, "xl")
      textOnPageCheck(pageContent, paragraphSelector)
      linkCheck(linkContent, linkSelector, linkHref)
    }

  }

  "IndividualUnauthorisedView in Welsh" should {

    "render the page" which {
      lazy val view: Html = individualUnauthorisedView()(fakeRequest, welshMessages, mockAppConfig)
      implicit lazy val document: Document = Jsoup.parse(view.body)

      titleCheck(validTitle)
      welshToggleCheck("Welsh")
      h1Check(validTitle, "xl")
      textOnPageCheck(pageContent, paragraphSelector)
      linkCheck(linkContent, linkSelector, linkHref)
    }

  }
}
