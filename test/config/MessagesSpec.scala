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

package config

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import utils.ViewTest

class MessagesSpec extends ViewTest with GuiceOneAppPerSuite {

  val exclusionKeys = Set(
    "global.error.badRequest400.title",
    "global.error.badRequest400.heading",
    "global.error.badRequest400.message",
    "global.error.pageNotFound404.title",
    "global.error.pageNotFound404.heading",
    "global.error.pageNotFound404.message",
    "global.error.InternalServerError500.title",
    "global.error.InternalServerError500.heading",
    "global.error.InternalServerError500.message",
    "global.error.fallbackClientError4xx.title",
    "global.error.fallbackClientError4xx.heading",
    "global.error.fallbackClientError4xx.message",
    "internal-server-error-template.heading",
    "internal-server-error-template.paragraph.1",
    "betaBar.banner.message.1",
    "betaBar.banner.message.2",
    "betaBar.banner.message.3",
    "language.day.plural",
    "language.day.singular",
    "back.text",
    "this.section.is",
    "radios.yesnoitems.yes",
    "radios.yesnoitems.no",
  )

  lazy val allLanguages: Map[String, Map[String, String]] = app.injector.instanceOf[MessagesApi].messages

  val defaults = allLanguages("default")
  val welsh = allLanguages("cy")


  "the messages file must have welsh translations" should {
    "check all keys in the default file other than those in the exclusion list has a corresponding translation" in {
      defaults.keys.foreach(
        key =>
          if (!exclusionKeys.contains(key)) {
            welsh.keys should contain(key)
          }
      )
    }
  }

  "the english messages file" should {
    "have no duplicate messages(values)" in {
      val messages: List[(String, String)] = defaults.filter(entry => !exclusionKeys.contains(entry._1)).toList

      val result = checkMessagesAreUnique(messages, messages, Set())

      result shouldBe Set()
    }
  }

  "the welsh messages file" should {
    "have no duplicate messages(values)" in {
      val messages: List[(String, String)] = welsh.filter(entry => !exclusionKeys.contains(entry._1)).toList

      val result = checkMessagesAreUnique(messages, messages, Set())

      result shouldBe Set()
    }
  }
}
