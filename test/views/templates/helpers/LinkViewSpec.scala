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

package views.templates.helpers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.ViewTest
import views.html.templates.helpers.Link

class LinkViewSpec extends ViewTest {

  lazy val linkView: Link = app.injector.instanceOf[Link]

  private val aTagSelector = "a"

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
