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

package models.otheremployment.session

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}


case class TaxableLumpSum(amount: BigDecimal,
                          payrollAmount: Option[BigDecimal] = None,
                          payrollHasPaidNoneSomeAll: Option[PayrollPaymentType] = None) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedTaxableLumpSum =
    EncryptedTaxableLumpSum(
      amount = amount.encrypted,
      payrollAmount = payrollAmount.map(_.encrypted),
      payrollHasPaidNoneSomeAll = payrollHasPaidNoneSomeAll.map(_.encrypted)
    )

  def validatePayrollHasPaidNoneSomeAll: Boolean = {
    payrollHasPaidNoneSomeAll.exists(p => PayrollPaymentType.validPaymentTypes.contains(p))
  }
}

object TaxableLumpSum {
  implicit val format: OFormat[TaxableLumpSum] = Json.format[TaxableLumpSum]
}

case class PayrollPaymentType(name: String) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedPayrollPaymentType =
    EncryptedPayrollPaymentType(encryptedName = Some(name.encrypted))
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


case class EncryptedTaxableLumpSum(amount: EncryptedValue,
                                   payrollAmount: Option[EncryptedValue] = None,
                                   payrollHasPaidNoneSomeAll: Option[EncryptedPayrollPaymentType] = None) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): TaxableLumpSum = TaxableLumpSum(
    amount = amount.decrypted[BigDecimal],
    payrollAmount = payrollAmount.map(_.decrypted[BigDecimal]),
    payrollHasPaidNoneSomeAll = payrollHasPaidNoneSomeAll.map(_.decrypted)
  )
}

object EncryptedTaxableLumpSum {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val format: Format[EncryptedTaxableLumpSum] = Json.format[EncryptedTaxableLumpSum]
  implicit lazy val formatPayrollType: Format[EncryptedPayrollPaymentType] = Json.format[EncryptedPayrollPaymentType]
}
