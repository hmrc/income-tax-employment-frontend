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
import play.api.libs.json.{Json, OWrites}

case class AmendStudentLoansDeductionsUpdateAudit(taxYear: Int,
                                                  userType: String,
                                                  nino: String,
                                                  mtditid: String,
                                                  priorStudentLoanDeductionsData: Deductions,
                                                  studentLoanDeductionsData: Deductions) {

  private def name = "AmendStudentLoansDeductionsUpdate"

  def toAuditModel: AuditModel[AmendStudentLoansDeductionsUpdateAudit] = AuditModel(name, name, this)

}

object AmendStudentLoansDeductionsUpdateAudit {
  implicit def writes: OWrites[AmendStudentLoansDeductionsUpdateAudit] = Json.writes[AmendStudentLoansDeductionsUpdateAudit]
}
