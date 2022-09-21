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

package audit

import models.expenses.Expenses
import play.api.libs.json.{JsValue, Json}
import support.{UnitTest, TaxYearHelper}

class ViewEmploymentExpensesSpec extends UnitTest with TaxYearHelper {

  "writes" when {
    "passed a ViewEmploymentExpensesModel" should {
      "produce a valid json" in {
        val auditJson: JsValue = Json.parse(
          s"""{
             | "taxYear": $taxYear,
             | "userType": "individual",
             | "nino": "AA12343AA",
             | "mtditid": "mtditid",
             | "expenses": {
             |   "jobExpenses": 150,
             |   "flatRateJobExpenses": 200,
             |   "professionalSubscriptions": 250,
             |   "otherAndCapitalAllowances": 350
             | }
             |}""".stripMargin)

        val auditModel: ViewEmploymentExpensesAudit = ViewEmploymentExpensesAudit(
          taxYear,
          "individual",
          "AA12343AA",
          "mtditid",
          Expenses(
            businessTravelCosts = Some(100.00),
            jobExpenses = Some(150.00),
            flatRateJobExpenses = Some(200.00),
            professionalSubscriptions = Some(250.00),
            hotelAndMealExpenses = Some(300.00),
            otherAndCapitalAllowances = Some(350.00),
            vehicleExpenses = Some(400.00),
            mileageAllowanceRelief = Some(450.00)
          )
        ).toAuditModel.detail

        Json.toJson(auditModel) shouldBe auditJson
      }
    }
  }
}
