/*
 * Copyright 2023 HM Revenue & Customs
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

package models.details

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}

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

  lazy val isSubmittable: Boolean =
    startDate.isDefined &&
      taxablePayToDate.isDefined &&
      totalTaxToDate.isDefined

  lazy val isFinished: Boolean = {
    val cessationSectionFinished = didYouLeaveQuestion match {
      case Some(true) => cessationDate.isDefined
      case Some(false) => true
      case None => false
    }

    startDate.isDefined &&
      taxablePayToDate.isDefined &&
      totalTaxToDate.isDefined &&
      cessationSectionFinished
  }

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedEmploymentDetails = EncryptedEmploymentDetails(
    employerName = employerName.encrypted,
    employerRef = employerRef.map(_.encrypted),
    startDate = startDate.map(_.encrypted),
    payrollId = payrollId.map(_.encrypted),
    didYouLeaveQuestion = didYouLeaveQuestion.map(_.encrypted),
    cessationDate = cessationDate.map(_.encrypted),
    dateIgnored = dateIgnored.map(_.encrypted),
    employmentSubmittedOn = employmentSubmittedOn.map(_.encrypted),
    employmentDetailsSubmittedOn = employmentDetailsSubmittedOn.map(_.encrypted),
    taxablePayToDate = taxablePayToDate.map(_.encrypted),
    totalTaxToDate = totalTaxToDate.map(_.encrypted),
    currentDataIsHmrcHeld = currentDataIsHmrcHeld.encrypted
  )
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
                                      currentDataIsHmrcHeld: EncryptedValue) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EmploymentDetails = EmploymentDetails(
    employerName = employerName.decrypted[String],
    employerRef = employerRef.map(_.decrypted[String]),
    startDate = startDate.map(_.decrypted[String]),
    payrollId = payrollId.map(_.decrypted[String]),
    didYouLeaveQuestion = didYouLeaveQuestion.map(_.decrypted[Boolean]),
    cessationDate = cessationDate.map(_.decrypted[String]),
    dateIgnored = dateIgnored.map(_.decrypted[String]),
    employmentSubmittedOn = employmentSubmittedOn.map(_.decrypted[String]),
    employmentDetailsSubmittedOn = employmentDetailsSubmittedOn.map(_.decrypted[String]),
    taxablePayToDate = taxablePayToDate.map(_.decrypted[BigDecimal]),
    totalTaxToDate = totalTaxToDate.map(_.decrypted[BigDecimal]),
    currentDataIsHmrcHeld = currentDataIsHmrcHeld.decrypted[Boolean]
  )
}

object EncryptedEmploymentDetails {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedEmploymentDetails] = Json.format[EncryptedEmploymentDetails]
}
