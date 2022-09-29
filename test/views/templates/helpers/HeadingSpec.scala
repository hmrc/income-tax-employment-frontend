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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import support.ViewUnitTest
import views.html.templates.helpers.Heading

class HeadingSpec extends ViewUnitTest {

  override protected val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val underTest = inject[Heading]

  "Heading template" should {
    implicit val messages: Messages = getMessages(false)

    val heading = "heading"
    val caption = "caption"
    val size = "s"

    "show the caption before the heading" in {
      val htmlFormat = underTest(heading, Some(caption), size = size)
      val document: Document = Jsoup.parse(htmlFormat.body)
      val headingAndCaption = document.select(s"h1.govuk-heading-$size").text().trim

      headingAndCaption.startsWith(caption) shouldBe true
      headingAndCaption.endsWith(heading) shouldBe true
    }

    "show only the heading when no caption is provided" in {
      val htmlFormat = underTest(heading, None, size = size)
      val document: Document = Jsoup.parse(htmlFormat.body)
      val documentHeading = document.select(s"h1.govuk-heading-$size").text().trim

      documentHeading shouldBe heading
    }

    "add the extra classes in the h1" in {
      val extraClass = "extra-class"
      val htmlFormat = underTest(heading, Some(caption), extraClasses = extraClass, size = size)
      val document: Document = Jsoup.parse(htmlFormat.body)

      val headingAndCaption = document.select(s"h1.govuk-heading-$size")

      headingAndCaption.hasClass(extraClass) shouldBe true
    }
  }
}
