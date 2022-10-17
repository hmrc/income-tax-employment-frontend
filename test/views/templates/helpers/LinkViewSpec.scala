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

package views.templates.helpers

import common.SessionValues
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import support.{TaxYearProvider, UnitTest, ViewHelper}
import views.html.templates.helpers.Link

class LinkViewSpec extends UnitTest
  with ViewHelper
  with GuiceOneAppPerSuite
  with TaxYearProvider {

  lazy val linkView: Link = app.injector.instanceOf[Link]

  private val aTagSelector = "a"

  lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy val messages: Messages = messagesApi.preferred(fakeRequest.withHeaders())

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")

  "LinkView" when {
    val link = "/contact/some-system"
    val message = "Get help with this page"
    val id = Some("tag-id")
    val isExternal = true
    val opensInNewTab = "(opens in new tab)"

    "provided with only the link and message parameters, linkView" should {

      "generate <a> tag" which {
        lazy val view = linkView(link, message)(messages)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        "contains the correct href value" in {
          element(aTagSelector).attr("href") shouldBe link
        }

        "contains the correct message value" in {
          element(aTagSelector).text shouldBe message
        }

        "does not contain an id value" in {
          element(aTagSelector).attr("id") shouldBe ""
        }

        "does not contain a _target value" in {
          element(aTagSelector).attr("_target") shouldBe ""
        }
      }
    }

    "provided with the link, message, id and isExternal parameters, linkView" should {

      "generate <a> tag" which {
        lazy val view = linkView(link, message, id, isExternal)(messages)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        "contains the correct href value" in {
          element(aTagSelector).attr("href") shouldBe link
        }

        "contains the correct message value" in {
          element(aTagSelector).text shouldBe message + " " + opensInNewTab
        }

        "contains an id value" in {
          element(aTagSelector).attr("id") shouldBe id.get
        }

        "contains a _target value" in {
          element(aTagSelector).attr("target") shouldBe "_blank"
        }
      }
    }

  }

}
