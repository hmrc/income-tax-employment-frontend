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

package models

import play.api.libs.json.{Json, OFormat}

case class GetEmploymentDataModel(
                                   submittedOn: String,
                                   source: Option[String],
                                   customerAdded: Option[String],
                                   dateIgnored: Option[String],
                                   employment: EmploymentModel
                                 )

object GetEmploymentDataModel {
  implicit val formats: OFormat[GetEmploymentDataModel] = Json.format[GetEmploymentDataModel]
}

case class EmploymentModel(
                            employmentSequenceNumber: Option[String],
                            payrollId: Option[String],
                            companyDirector: Option[Boolean],
                            closeCompany: Option[Boolean],
                            directorshipCeasedDate: Option[String],
                            startDate: Option[String],
                            cessationDate: Option[String],
                            occPen: Option[Boolean],
                            disguisedRemuneration: Option[Boolean],
                            employer: EmployerModel,
                            pay: PayModel
                          )

object EmploymentModel {
  implicit val formats: OFormat[EmploymentModel] = Json.format[EmploymentModel]
}

case class EmployerModel(
                          employerRef: Option[String],
                          employerName: String
                        )

object EmployerModel {
  implicit val formats: OFormat[EmployerModel] = Json.format[EmployerModel]
}

case class PayModel(
                     taxablePayToDate: BigDecimal,
                     totalTaxToDate: BigDecimal,
                     tipsAndOtherPayments: Option[BigDecimal],
                     payFrequency: String,
                     paymentDate: String,
                     taxWeekNo: Option[Int],
                     taxMonthNo: Option[Int]
                   )

object PayModel {
  implicit val formats: OFormat[PayModel] = Json.format[PayModel]
}
