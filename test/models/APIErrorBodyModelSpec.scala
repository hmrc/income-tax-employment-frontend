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

package models

import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class APIErrorBodyModelSpec extends UnitTest {
  val model: APIErrorBodyModel = new APIErrorBodyModel(
    "SERVICE_UNAVAILABLE", "The service is currently unavailable")
  val jsModel: JsObject = Json.obj(
    "code" -> "SERVICE_UNAVAILABLE",
    "reason" -> "The service is currently unavailable"
  )

  val errorsJsModel: JsObject = Json.obj(
    "failures" -> Json.arr(
      Json.obj("code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "The service is currently unavailable"),
      Json.obj("code" -> "INTERNAL_SERVER_ERROR",
        "reason" -> "The service is currently facing issues.")
    )
  )

  "The APIErrorBodyModel" should {

    "parse to Json" in {
      Json.toJson(model) shouldBe jsModel
    }
    "parse from json" in {
      jsModel.as[APIErrorBodyModel]
    }
  }

  "The APIErrorModel" should {

    val model = APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("SERVICE_UNAVAILABLE","The service is currently unavailable"))
    val errorsModel = APIErrorModel(SERVICE_UNAVAILABLE, APIErrorsBodyModel(Seq(
      APIErrorBodyModel("SERVICE_UNAVAILABLE","The service is currently unavailable"),
      APIErrorBodyModel("INTERNAL_SERVER_ERROR","The service is currently facing issues.")
    )))

    "parse to Json" in {
      model.toJson shouldBe jsModel
    }
    "parse to Json for multiple errors" in {
      errorsModel.toJson shouldBe errorsJsModel
    }
  }

}