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

package models.mongo

import models.employment.{BenefitsViewModel, EmploymentDetailsViewModel, EmploymentSource}
import play.api.libs.json.{Json, OFormat}

case class EmploymentDetails(employerName: String,
                             employerRef: Option[String] = None,
                             startDate: Option[String] = None,
                             payrollId: Option[String] = None,
                             cessationDateQuestion: Option[Boolean] = None,
                             cessationDate: Option[String] = None,
                             dateIgnored: Option[String] = None,
                             employmentSubmittedOn: Option[String] = None,
                             employmentDetailsSubmittedOn: Option[String] = None,
                             taxablePayToDate: Option[BigDecimal] = None,
                             totalTaxToDate: Option[BigDecimal] = None,
                             currentDataIsHmrcHeld: Boolean) {

  val isFinished: Boolean = {

    val cessationSectionFinished = {
      cessationDateQuestion match {
        case Some(true) => true
        case Some(false) => cessationDate.isDefined
        case None => false
      }
    }

    employerRef.isDefined &&
      startDate.isDefined &&
      taxablePayToDate.isDefined &&
      totalTaxToDate.isDefined &&
      cessationSectionFinished
  }
}
object EmploymentDetails {
  implicit val format: OFormat[EmploymentDetails] = Json.format[EmploymentDetails]
}

case class EmploymentCYAModel(employmentDetails: EmploymentDetails,
                              employmentBenefits: Option[BenefitsViewModel] = None){

  def toEmploymentDetailsView(employmentId: String, isUsingCustomerData: Boolean): EmploymentDetailsViewModel = {
    EmploymentDetailsViewModel(
      employmentDetails.employerName,
      employmentDetails.employerRef,
      employmentId,
      employmentDetails.startDate,
      //TODO: could use the below for cessationDateQuestion when cessation date is implemented
      //cessationDateQuestion = Some(employmentDetails.cessationDate.isEmpty),
      employmentDetails.cessationDateQuestion,
      employmentDetails.cessationDate,
      employmentDetails.taxablePayToDate,
      employmentDetails.totalTaxToDate,
      employmentDetails.payrollId,
      isUsingCustomerData)
  }
}

object EmploymentCYAModel {
  implicit val format: OFormat[EmploymentCYAModel] = Json.format[EmploymentCYAModel]

  def apply(employmentSource: EmploymentSource, isUsingCustomerData: Boolean): EmploymentCYAModel = EmploymentCYAModel(
    employmentDetails = employmentSource.toEmploymentDetails(isUsingCustomerData),
    employmentBenefits = employmentSource.toBenefitsViewModel(isUsingCustomerData)
  )
}
