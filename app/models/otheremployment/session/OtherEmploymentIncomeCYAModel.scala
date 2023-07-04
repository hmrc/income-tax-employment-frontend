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

import models.otheremployment.api.OtherEmploymentIncome
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto


case class OtherEmploymentIncomeCYAModel(taxableLumpSums: Seq[TaxableLumpSum] = Seq.empty[TaxableLumpSum]) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedOtherEmploymentIncomeCYAModel =
    EncryptedOtherEmploymentIncomeCYAModel(taxableLumpSums.map(_.encrypted))
}

object OtherEmploymentIncomeCYAModel {
  implicit val format: OFormat[OtherEmploymentIncomeCYAModel] = Json.format[OtherEmploymentIncomeCYAModel]

  def apply(otherEmploymentIncome: OtherEmploymentIncome, employerRef: String): OtherEmploymentIncomeCYAModel = {
    OtherEmploymentIncomeCYAModel(
      taxableLumpSums = otherEmploymentIncome.lumpSums.getOrElse(Seq.empty)
        .filter(_.employerRef == employerRef)
        .flatMap(_.taxableLumpSumsAndCertainIncome.map(lumpSum => TaxableLumpSum(amount = lumpSum.amount))).toSeq
    )
  }
}

case class EncryptedOtherEmploymentIncomeCYAModel(encryptedTaxableLumpSums: Seq[EncryptedTaxableLumpSum] = Seq.empty[EncryptedTaxableLumpSum]) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): OtherEmploymentIncomeCYAModel = OtherEmploymentIncomeCYAModel(
    taxableLumpSums = encryptedTaxableLumpSums.map(_.decrypted)
  )
}

object EncryptedOtherEmploymentIncomeCYAModel {
  implicit lazy val formatEnc: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val format: OFormat[EncryptedOtherEmploymentIncomeCYAModel] = Json.format[EncryptedOtherEmploymentIncomeCYAModel]
  implicit val formatPayrollType: Format[EncryptedPayrollPaymentType] = Json.format[EncryptedPayrollPaymentType]
}