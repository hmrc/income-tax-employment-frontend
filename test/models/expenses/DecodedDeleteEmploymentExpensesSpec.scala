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

package models.expenses

import play.api.libs.json.{JsValue, Json}
import utils.UnitTest

class DecodedDeleteEmploymentExpensesSpec extends UnitTest {

  "writes" when {

    "passed a valid DeleteEmploymentExpenses model" should {

      "produce valid json" in {

        val validJson: JsValue = Json.parse(
          """{
            |    "expenses": {
            |      "jobExpenses": 100,
            |      "flatRateJobExpenses": 100,
            |      "professionalSubscriptions": 100,
            |      "otherAndCapitalAllowances": 100
            |    }
            |}""".stripMargin
        )

        val validModel: DecodedDeleteEmploymentExpensesPayload = DecodedDeleteEmploymentExpensesPayload(
          expenses = Some(Expenses(
            businessTravelCosts = Some(100),
            jobExpenses = Some(100),
            flatRateJobExpenses = Some(100),
            professionalSubscriptions = Some(100),
            hotelAndMealExpenses = Some(100),
            otherAndCapitalAllowances = Some(100),
            vehicleExpenses = Some(100),
            mileageAllowanceRelief = Some(100)
          ))
        ).toNrsPayloadModel

        Json.toJson(validModel) shouldBe validJson
      }
    }
  }

}
