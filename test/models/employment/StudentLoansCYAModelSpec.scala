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

import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsObject, Json}
import support.UnitTest
import support.builders.models.employment.StudentLoansCYAModelBuilder.aStudentLoansCYAModel
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher}

class StudentLoansCYAModelSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

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

      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.uglDeduction, textAndKey).returning(encryptedUglDeduction)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.uglDeductionAmount.get, textAndKey).returning(encryptedUglDeductionAmount)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.pglDeduction, textAndKey).returning(encryptedPglDeduction)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.pglDeductionAmount.get, textAndKey).returning(encryptedPglDeductionAmount)

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

      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedUglDeduction.value, encryptedUglDeduction.nonce, textAndKey, *).returning(value = aStudentLoansCYAModel.uglDeduction)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedUglDeductionAmount.value, encryptedUglDeductionAmount.nonce, textAndKey, *).returning(value = aStudentLoansCYAModel.uglDeductionAmount.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedPglDeduction.value, encryptedPglDeduction.nonce, textAndKey, *).returning(value = aStudentLoansCYAModel.pglDeduction)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedPglDeductionAmount.value, encryptedPglDeductionAmount.nonce, textAndKey, *).returning(value = aStudentLoansCYAModel.pglDeductionAmount.get)

      underTest.decrypted shouldBe aStudentLoansCYAModel
    }
  }
}
