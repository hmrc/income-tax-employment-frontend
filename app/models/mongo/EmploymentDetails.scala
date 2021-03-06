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

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class EmploymentDetails(employerName: String,
                             employerRef: Option[String] = None,
                             startDate: Option[String] = None,
                             payrollId: Option[String] = None,
                             didYouLeaveQuestion: Option[Boolean] = None,
                             cessationDate: Option[String] = None,
                             dateIgnored: Option[String] = None,
                             employmentSubmittedOn: Option[String] = None,
                             employmentDetailsSubmittedOn: Option[String] = None,
                             taxablePayToDate: Option[BigDecimal] = None,
                             totalTaxToDate: Option[BigDecimal] = None,
                             currentDataIsHmrcHeld: Boolean) {

  val isSubmittable: Boolean =
    employerRef.isDefined &&
      startDate.isDefined &&
      payrollId.isDefined &&
      taxablePayToDate.isDefined &&
      totalTaxToDate.isDefined

  val isFinished: Boolean = {
    val cessationSectionFinished = didYouLeaveQuestion match {
      case Some(true) => cessationDate.isDefined
      case Some(false) => true
      case None => false
    }

    employerRef.isDefined &&
      startDate.isDefined &&
      payrollId.isDefined &&
      taxablePayToDate.isDefined &&
      totalTaxToDate.isDefined &&
      cessationSectionFinished
  }
}

object EmploymentDetails {
  implicit val format: OFormat[EmploymentDetails] = Json.format[EmploymentDetails]
}

case class EncryptedEmploymentDetails(employerName: EncryptedValue,
                                      employerRef: Option[EncryptedValue] = None,
                                      startDate: Option[EncryptedValue] = None,
                                      payrollId: Option[EncryptedValue] = None,
                                      didYouLeaveQuestion: Option[EncryptedValue] = None,
                                      cessationDate: Option[EncryptedValue] = None,
                                      dateIgnored: Option[EncryptedValue] = None,
                                      employmentSubmittedOn: Option[EncryptedValue] = None,
                                      employmentDetailsSubmittedOn: Option[EncryptedValue] = None,
                                      taxablePayToDate: Option[EncryptedValue] = None,
                                      totalTaxToDate: Option[EncryptedValue] = None,
                                      currentDataIsHmrcHeld: EncryptedValue)

object EncryptedEmploymentDetails {
  implicit val format: OFormat[EncryptedEmploymentDetails] = Json.format[EncryptedEmploymentDetails]
}
