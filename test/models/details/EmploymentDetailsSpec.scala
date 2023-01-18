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

import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher}

class EmploymentDetailsSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val encryptedEmployerName = EncryptedValue("encryptedEmployerName", "some-nonce")
  private val encryptedEmployerRef = EncryptedValue("encryptedEmployerRef", "some-nonce")
  private val encryptedStartDate = EncryptedValue("encryptedStartDate", "some-nonce")
  private val encryptedPayrollId = EncryptedValue("encryptedPayrollId", "some-nonce")
  private val encryptedDidYouLeaveQuestion = EncryptedValue("encryptedDidYouLeaveQuestion", "some-nonce")
  private val encryptedCessationDate = EncryptedValue("encryptedCessationDate", "some-nonce")
  private val encryptedDateIgnored = EncryptedValue("encryptedDateIgnored", "some-nonce")
  private val encryptedEmploymentSubmittedOn = EncryptedValue("encryptedEmploymentSubmittedOn", "some-nonce")
  private val encryptedEmploymentDetailsSubmittedOn = EncryptedValue("encryptedEmploymentDetailsSubmittedOn", "some-nonce")
  private val encryptedTaxablePayToDate = EncryptedValue("encryptedTaxablePayToDate", "some-nonce")
  private val encryptedTotalTaxToDate = EncryptedValue("encryptedTotalTaxToDate", "some-nonce")
  private val encryptedCurrentDataIsHmrcHeld = EncryptedValue("encryptedCurrentDataIsHmrcHeld", "some-nonce")

  "EmploymentDetails.isSubmittable" should {
    "return false" when {
      "startDate is not defined" in {
        val underTest = anEmploymentDetails.copy(startDate = None)

        underTest.isSubmittable shouldBe false
      }

      "taxablePayToDate is not defined" in {
        val underTest = anEmploymentDetails.copy(taxablePayToDate = None)

        underTest.isSubmittable shouldBe false
      }

      "totalTaxToDate is not defined" in {
        val underTest = anEmploymentDetails.copy(totalTaxToDate = None)

        underTest.isSubmittable shouldBe false
      }
    }

    "return true when startDate, taxablePayToDate and totalTaxToDate are defined" in {
      val underTest = anEmploymentDetails.copy(
        employerRef = None,
        startDate = Some("2020-11-11"),
        payrollId = None,
        taxablePayToDate = Some(55.99),
        totalTaxToDate = Some(3453453.00)
      )

      underTest.isSubmittable shouldBe true
    }
  }

  "EmploymentDetails.isFinished" should {
    "return false" when {
      "startDate is None" in {
        val underTest = anEmploymentDetails.copy(startDate = None)

        underTest.isFinished shouldBe false
      }

      "didYouLeaveQuestion is true and cessationDate is None" in {
        val underTest = anEmploymentDetails.copy(didYouLeaveQuestion = Some(true), cessationDate = None)

        underTest.isFinished shouldBe false
      }

      "didYouLeaveQuestion is None" in {
        val underTest = anEmploymentDetails.copy(didYouLeaveQuestion = None)

        underTest.isFinished shouldBe false
      }

      "taxablePayToDate is None" in {
        val underTest = anEmploymentDetails.copy(taxablePayToDate = None)

        underTest.isFinished shouldBe false
      }

      "totalTaxToDate is None" in {
        val underTest = anEmploymentDetails.copy(totalTaxToDate = None)

        underTest.isFinished shouldBe false
      }
    }

    "return true" when {
      "all fields are populated and didYouLeaveQuestion is false" in {
        val underTest = anEmploymentDetails.copy(didYouLeaveQuestion = Some(false), cessationDate = None)

        underTest.isFinished shouldBe true
      }

      "all fields are populated" in {
        anEmploymentDetails.isFinished shouldBe true
      }
    }
  }

  "EmploymentDetails.encrypted" should {
    "return EncryptedEmploymentDetails" in {
      val underTest = anEmploymentDetails.copy(cessationDate = Some("some-cessation-date"), dateIgnored = Some("some-date-ignored"))

      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.employerName, textAndKey).returning(encryptedEmployerName)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.employerRef.get, textAndKey).returning(encryptedEmployerRef)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.startDate.get, textAndKey).returning(encryptedStartDate)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.payrollId.get, textAndKey).returning(encryptedPayrollId)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.didYouLeaveQuestion.get, textAndKey).returning(encryptedDidYouLeaveQuestion)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.cessationDate.get, textAndKey).returning(encryptedCessationDate)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.dateIgnored.get, textAndKey).returning(encryptedDateIgnored)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.employmentSubmittedOn.get, textAndKey).returning(encryptedEmploymentSubmittedOn)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.employmentDetailsSubmittedOn.get, textAndKey).returning(encryptedEmploymentDetailsSubmittedOn)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.taxablePayToDate.get, textAndKey).returning(encryptedTaxablePayToDate)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.totalTaxToDate.get, textAndKey).returning(encryptedTotalTaxToDate)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.currentDataIsHmrcHeld, textAndKey).returning(encryptedCurrentDataIsHmrcHeld)

      underTest.encrypted shouldBe EncryptedEmploymentDetails(
        employerName = encryptedEmployerName,
        employerRef = Some(encryptedEmployerRef),
        startDate = Some(encryptedStartDate),
        payrollId = Some(encryptedPayrollId),
        didYouLeaveQuestion = Some(encryptedDidYouLeaveQuestion),
        cessationDate = Some(encryptedCessationDate),
        dateIgnored = Some(encryptedDateIgnored),
        employmentSubmittedOn = Some(encryptedEmploymentSubmittedOn),
        employmentDetailsSubmittedOn = Some(encryptedEmploymentDetailsSubmittedOn),
        taxablePayToDate = Some(encryptedTaxablePayToDate),
        totalTaxToDate = Some(encryptedTotalTaxToDate),
        currentDataIsHmrcHeld = encryptedCurrentDataIsHmrcHeld
      )
    }
  }

  "EncryptedEmploymentDetails.decrypted" should {
    "return EmploymentDetails" in {
      val encryptedEmploymentDetails = EncryptedEmploymentDetails(
        employerName = encryptedEmployerName,
        employerRef = Some(encryptedEmployerRef),
        startDate = Some(encryptedStartDate),
        payrollId = Some(encryptedPayrollId),
        didYouLeaveQuestion = Some(encryptedDidYouLeaveQuestion),
        cessationDate = Some(encryptedCessationDate),
        dateIgnored = Some(encryptedDateIgnored),
        employmentSubmittedOn = Some(encryptedEmploymentSubmittedOn),
        employmentDetailsSubmittedOn = Some(encryptedEmploymentDetailsSubmittedOn),
        taxablePayToDate = Some(encryptedTaxablePayToDate),
        totalTaxToDate = Some(encryptedTotalTaxToDate),
        currentDataIsHmrcHeld = encryptedCurrentDataIsHmrcHeld
      )

      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedEmployerName.value, encryptedEmployerName.nonce, textAndKey, *).returning("some-employer-name")
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedEmployerRef.value, encryptedEmployerRef.nonce, textAndKey, *).returning("some-employer-ref")
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedStartDate.value, encryptedStartDate.nonce, textAndKey, *).returning("some-start-date")
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedPayrollId.value, encryptedPayrollId.nonce, textAndKey, *).returning("some-payroll-id")
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedDidYouLeaveQuestion.value, encryptedDidYouLeaveQuestion.nonce, textAndKey, *).returning(true)
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedCessationDate.value, encryptedCessationDate.nonce, textAndKey, *).returning("some-cessation-date")
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedDateIgnored.value, encryptedDateIgnored.nonce, textAndKey, *).returning("some-date-ignored")
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedEmploymentSubmittedOn.value, encryptedEmploymentSubmittedOn.nonce, textAndKey, *).returning("some-employment-submitted-on")
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedEmploymentDetailsSubmittedOn.value, encryptedEmploymentDetailsSubmittedOn.nonce, textAndKey, *).returning("some-employment-details-submitted-on")
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedTaxablePayToDate.value, encryptedTaxablePayToDate.nonce, textAndKey, *).returning(100)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedTotalTaxToDate.value, encryptedTotalTaxToDate.nonce, textAndKey, *).returning(200)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedCurrentDataIsHmrcHeld.value, encryptedCurrentDataIsHmrcHeld.nonce, textAndKey, *).returning(true)

      encryptedEmploymentDetails.decrypted shouldBe EmploymentDetails(
        employerName = "some-employer-name",
        employerRef = Some("some-employer-ref"),
        startDate = Some("some-start-date"),
        payrollId = Some("some-payroll-id"),
        didYouLeaveQuestion = Some(true),
        cessationDate = Some("some-cessation-date"),
        dateIgnored = Some("some-date-ignored"),
        employmentSubmittedOn = Some("some-employment-submitted-on"),
        employmentDetailsSubmittedOn = Some("some-employment-details-submitted-on"),
        taxablePayToDate = Some(100),
        totalTaxToDate = Some(200),
        currentDataIsHmrcHeld = true
      )
    }
  }
}
