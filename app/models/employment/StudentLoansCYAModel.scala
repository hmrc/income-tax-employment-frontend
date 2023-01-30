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


case class StudentLoansCYAModel(uglDeduction: Boolean,
                                uglDeductionAmount: Option[BigDecimal] = None,
                                pglDeduction: Boolean,
                                pglDeductionAmount: Option[BigDecimal] = None) {

  lazy val asDeductions: Option[Deductions] =
    if (uglDeductionAmount.isDefined || pglDeductionAmount.isDefined) {
      Some(Deductions(Some(StudentLoans(uglDeductionAmount, pglDeductionAmount))))
    } else {
      None
    }

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedStudentLoansCYAModel = EncryptedStudentLoansCYAModel(
    uglDeduction = uglDeduction.encrypted,
    uglDeductionAmount = uglDeductionAmount.map(_.encrypted),
    pglDeduction = pglDeduction.encrypted,
    pglDeductionAmount = pglDeductionAmount.map(_.encrypted)
  )
}


object StudentLoansCYAModel {
  implicit val formats: OFormat[StudentLoansCYAModel] = Json.format[StudentLoansCYAModel]
}

case class EncryptedStudentLoansCYAModel(uglDeduction: EncryptedValue,
                                         uglDeductionAmount: Option[EncryptedValue] = None,
                                         pglDeduction: EncryptedValue,
                                         pglDeductionAmount: Option[EncryptedValue] = None) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): StudentLoansCYAModel = StudentLoansCYAModel(
    uglDeduction = uglDeduction.decrypted[Boolean],
    uglDeductionAmount = uglDeductionAmount.map(_.decrypted[BigDecimal]),
    pglDeduction = pglDeduction.decrypted[Boolean],
    pglDeductionAmount = pglDeductionAmount.map(_.decrypted[BigDecimal])
  )
}


object EncryptedStudentLoansCYAModel {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val formats: Format[EncryptedStudentLoansCYAModel] = Json.format[EncryptedStudentLoansCYAModel]
}
