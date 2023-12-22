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

import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

class EmploymentDetailsSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

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
  private val encryptedOffPayrollWorkingStatus = EncryptedValue("encryptedOffPayrollWorkingStatus", "some-nonce")

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
      val underTest = anEmploymentDetails.copy(cessationDate = Some("some-cessation-date"), dateIgnored = Some("some-date-ignored"), offPayrollWorkingStatus = Some(true))

      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.employerName, associatedText).returning(encryptedEmployerName)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.employerRef.get, associatedText).returning(encryptedEmployerRef)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.startDate.get, associatedText).returning(encryptedStartDate)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.payrollId.get, associatedText).returning(encryptedPayrollId)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.didYouLeaveQuestion.get.toString, associatedText).returning(encryptedDidYouLeaveQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.cessationDate.get, associatedText).returning(encryptedCessationDate)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.dateIgnored.get, associatedText).returning(encryptedDateIgnored)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.employmentSubmittedOn.get, associatedText).returning(encryptedEmploymentSubmittedOn)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.employmentDetailsSubmittedOn.get, associatedText).returning(encryptedEmploymentDetailsSubmittedOn)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.taxablePayToDate.get.toString(), associatedText).returning(encryptedTaxablePayToDate)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.totalTaxToDate.get.toString(), associatedText).returning(encryptedTotalTaxToDate)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.currentDataIsHmrcHeld.toString, associatedText).returning(encryptedCurrentDataIsHmrcHeld)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.offPayrollWorkingStatus.get.toString(), associatedText).returning(encryptedOffPayrollWorkingStatus)

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
        currentDataIsHmrcHeld = encryptedCurrentDataIsHmrcHeld,
        offPayrollWorkingStatus = Some(encryptedOffPayrollWorkingStatus)
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
        currentDataIsHmrcHeld = encryptedCurrentDataIsHmrcHeld,
        offPayrollWorkingStatus = Some(encryptedOffPayrollWorkingStatus)
      )

      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedEmployerName, associatedText).returning("some-employer-name")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedEmployerRef, associatedText).returning("some-employer-ref")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedStartDate, associatedText).returning("some-start-date")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedPayrollId, associatedText).returning("some-payroll-id")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedDidYouLeaveQuestion, associatedText).returning("true")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedCessationDate, associatedText).returning("some-cessation-date")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedDateIgnored, associatedText).returning("some-date-ignored")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedEmploymentSubmittedOn, associatedText).returning("some-employment-submitted-on")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedEmploymentDetailsSubmittedOn, associatedText).returning("some-employment-details-submitted-on")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedTaxablePayToDate, associatedText).returning("100")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedTotalTaxToDate, associatedText).returning("200")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedCurrentDataIsHmrcHeld, associatedText).returning("true")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedOffPayrollWorkingStatus, associatedText).returning("true")

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
        currentDataIsHmrcHeld = true,
        offPayrollWorkingStatus = Some(true)
      )
    }
  }
}
