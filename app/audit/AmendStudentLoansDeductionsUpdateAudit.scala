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

import models.employment.Deductions
import play.api.libs.json.{JsNull, JsNumber, JsValue, Json, OWrites}
import utils.JsonUtils.jsonObjNoNulls

case class AmendStudentLoansDeductionsUpdateAudit(taxYear: Int,
                                                  userType: String,
                                                  nino: String,
                                                  mtditid: String,
                                                  priorStudentLoanDeductionsData: Option[Deductions],
                                                  studentLoanDeductionsData: Option[Deductions]) {

  private def name = "AmendStudentLoansDeductionsUpdate"

  def toAuditModel: AuditModel[AmendStudentLoansDeductionsUpdateAudit] = AuditModel(name, name, this)

}

object AmendStudentLoansDeductionsUpdateAudit {
  implicit def writes: OWrites[AmendStudentLoansDeductionsUpdateAudit] = (audit: AmendStudentLoansDeductionsUpdateAudit) => {

    Json.obj(
      "taxYear" -> audit.taxYear,
      "userType" -> audit.userType,
      "nino" -> audit.nino,
      "mtditid" -> audit.mtditid
    ).++(
      {
        val studentLoansPrior = audit.priorStudentLoanDeductionsData.flatMap(_.studentLoans)
        val uglDeductionAmount = studentLoansPrior.flatMap(_.uglDeductionAmount)
        val pglDeductionAmount = studentLoansPrior.flatMap(_.pglDeductionAmount)

        jsonObjNoNulls(
          "priorStudentLoanDeductionsData" ->
            jsonObjNoNulls(
              "studentLoans" ->
                jsonObjNoNulls(
                  "undergraduateLoanDeductionAmount" -> uglDeductionAmount.fold[JsValue](JsNull)(JsNumber),
                  "postgraduateLoanDeductionAmount" -> pglDeductionAmount.fold[JsValue](JsNull)(JsNumber)
                )
            )
        )
      }
    ).++(
      {
        val studentLoans = audit.studentLoanDeductionsData.flatMap(_.studentLoans)
        val uglDeductionAmount = studentLoans.flatMap(_.uglDeductionAmount)
        val pglDeductionAmount = studentLoans.flatMap(_.pglDeductionAmount)

        jsonObjNoNulls(
          "studentLoanDeductionsData" ->
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
  }
}
