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

import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsObject, Json}
import support.UnitTest
import support.builders.models.employment.StudentLoansCYAModelBuilder.aStudentLoansCYAModel
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

class StudentLoansCYAModelSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  private val encryptedUglDeduction = EncryptedValue("encryptedUglDeduction", "some-nonce")
  private val encryptedUglDeductionAmount = EncryptedValue("encryptedUglDeductionAmount", "some-nonce")
  private val encryptedPglDeduction = EncryptedValue("encryptedPglDeduction", "some-nonce")
  private val encryptedPglDeductionAmount = EncryptedValue("encryptedPglDeductionAmount", "some-nonce")

  private val validModel: StudentLoansCYAModel = StudentLoansCYAModel(
    uglDeduction = true,
    uglDeductionAmount = Some(500.00),
    pglDeduction = true,
    pglDeductionAmount = Some(500.00)
  )

  private val validJson: JsObject = Json.obj(
    "uglDeduction" -> true,
    "uglDeductionAmount" -> 500.00,
    "pglDeduction" -> true,
    "pglDeductionAmount" -> 500.00
  )

  "StudentLoansCYAModel.format" should {
    "parse from json" in {
      validJson.as[StudentLoansCYAModel] shouldBe validModel
    }

    "parse to json" in {
      Json.toJson(validModel) shouldBe validJson
    }
  }

  "StudentLoansCYAModel.toDeductions" when {
    "passed a valid student loans cya model" should {
      "produce a deductions model" in {
        StudentLoansCYAModel(uglDeduction = false, None, pglDeduction = true, Some(44)).asDeductions shouldBe
          Some(Deductions(Some(StudentLoans(None, Some(44)))))
      }

      "produce a ugl deductions model" in {
        StudentLoansCYAModel(uglDeduction = true, Some(67), pglDeduction = false, None).asDeductions shouldBe
          Some(Deductions(Some(StudentLoans(Some(67), None))))
      }

      "produce a deductions model with both values" in {
        StudentLoansCYAModel(uglDeduction = true, Some(55), pglDeduction = true, Some(44)).asDeductions shouldBe
          Some(Deductions(Some(StudentLoans(Some(55), Some(44)))))
      }

      "return none" in {
        StudentLoansCYAModel(uglDeduction = false, None, pglDeduction = false, None).asDeductions shouldBe None
      }
    }
  }

  "StudentLoansCYAModel.encrypted" should {
    "return EncryptedStudentLoansCYAModel instance" in {
      val underTest = aStudentLoansCYAModel

      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.uglDeduction.toString, associatedText).returning(encryptedUglDeduction)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.uglDeductionAmount.get.toString(), associatedText).returning(encryptedUglDeductionAmount)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.pglDeduction.toString, associatedText).returning(encryptedPglDeduction)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.pglDeductionAmount.get.toString(), associatedText).returning(encryptedPglDeductionAmount)

      underTest.encrypted shouldBe EncryptedStudentLoansCYAModel(
        uglDeduction = encryptedUglDeduction,
        uglDeductionAmount = Some(encryptedUglDeductionAmount),
        pglDeduction = encryptedPglDeduction,
        pglDeductionAmount = Some(encryptedPglDeductionAmount)
      )
    }
  }

  "EncryptedStudentLoansCYAModel.decrypted" should {
    "return StudentLoansCYAModel instance" in {
      val underTest = EncryptedStudentLoansCYAModel(
        uglDeduction = encryptedUglDeduction,
        uglDeductionAmount = Some(encryptedUglDeductionAmount),
        pglDeduction = encryptedPglDeduction,
        pglDeductionAmount = Some(encryptedPglDeductionAmount)
      )

      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedUglDeduction, associatedText).returning(value = aStudentLoansCYAModel.uglDeduction.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedUglDeductionAmount, associatedText).returning(value = aStudentLoansCYAModel.uglDeductionAmount.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedPglDeduction, associatedText).returning(value = aStudentLoansCYAModel.pglDeduction.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedPglDeductionAmount, associatedText).returning(value = aStudentLoansCYAModel.pglDeductionAmount.get.toString())

      underTest.decrypted shouldBe aStudentLoansCYAModel
    }
  }
}
