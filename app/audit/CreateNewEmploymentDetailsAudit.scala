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

import play.api.libs.json.{Json, OWrites}

case class AuditNewEmploymentData(employerName: Option[String],
                                  employerRef: Option[String],
                                  startDate: Option[String],
                                  cessationDate: Option[String],
                                  taxablePayToDate: Option[BigDecimal],
                                  totalTaxToDate: Option[BigDecimal],
                                  payrollId: Option[String])

object AuditNewEmploymentData {
  implicit def writes: OWrites[AuditNewEmploymentData] = Json.writes[AuditNewEmploymentData]
}

case class PriorEmploymentAuditInfo(employerName: String, employerRef: Option[String])

object PriorEmploymentAuditInfo {
  implicit def writes: OWrites[PriorEmploymentAuditInfo] = Json.writes[PriorEmploymentAuditInfo]
}

case class CreateNewEmploymentDetailsAudit(taxYear: Int,
                                           userType: String,
                                           nino: String,
                                           mtditid: String,
                                           employmentData: AuditNewEmploymentData,
                                           existingEmployments: Seq[PriorEmploymentAuditInfo]) {

  def toAuditModel: AuditModel[CreateNewEmploymentDetailsAudit] = AuditModel(name, name, this)

  private def name = "CreateNewEmploymentDetails"
}

object CreateNewEmploymentDetailsAudit {
  implicit def writes: OWrites[CreateNewEmploymentDetailsAudit] = Json.writes[CreateNewEmploymentDetailsAudit]
}
