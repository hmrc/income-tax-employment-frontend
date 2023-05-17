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

package models.employment

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}

case class AdditionalInfoViewModel(additionalInfoModel: Seq[AdditionalInfoModel] = Seq.empty[AdditionalInfoModel]) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedAdditionalInfoViewModel =
    EncryptedAdditionalInfoViewModel(additionalInfoModel.map(_.encrypted))
}

object AdditionalInfoViewModel{
  implicit val formats: OFormat[AdditionalInfoViewModel] = Json.format[AdditionalInfoViewModel]
}

case class EncryptedAdditionalInfoViewModel(encryptedAdditionalInfoViewModel: Seq[EncryptedAdditionalInfoModel] = Seq.empty[EncryptedAdditionalInfoModel]) {
  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): AdditionalInfoViewModel = AdditionalInfoViewModel(
    additionalInfoModel = encryptedAdditionalInfoViewModel.map(_.decrypted)
  )
}

object EncryptedAdditionalInfoViewModel{
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val formats: OFormat[EncryptedAdditionalInfoModel] = Json.format[EncryptedAdditionalInfoModel]
}



case class AdditionalInfoModel(
                               lumpSumAmount: Option[BigDecimal] = None,
                               payrollAmount: Option[BigDecimal] = None
                               ) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedAdditionalInfoModel =
    EncryptedAdditionalInfoModel(
    encryptedLumpSumAmount = lumpSumAmount.map(_.encrypted),
    encryptedPayrollAmount = payrollAmount.map(_.encrypted)
  )


  def payrollHasPaidSome: Boolean = payrollAmount.getOrElse(BigDecimal(0)) > 0

  def payrollHasPaidAll: Boolean = payrollAmount.equals(lumpSumAmount)

}

object AdditionalInfoModel {
  implicit val formats: OFormat[AdditionalInfoModel] = Json.format[AdditionalInfoModel]
}


case class EncryptedAdditionalInfoModel(
                                             encryptedLumpSumAmount: Option[EncryptedValue] = None,
                                             encryptedPayrollAmount: Option[EncryptedValue] = None) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): AdditionalInfoModel = AdditionalInfoModel(
    lumpSumAmount = encryptedLumpSumAmount.map(_.decrypted[BigDecimal]),
    payrollAmount = encryptedPayrollAmount.map(_.decrypted[BigDecimal])
  )
}

object EncryptedAdditionalInfoModel {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val format: Format[EncryptedAdditionalInfoModel] = Json.format[EncryptedAdditionalInfoModel]
}