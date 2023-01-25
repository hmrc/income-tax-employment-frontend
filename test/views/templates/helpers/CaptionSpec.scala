/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.i18n.Messages
import support.ViewUnitTest
import views.html.templates.helpers.Caption

class CaptionSpec extends ViewUnitTest {

  override protected val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val underTest = inject[Caption]

  "Caption template" should {
    implicit val messages: Messages = getMessages(false)

    val caption = "caption"
    val size = "s"

    "show the span with the caption" in {
      val htmlFormat = underTest(caption, size)
      val document: Document = Jsoup.parse(htmlFormat.body)
      val documentCaption = document.select(s"span.govuk-caption-$size").text().trim

      documentCaption shouldBe caption
    }
  }
}
