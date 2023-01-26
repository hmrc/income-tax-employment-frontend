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

import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.AesGcmAdCrypto

class EmploymentUserDataSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  private val employmentCYAModel = mock[EmploymentCYAModel]
  private val encryptedEmploymentCYAModel = mock[EncryptedEmploymentCYAModel]

  "EmploymentUserData.encrypted" should {
    "return EncryptedEmploymentUserData instance" in {
      val underTest = anEmploymentUserData.copy(employment = employmentCYAModel)

      (employmentCYAModel.encrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(encryptedEmploymentCYAModel)

      val encryptedResult = underTest.encrypted

      encryptedResult.sessionId shouldBe anEmploymentUserData.sessionId
      encryptedResult.mtdItId shouldBe anEmploymentUserData.mtdItId
      encryptedResult.nino shouldBe anEmploymentUserData.nino
      encryptedResult.taxYear shouldBe anEmploymentUserData.taxYear
      encryptedResult.employmentId shouldBe anEmploymentUserData.employmentId
      encryptedResult.isPriorSubmission shouldBe anEmploymentUserData.isPriorSubmission
      encryptedResult.hasPriorBenefits shouldBe anEmploymentUserData.hasPriorBenefits
      encryptedResult.hasPriorStudentLoans shouldBe anEmploymentUserData.hasPriorStudentLoans
      encryptedResult.employment shouldBe encryptedEmploymentCYAModel
      encryptedResult.lastUpdated shouldBe anEmploymentUserData.lastUpdated
    }
  }

  "EncryptedEmploymentUserData.decrypted" should {
    "return EmploymentUserData instance" in {
      val underTest = EncryptedEmploymentUserData(
        sessionId = anEmploymentUserData.sessionId,
        mtdItId = anEmploymentUserData.mtdItId,
        nino = anEmploymentUserData.nino,
        taxYear = anEmploymentUserData.taxYear,
        employmentId = anEmploymentUserData.employmentId,
        isPriorSubmission = anEmploymentUserData.isPriorSubmission,
        hasPriorBenefits = anEmploymentUserData.hasPriorBenefits,
        hasPriorStudentLoans = anEmploymentUserData.hasPriorStudentLoans,
        employment = encryptedEmploymentCYAModel,
        lastUpdated = anEmploymentUserData.lastUpdated,
      )

      (encryptedEmploymentCYAModel.decrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(employmentCYAModel)

      val decryptedResult = underTest.decrypted

      decryptedResult.sessionId shouldBe anEmploymentUserData.sessionId
      decryptedResult.mtdItId shouldBe anEmploymentUserData.mtdItId
      decryptedResult.nino shouldBe anEmploymentUserData.nino
      decryptedResult.taxYear shouldBe anEmploymentUserData.taxYear
      decryptedResult.employmentId shouldBe anEmploymentUserData.employmentId
      decryptedResult.isPriorSubmission shouldBe anEmploymentUserData.isPriorSubmission
      decryptedResult.hasPriorBenefits shouldBe anEmploymentUserData.hasPriorBenefits
      decryptedResult.hasPriorStudentLoans shouldBe anEmploymentUserData.hasPriorStudentLoans
      decryptedResult.employment shouldBe employmentCYAModel
      decryptedResult.lastUpdated shouldBe anEmploymentUserData.lastUpdated
    }
  }
}
