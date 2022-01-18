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

package models.mongo

import models.benefits.{BenefitsViewModel, EncryptedBenefitsViewModel}
import models.employment.{EmploymentDetailsViewModel, EmploymentSource, EncryptedStudentLoansCYAModel, StudentLoansCYAModel}
import play.api.libs.json.{Json, OFormat}

case class EmploymentCYAModel(employmentDetails: EmploymentDetails,
                              employmentBenefits: Option[BenefitsViewModel] = None,
                              studentLoansCYAModel: Option[StudentLoansCYAModel] = None) {

  def toEmploymentDetailsView(employmentId: String, isUsingCustomerData: Boolean): EmploymentDetailsViewModel = {
    EmploymentDetailsViewModel(
      employmentDetails.employerName,
      employmentDetails.employerRef,
      employmentDetails.payrollId,
      employmentId,
      employmentDetails.startDate,
      employmentDetails.cessationDateQuestion,
      employmentDetails.cessationDate,
      employmentDetails.taxablePayToDate,
      employmentDetails.totalTaxToDate,
      isUsingCustomerData)
  }
}

object EmploymentCYAModel {
  implicit val format: OFormat[EmploymentCYAModel] = Json.format[EmploymentCYAModel]

  def apply(employmentSource: EmploymentSource, isUsingCustomerData: Boolean): EmploymentCYAModel = EmploymentCYAModel(
    employmentDetails = employmentSource.toEmploymentDetails(isUsingCustomerData),
    employmentBenefits = employmentSource.toBenefitsViewModel(isUsingCustomerData),
    studentLoansCYAModel = employmentSource.toStudentLoansCYAModel
  )
}

case class EncryptedEmploymentCYAModel(employmentDetails: EncryptedEmploymentDetails,
                                       employmentBenefits: Option[EncryptedBenefitsViewModel] = None,
                                       studentLoansCYAModel: Option[EncryptedStudentLoansCYAModel])

object EncryptedEmploymentCYAModel {
  implicit val format: OFormat[EncryptedEmploymentCYAModel] = Json.format[EncryptedEmploymentCYAModel]
}
