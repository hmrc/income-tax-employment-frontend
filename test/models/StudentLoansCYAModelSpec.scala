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

import models.employment.StudentLoansCYAModel
import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class StudentLoansCYAModelSpec extends UnitTest{

  val validModel: StudentLoansCYAModel = StudentLoansCYAModel(
    uglDeduction = true,
    uglDeductionAmount = Some(500.00),
    pglDeduction = true,
    pglDeductionAmount = Some(500.00)
  )

  val validJson: JsObject = Json.obj(
    "uglDeduction" -> true,
    "uglDeductionAmount" -> 500.00,
    "pglDeduction" -> true,
    "pglDeductionAmount" -> 500.00
  )

  "StudentLoansCYAModel" should {

    "parse from json" in {
      validJson.as[StudentLoansCYAModel] shouldBe validModel
    }

    "parse to json" in {
      Json.toJson(validModel) shouldBe validJson
    }

  }

}
