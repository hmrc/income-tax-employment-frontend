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

package models.otheremployment

import models.otheremployment.session.{EncryptedOtherEmploymentIncomeCYAModel, EncryptedTaxableLumpSum, OtherEmploymentIncomeCYAModel, TaxableLumpSum}
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import utils.AesGcmAdCrypto

class OtherEmploymentIncomeCYAModelSpec extends UnitTest with MockFactory {

  private implicit val secureGCMCipher: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  private val taxableLumpSum = mock[TaxableLumpSum]
  private val encryptedTaxableLumpSum = mock[EncryptedTaxableLumpSum]

  "OtherEmploymentIncomeCYAModel.encrypted" should {
    "return EncryptedOtherEmploymentIncomeCYAModel instance" in {
      val underTest = OtherEmploymentIncomeCYAModel(Seq(taxableLumpSum))

      (taxableLumpSum.encrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(encryptedTaxableLumpSum)

      val encryptedResult = underTest.encrypted

      encryptedResult.encryptedTaxableLumpSums.head shouldBe encryptedTaxableLumpSum
    }
  }

  "EncryptedOtherEmploymentIncomeCYAModel.decrypted" should {
    "return OtherEmploymentIncomeCYAModel instance" in {
      val underTest = EncryptedOtherEmploymentIncomeCYAModel(Seq(encryptedTaxableLumpSum))

      (encryptedTaxableLumpSum.decrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(taxableLumpSum)

      val decryptedResult = underTest.decrypted
      decryptedResult.taxableLumpSums.head shouldBe taxableLumpSum
    }
  }
}
