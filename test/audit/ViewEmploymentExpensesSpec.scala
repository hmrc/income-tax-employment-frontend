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

package audit

import models.employment.Expenses
import play.api.libs.json.Json
import utils.UnitTestWithApp

class ViewEmploymentExpensesSpec extends UnitTestWithApp {

  "writes" when {
    "passed a ViewEmploymentExpensesModel" should {
      "produce a valid json" in {
        val json = Json.parse(
          """{
            | "taxYear": 2020,
            | "userType": "individual",
            | "nino": "AA12343AA",
            | "mtditid": "mtditid",
            | "expenses": {
            |   "businessTravelCosts": 100,
            |   "jobExpenses": 150,
            |   "flatRateJobExpenses": 200,
            |   "professionalSubscriptions": 250,
            |   "hotelAndMealExpenses": 300,
            |   "otherAndCapitalAllowances": 350,
            |   "vehicleExpenses": 400,
            |   "mileageAllowanceRelief": 450
            | }
            |}""".stripMargin)

        val auditModel = ViewEmploymentExpensesAudit(2020, "individual", "AA12343AA", "mtditid",
          Expenses(
            businessTravelCosts = Some(100),
            jobExpenses = Some(150),
            flatRateJobExpenses = Some(200),
            professionalSubscriptions = Some(250),
            hotelAndMealExpenses = Some(300),
            otherAndCapitalAllowances = Some(350),
            vehicleExpenses = Some(400),
            mileageAllowanceRelief = Some(450)
          )
        )

        Json.toJson(auditModel) shouldBe json
      }
    }
  }

}
