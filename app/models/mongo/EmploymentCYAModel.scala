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

package models.mongo

import models.benefits.{BenefitsViewModel, EncryptedBenefitsViewModel}
import models.details.{EmploymentDetails, EncryptedEmploymentDetails}
import models.employment._
import models.otheremployment.api.OtherEmploymentIncome
import models.otheremployment.session.{EncryptedOtherEmploymentIncomeCYAModel, OtherEmploymentIncomeCYAModel}
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

case class EmploymentCYAModel(employmentDetails: EmploymentDetails,
                              employmentBenefits: Option[BenefitsViewModel] = None,
                              studentLoans: Option[StudentLoansCYAModel] = None,
                              otherEmploymentIncome: Option[OtherEmploymentIncomeCYAModel] = None) {

  def toEmploymentDetailsView(employmentId: String, isUsingCustomerData: Boolean): EmploymentDetailsViewModel = EmploymentDetailsViewModel(
    employmentDetails.employerName,
    employmentDetails.employerRef,
    employmentDetails.payrollId,
    employmentId,
    employmentDetails.startDate,
    employmentDetails.didYouLeaveQuestion,
    employmentDetails.cessationDate,
    employmentDetails.taxablePayToDate,
    employmentDetails.totalTaxToDate,
    isUsingCustomerData)

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedEmploymentCYAModel = EncryptedEmploymentCYAModel(
    employmentDetails = employmentDetails.encrypted,
    employmentBenefits = employmentBenefits.map(_.encrypted),
    studentLoansCYAModel = studentLoans.map(_.encrypted),
    otherEmploymentIncome = otherEmploymentIncome.map(_.encrypted)
  )
}

object EmploymentCYAModel {
  implicit val format: OFormat[EmploymentCYAModel] = Json.format[EmploymentCYAModel]

  def apply(employmentSource: EmploymentSource,
            isUsingCustomerData: Boolean,
            otherEmploymentIncome: Option[OtherEmploymentIncome]): EmploymentCYAModel = {
    val employmentDetails = employmentSource.toEmploymentDetails(isUsingCustomerData)
    val otherEmploymentCYAModel = for {
      otherEmploymentIncome <- otherEmploymentIncome
      employerRef <- employmentDetails.employerRef
    } yield OtherEmploymentIncomeCYAModel(otherEmploymentIncome, employerRef)

    EmploymentCYAModel(
      employmentDetails = employmentDetails,
      employmentBenefits = employmentSource.toBenefitsViewModel(isUsingCustomerData),
      studentLoans = employmentSource.toStudentLoansCYAModel,
      otherEmploymentIncome = otherEmploymentCYAModel
    )
  }
}

case class EncryptedEmploymentCYAModel(employmentDetails: EncryptedEmploymentDetails,
                                       employmentBenefits: Option[EncryptedBenefitsViewModel] = None,
                                       studentLoansCYAModel: Option[EncryptedStudentLoansCYAModel] = None,
                                       otherEmploymentIncome: Option[EncryptedOtherEmploymentIncomeCYAModel] = None) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EmploymentCYAModel = EmploymentCYAModel(
    employmentDetails = employmentDetails.decrypted,
    employmentBenefits = employmentBenefits.map(_.decrypted),
    studentLoans = studentLoansCYAModel.map(_.decrypted),
    otherEmploymentIncome = otherEmploymentIncome.map(_.decrypted)
  )
}

object EncryptedEmploymentCYAModel {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val format: Format[EncryptedEmploymentCYAModel] = Json.format[EncryptedEmploymentCYAModel]
}
