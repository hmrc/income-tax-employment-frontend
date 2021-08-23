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

package models.employment

import models.employment.createUpdate.CreateUpdatePay
import play.api.libs.json.{Json, OFormat}

case class EmploymentData(submittedOn: String,
                          employmentSequenceNumber: Option[String],
                          companyDirector: Option[Boolean],
                          closeCompany: Option[Boolean],
                          directorshipCeasedDate: Option[String],
                          occPen: Option[Boolean],
                          disguisedRemuneration: Option[Boolean],
                          pay: Option[Pay],
                          deductions: Option[Deductions]
                         ){

  def dataHasNotChanged(createUpdatePay: CreateUpdatePay): Boolean = {
    pay.flatMap(_.taxablePayToDate).contains(createUpdatePay.taxablePayToDate) &&
    pay.flatMap(_.totalTaxToDate).contains(createUpdatePay.totalTaxToDate)
  }
}

object EmploymentData {
  implicit val formats: OFormat[EmploymentData] = Json.format[EmploymentData]
}
