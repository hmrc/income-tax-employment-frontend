/*
 * Copyright 2022 HM Revenue & Customs
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
import models.employment.{EncryptedStudentLoansCYAModel, StudentLoansCYAModel}
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import utils.SecureGCMCipher

class EmploymentCYAModelSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val employmentDetails = mock[EmploymentDetails]
  private val employmentBenefits = mock[BenefitsViewModel]
  private val studentLoans = mock[StudentLoansCYAModel]

  private val encryptedEmploymentDetails = mock[EncryptedEmploymentDetails]
  private val encryptedBenefitsViewModel = mock[EncryptedBenefitsViewModel]
  private val encryptedStudentLoansCYAModel = mock[EncryptedStudentLoansCYAModel]

  "EmploymentCYAModel.encrypted" should {
    "return EncryptedEmploymentCYAModel instance" in {
      val underTest = EmploymentCYAModel(
        employmentDetails = employmentDetails,
        employmentBenefits = Some(employmentBenefits),
        studentLoans = Some(studentLoans)
      )

      (employmentDetails.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedEmploymentDetails)
      (employmentBenefits.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedBenefitsViewModel)
      (studentLoans.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedStudentLoansCYAModel)

      val encryptedResult = underTest.encrypted

      encryptedResult.employmentDetails shouldBe encryptedEmploymentDetails
      encryptedResult.employmentBenefits shouldBe Some(encryptedBenefitsViewModel)
      encryptedResult.studentLoansCYAModel shouldBe Some(encryptedStudentLoansCYAModel)
    }
  }

  "EncryptedEmploymentCYAModel.decrypt" should {
    "return EmploymentCYAModel instance" in {
      val underTest = EncryptedEmploymentCYAModel(
        employmentDetails = encryptedEmploymentDetails,
        employmentBenefits = Some(encryptedBenefitsViewModel),
        studentLoansCYAModel = Some(encryptedStudentLoansCYAModel)
      )

      (encryptedEmploymentDetails.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(employmentDetails)
      (encryptedBenefitsViewModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(employmentBenefits)
      (encryptedStudentLoansCYAModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(studentLoans)

      val decryptedResult = underTest.decrypted

      decryptedResult.employmentDetails shouldBe employmentDetails
      decryptedResult.employmentBenefits shouldBe Some(employmentBenefits)
      decryptedResult.studentLoans shouldBe Some(studentLoans)
    }
  }
}