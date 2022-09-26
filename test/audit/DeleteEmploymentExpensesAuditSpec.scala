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
import support.{TaxYearProvider, UnitTest}

class DeleteEmploymentExpensesAuditSpec extends UnitTest with TaxYearProvider {

  val fullJson: JsValue = Json.parse(
    s"""{
       |    "taxYear": $taxYearEOY,
       |    "userType": "individual",
       |    "nino": "AA12343AA",
       |    "mtditid": "mtditid",
       |    "expenses": {
       |      "jobExpenses": 100.01,
       |      "flatRateJobExpenses": 200.01,
       |      "professionalSubscriptions": 300.03,
       |      "otherAndCapitalAllowances": 400.04
       |    }
       |}""".stripMargin
  )

  val fullAuditModel: DeleteEmploymentExpensesAudit = DeleteEmploymentExpensesAudit(
    taxYearEOY, "individual", "AA12343AA", "mtditid",
    Expenses(
      businessTravelCosts = None,
      jobExpenses = Some(100.01),
      flatRateJobExpenses = Some(200.01),
      professionalSubscriptions = Some(300.03),
      hotelAndMealExpenses = None,
      otherAndCapitalAllowances = Some(400.04),
      vehicleExpenses = None,
      mileageAllowanceRelief = None
    )
  )

  val fullAuditModelWithExtraFields: DeleteEmploymentExpensesAudit = DeleteEmploymentExpensesAudit(
    taxYearEOY, "individual", "AA12343AA", "mtditid",
    Expenses(
      businessTravelCosts = Some(400),
      jobExpenses = Some(100.01),
      flatRateJobExpenses = Some(200.01),
      professionalSubscriptions = Some(300.03),
      hotelAndMealExpenses = Some(400),
      otherAndCapitalAllowances = Some(400.04),
      vehicleExpenses = Some(400),
      mileageAllowanceRelief = Some(400)
    )
  )

  val jsonWithSomeFieldsUndefined: JsValue = Json.parse(
    s"""{
       |    "taxYear": $taxYearEOY,
       |    "userType": "individual",
       |    "nino": "AA12343AA",
       |    "mtditid": "mtditid",
       |    "expenses": {
       |      "otherAndCapitalAllowances": 400.04
       |    }
       |}""".stripMargin
  )

  val auditModelWithSomeFieldsUndefined: DeleteEmploymentExpensesAudit = DeleteEmploymentExpensesAudit(
    taxYearEOY, "individual", "AA12343AA", "mtditid",
    Expenses(
      otherAndCapitalAllowances = Some(400.04)
    )
  )


  "writes" when {
    "passed a DeleteEmploymentExpensesAudit model" should {
      "produce full valid json" in {
        Json.toJson(fullAuditModel) shouldBe fullJson
      }
      "omit any undefined fields" in {
        Json.toJson(auditModelWithSomeFieldsUndefined) shouldBe jsonWithSomeFieldsUndefined
      }
    }
  }

  "toAuditModel" should {
    "return correct DeleteEmploymentExpensesAudit model when passed additional expenses fields" in {
      fullAuditModelWithExtraFields.toAuditModel.detail shouldBe fullAuditModel
    }
  }
}
