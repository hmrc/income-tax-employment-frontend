/*
 * Copyright 2020 HM Revenue & Customs
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

package models.mongo

import models.employment.EmploymentDetailsView
import play.api.libs.json.{Json, OFormat}

case class EmploymentDetails(employerName: String,
                             employerRef: Option[String],
                             startDate: Option[String],
                             cessationDateQuestion: Option[Boolean],
                             cessationDate: Option[String],
                             dateIgnored: Option[String],
                             submittedOn: Option[String],
                             taxablePayToDate: Option[BigDecimal],
                             totalTaxToDate: Option[BigDecimal],
                             tipsAndOtherPaymentsQuestion: Option[Boolean],
                             tipsAndOtherPayments: Option[BigDecimal])

object EmploymentDetails {
  implicit val format: OFormat[EmploymentDetails] = Json.format[EmploymentDetails]
}

case class EmploymentBenefits()

object EmploymentBenefits {
  implicit val format: OFormat[EmploymentBenefits] = Json.format[EmploymentBenefits]
}

case class EmploymentExpenses()

object EmploymentExpenses {
  implicit val format: OFormat[EmploymentExpenses] = Json.format[EmploymentExpenses]
}

case class EmploymentCYAModel(employmentDetails: EmploymentDetails,
                              employmentBenefits: Option[EmploymentBenefits],
                              employmentExpenses: Option[EmploymentExpenses]){

  def toEmploymentDetailsView(employmentId: String, isUsingCustomerData: Boolean): EmploymentDetailsView = {
    EmploymentDetailsView(
      employmentDetails.employerName,
      employmentDetails.employerRef,
      employmentId,
      employmentDetails.startDate,
      Some(employmentDetails.cessationDate.isDefined),
      employmentDetails.cessationDate,
      employmentDetails.taxablePayToDate,
      employmentDetails.totalTaxToDate,
      employmentDetails.tipsAndOtherPaymentsQuestion,
      employmentDetails.tipsAndOtherPayments,
      isUsingCustomerData)
  }
}

object EmploymentCYAModel {
  implicit val format: OFormat[EmploymentCYAModel] = Json.format[EmploymentCYAModel]
}
