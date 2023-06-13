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

case class TaxableLumpSumViewModel(items: Seq[TaxableLumpSumItemModel] = Seq.empty[TaxableLumpSumItemModel]) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedTaxableLumpSumViewModel =
    EncryptedTaxableLumpSumViewModel(items.map(_.encrypted))
}

object TaxableLumpSumViewModel{
  implicit val formatSeq: OFormat[TaxableLumpSumItemModel] = Json.format[TaxableLumpSumItemModel]
  implicit val format: OFormat[TaxableLumpSumViewModel] = Json.format[TaxableLumpSumViewModel]}

case class EncryptedTaxableLumpSumViewModel(encryptedAdditionalInfoViewModel: Seq[EncryptedTaxableLumpSumItemModel]
                                            = Seq.empty[EncryptedTaxableLumpSumItemModel]) {
  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): TaxableLumpSumViewModel = TaxableLumpSumViewModel(
    items = encryptedAdditionalInfoViewModel.map(_.decrypted)
  )
}

object EncryptedTaxableLumpSumViewModel{
  implicit val formatEnc: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val formatSeq: OFormat[EncryptedTaxableLumpSumItemModel] = Json.format[EncryptedTaxableLumpSumItemModel]
  implicit val format: OFormat[EncryptedTaxableLumpSumViewModel] = Json.format[EncryptedTaxableLumpSumViewModel]
  implicit val formatPayrollType: Format[EncryptedPayrollPaymentType] = Json.format[EncryptedPayrollPaymentType]
}



case class TaxableLumpSumItemModel(
                               lumpSumAmount: Option[BigDecimal] = None,
                               payrollAmount: Option[BigDecimal] = None,
                               payrollHasPaidNoneSomeAll: Option[PayrollPaymentType] = None
                               ) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedTaxableLumpSumItemModel =
    EncryptedTaxableLumpSumItemModel(
    encryptedLumpSumAmount = lumpSumAmount.map(_.encrypted),
    encryptedPayrollAmount = payrollAmount.map(_.encrypted),
    encryptedPayrollHasPaidNoneSomeAll = payrollHasPaidNoneSomeAll.map(_.encrypted)
  )

  def validatePayrollHasPaidNoneSomeAll: Boolean = {
    payrollHasPaidNoneSomeAll.exists(p => PayrollPaymentType.validPaymentTypes.contains(p))
  }
}

object TaxableLumpSumItemModel {
  implicit val format: OFormat[TaxableLumpSumItemModel] = Json.format[TaxableLumpSumItemModel]
}

case class PayrollPaymentType(name: String) {
  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedPayrollPaymentType =
    EncryptedPayrollPaymentType(encryptedName =  Some(name.encrypted))
}

case class EncryptedPayrollPaymentType(encryptedName: Option[EncryptedValue] = None) {
  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): PayrollPaymentType = {
    PayrollPaymentType(name = encryptedName.map(_.decrypted[String]).getOrElse("ERROR"))
  }

  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val format: Format[EncryptedPayrollPaymentType] = Json.format[EncryptedPayrollPaymentType]

}

object PayrollPaymentType {
  val AllPaid: PayrollPaymentType = PayrollPaymentType("ALL_PAID")
  val SomePaid: PayrollPaymentType = PayrollPaymentType("SOME_PAID")
  val NonePaid: PayrollPaymentType = PayrollPaymentType("NONE_PAID")
  val Error: PayrollPaymentType = PayrollPaymentType("ERROR")
  val validPaymentTypes: Seq[PayrollPaymentType] = Seq(SomePaid, NonePaid, AllPaid)
  implicit val format: OFormat[PayrollPaymentType] = Json.format[PayrollPaymentType]
}


case class EncryptedTaxableLumpSumItemModel(
                                             encryptedLumpSumAmount: Option[EncryptedValue] = None,
                                             encryptedPayrollAmount: Option[EncryptedValue] = None,
                                             encryptedPayrollHasPaidNoneSomeAll: Option[EncryptedPayrollPaymentType] = None
                                           ) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): TaxableLumpSumItemModel = TaxableLumpSumItemModel(
    lumpSumAmount = encryptedLumpSumAmount.map(_.decrypted[BigDecimal]),
    payrollAmount = encryptedPayrollAmount.map(_.decrypted[BigDecimal]),
    payrollHasPaidNoneSomeAll = encryptedPayrollHasPaidNoneSomeAll.map(_.decrypted)
  )
}

object EncryptedTaxableLumpSumItemModel {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val format: Format[EncryptedTaxableLumpSumItemModel] = Json.format[EncryptedTaxableLumpSumItemModel]
  implicit val formatPayrollType: Format[EncryptedPayrollPaymentType] = Json.format[EncryptedPayrollPaymentType]
}