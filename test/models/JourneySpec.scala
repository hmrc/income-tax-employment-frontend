/*
 * Copyright 2024 HM Revenue & Customs
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

package models

import models.Journey._
import play.api.libs.json.{JsSuccess, Json}
import support.UnitTest

class JourneySpec extends UnitTest {

  val journeyTypeList: Seq[Journey] = Seq(
    Employment
  )


  "Journey" should {
    "contain the correct values" in {
      Journey.values shouldEqual journeyTypeList
    }
  }

  "Journey values" should {
    "parse to and from json" in {
      val jsValues = Journey.values.filter(_.equals(Employment)).map(s => Json.toJson(s))

      jsValues.toList  shouldBe journeyTypeList.filter(_.equals(Employment)).map(s => Json.toJson(s))

      val results = jsValues.map(s => s.validate[Journey])
      results.toList shouldBe Seq(JsSuccess(Employment))
    }
  }
}
