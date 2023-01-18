/*
 * Copyright 2023 HM Revenue & Customs
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

package models.employment

import models.benefits.Benefits
import models.expenses.Expenses
import play.api.libs.json.{JsNull, JsNumber, JsValue, OWrites}
import utils.JsonUtils.jsonObjNoNulls

case class DecodedDeleteEmploymentPayload(employmentData: EmploymentDetailsViewModel,
                                          benefits: Option[Benefits],
                                          expenses: Option[Expenses],
                                          deductions: Option[Deductions]) {

  def toNrsPayloadModel: DecodedDeleteEmploymentPayload = {
   val nrsExpenses = expenses.map(_.copy(
     businessTravelCosts = None,
     hotelAndMealExpenses = None,
     vehicleExpenses = None,
     mileageAllowanceRelief = None
   ))
    DecodedDeleteEmploymentPayload(employmentData, benefits, nrsExpenses, deductions)
  }
}

object DecodedDeleteEmploymentPayload {
  implicit def writes: OWrites[DecodedDeleteEmploymentPayload] = (audit: DecodedDeleteEmploymentPayload) => {

    jsonObjNoNulls(
      "employmentData" -> audit.employmentData
    ).++(
      jsonObjNoNulls(
        "benefits" -> audit.benefits
      ).++(
        jsonObjNoNulls(
          "expenses" -> jsonObjNoNulls(
            "jobExpenses" -> audit.expenses.map(_.jobExpenses),
            "flatRateJobExpenses" -> audit.expenses.map(_.flatRateJobExpenses),
            "professionalSubscriptions" -> audit.expenses.map(_.professionalSubscriptions),
            "otherAndCapitalAllowances" -> audit.expenses.map(_.otherAndCapitalAllowances)
          )
        )
      ).++(
        {
          val studentLoans = audit.deductions.flatMap(_.studentLoans)
          val uglDeductionAmount = studentLoans.flatMap(_.uglDeductionAmount)
          val pglDeductionAmount = studentLoans.flatMap(_.pglDeductionAmount)

          jsonObjNoNulls(
            "deductions" ->
              jsonObjNoNulls(
                "studentLoans" ->
                  jsonObjNoNulls(
                    "undergraduateLoanDeductionAmount" -> uglDeductionAmount.fold[JsValue](JsNull)(JsNumber),
                    "postgraduateLoanDeductionAmount" -> pglDeductionAmount.fold[JsValue](JsNull)(JsNumber)
                  )
              )
          )
        }
      )
    )
  }
}
