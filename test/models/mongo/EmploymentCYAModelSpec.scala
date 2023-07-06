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
import models.employment.{EmploymentSource, EncryptedStudentLoansCYAModel, StudentLoansCYAModel}
import models.otheremployment.api.{LumpSum, OtherEmploymentIncome, TaxableLumpSumsAndCertainIncome}
import models.otheremployment.session.{EncryptedOtherEmploymentIncomeCYAModel, OtherEmploymentIncomeCYAModel, TaxableLumpSum}
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import utils.AesGcmAdCrypto

class EmploymentCYAModelSpec extends UnitTest
  with MockFactory {

  private implicit val aesGcmAdCrypto: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  private val employmentDetails = mock[EmploymentDetails]
  private val employmentBenefits = mock[BenefitsViewModel]
  private val studentLoans = mock[StudentLoansCYAModel]
  private val otherEmploymentIncome = mock[OtherEmploymentIncomeCYAModel]

  private val encryptedEmploymentDetails = mock[EncryptedEmploymentDetails]
  private val encryptedBenefitsViewModel = mock[EncryptedBenefitsViewModel]
  private val encryptedStudentLoansCYAModel = mock[EncryptedStudentLoansCYAModel]
  private val encryptedOtherEmploymentIncome = mock[EncryptedOtherEmploymentIncomeCYAModel]

  "EmploymentCYAModel.apply" should {
    "return an EmploymentCYAModel with the lump sums that are related to the same employerRef" in {
      val employmentSource = EmploymentSource("employmentId", "employmentName", Some("employerRef"), None, None, None, None, None, None, None)
      val otherEmploymentIncome = OtherEmploymentIncome(lumpSums = Some(Set(LumpSum(
        employerName = employmentSource.employerName,
        employerRef = employmentSource.employerRef.get,
        taxableLumpSumsAndCertainIncome = Some(TaxableLumpSumsAndCertainIncome(100)),
        benefitFromEmployerFinancedRetirementScheme = None,
        redundancyCompensationPaymentsOverExemption = None,
        redundancyCompensationPaymentsUnderExemption = None
      ))))

      val result = EmploymentCYAModel(employmentSource, true, Some(otherEmploymentIncome))

      result.otherEmploymentIncome shouldBe Some(OtherEmploymentIncomeCYAModel(Seq(TaxableLumpSum(100))))
    }

    "return an EmploymentCYAModel with no lump sums if there is not one with same employerRef" in {
      val employmentSource = EmploymentSource("employmentId", "employmentName", Some("employerRef"), None, None, None, None, None, None, None)
      val otherEmploymentIncome = OtherEmploymentIncome(lumpSums = Some(Set(LumpSum(
        employerName = "differentName",
        employerRef = "differentRef",
        taxableLumpSumsAndCertainIncome = Some(TaxableLumpSumsAndCertainIncome(100)),
        benefitFromEmployerFinancedRetirementScheme = None,
        redundancyCompensationPaymentsOverExemption = None,
        redundancyCompensationPaymentsUnderExemption = None
      ))))

      val result = EmploymentCYAModel(employmentSource, true, Some(otherEmploymentIncome))

      result.otherEmploymentIncome shouldBe Some(OtherEmploymentIncomeCYAModel(Seq()))
    }

    "return an EmploymentCYAModel with no OtherEmploymentIncome if there is None" in {
      val employmentSource = EmploymentSource("employmentId", "employmentName", Some("employerRef"), None, None, None, None, None, None, None)

      val result = EmploymentCYAModel(employmentSource, true, None)

      result.otherEmploymentIncome shouldBe None
    }
  }

  "EmploymentCYAModel.encrypted" should {
    "return EncryptedEmploymentCYAModel instance" in {
      val underTest = EmploymentCYAModel(
        employmentDetails = employmentDetails,
        employmentBenefits = Some(employmentBenefits),
        studentLoans = Some(studentLoans),
        otherEmploymentIncome = Some(otherEmploymentIncome)
      )

      (employmentDetails.encrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(encryptedEmploymentDetails)
      (employmentBenefits.encrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(encryptedBenefitsViewModel)
      (studentLoans.encrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(encryptedStudentLoansCYAModel)
      (otherEmploymentIncome.encrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(encryptedOtherEmploymentIncome)

      val encryptedResult = underTest.encrypted

      encryptedResult.employmentDetails shouldBe encryptedEmploymentDetails
      encryptedResult.employmentBenefits shouldBe Some(encryptedBenefitsViewModel)
      encryptedResult.studentLoansCYAModel shouldBe Some(encryptedStudentLoansCYAModel)
      encryptedResult.otherEmploymentIncome shouldBe Some(encryptedOtherEmploymentIncome)
    }
  }

  "EncryptedEmploymentCYAModel.decrypt" should {
    "return EmploymentCYAModel instance" in {
      val underTest = EncryptedEmploymentCYAModel(
        employmentDetails = encryptedEmploymentDetails,
        employmentBenefits = Some(encryptedBenefitsViewModel),
        studentLoansCYAModel = Some(encryptedStudentLoansCYAModel),
        otherEmploymentIncome = Some(encryptedOtherEmploymentIncome)
      )

      (encryptedEmploymentDetails.decrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(employmentDetails)
      (encryptedBenefitsViewModel.decrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(employmentBenefits)
      (encryptedStudentLoansCYAModel.decrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(studentLoans)
      (encryptedOtherEmploymentIncome.decrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(otherEmploymentIncome)

      val decryptedResult = underTest.decrypted

      decryptedResult.employmentDetails shouldBe employmentDetails
      decryptedResult.employmentBenefits shouldBe Some(employmentBenefits)
      decryptedResult.studentLoans shouldBe Some(studentLoans)
      decryptedResult.otherEmploymentIncome shouldBe Some(otherEmploymentIncome)
    }
  }
}
