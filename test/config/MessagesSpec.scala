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
    "betaBar.banner.message.1",
    "betaBar.banner.message.2",
    "betaBar.banner.message.3"
  )

  lazy val allLanguages: Map[String, Map[String, String]] = app.injector.instanceOf[MessagesApi].messages

  val defaults = allLanguages("default")
  val welsh = allLanguages("cy")


  "the messages file must have welsh translations" should {
    "check all keys in the default file other than those in the exclusion list has a corresponding translation" in {
      defaults.keys.foreach(
        key =>
          if (!exclusionKeys.contains(key))
          {welsh.keys should contain(key)}
      )
    }
  }

  "the english messages file" should {
    "have no duplicate messages(values)" in {
      checkMessagesAreUnique(defaults, exclusionKeys)

    }
  }

  "the welsh messages file" should {
    "have no duplicate messages(values)" in {

//      val x = defaults.filterNot(x => welsh.contains(x._1)).toSeq.sortBy(_._1)
//
//      x.map{
//        println(_)
//      }
//
//      val y = welsh.filterNot(x => defaults.contains(x._1)).toSeq.sortBy(_._1)
//
//      y.map{
//        println(_)
//      }

      val xx: Seq[(String, String)] = welsh.toSeq.sortBy(_._2)
      val yy = defaults.toSeq.sortBy(_._1).toMap.keys.toSeq.sorted

//      (0 to 100) map {
//        x =>
//          println("welsh " +  xx(x) + " : english " + yy(x))
//
//
//      }

//      val xxx = welsh.toSeq.sortBy(_._2).toMap.values.toSeq.sorted
      val yyy = defaults.toSeq.sortBy(_._2).toMap.values.toSeq.sorted

      (0 to 100) map {
        x =>
          println(s"welsh ${xx(x)._2}")
//          println("english " +  yyy(x) + " : " + yy(x))
      }

      checkMessagesAreUnique(welsh, exclusionKeys)
    }
  }
}
