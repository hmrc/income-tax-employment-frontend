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

package models.employment.createUpdate

import models.employment.{Benefits, Deductions}
import play.api.libs.json.{Json, OFormat}

case class CreateUpdateEmploymentRequest(employmentId: Option[String] = None,
                                         employment: Option[CreateUpdateEmployment] = None,
                                         employmentData: Option[CreateUpdateEmploymentData] = None,
                                         hmrcEmploymentIdToIgnore: Option[String] = None)

object CreateUpdateEmploymentRequest {
  implicit val formats: OFormat[CreateUpdateEmploymentRequest] = Json.format[CreateUpdateEmploymentRequest]
}

case class CreateUpdateEmployment(employerRef: Option[String],
                                  employerName: String,
                                  startDate: String,
                                  cessationDate: Option[String] = None,
                                  payrollId: Option[String] = None)

object CreateUpdateEmployment {
  implicit val formats: OFormat[CreateUpdateEmployment] = Json.format[CreateUpdateEmployment]
}

case class CreateUpdateEmploymentData(pay: CreateUpdatePay,
                                      deductions: Option[Deductions] = None,
                                      benefitsInKind: Option[Benefits] = None)

object CreateUpdateEmploymentData {
  implicit val formats: OFormat[CreateUpdateEmploymentData] = Json.format[CreateUpdateEmploymentData]
}

case class CreateUpdatePay(taxablePayToDate: BigDecimal,
                           totalTaxToDate: BigDecimal,
                           tipsAndOtherPayments: Option[BigDecimal])

object CreateUpdatePay {
  implicit val formats: OFormat[CreateUpdatePay] = Json.format[CreateUpdatePay]
}
