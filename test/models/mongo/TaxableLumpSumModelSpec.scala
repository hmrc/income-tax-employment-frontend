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

import models.employment.{EncryptedTaxableLumpSumViewModel, PayrollPaymentType, TaxableLumpSumItemModel, TaxableLumpSumViewModel}
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import utils.AesGcmAdCrypto

class TaxableLumpSumModelSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  private val extraDetailsView = mock[TaxableLumpSumViewModel]
  private val encryptedAdditionalInfoViewModel = mock[EncryptedTaxableLumpSumViewModel]

  "AdditionalInfoViewModel.encrypted" should {
    "return EncryptedAdditionalInfoViewModel instance" in {
      val underTest = extraDetailsView

      (extraDetailsView.encrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(encryptedAdditionalInfoViewModel)
      val encryptedResult = underTest.encrypted
      encryptedResult.encryptedAdditionalInfoViewModel shouldBe encryptedAdditionalInfoViewModel.encryptedAdditionalInfoViewModel
    }
  }

  "EncryptedAdditionalInfoViewModel.decrypted" should {
    "return AdditionalInfoViewModel instance" in {
      val underTest = encryptedAdditionalInfoViewModel

      (encryptedAdditionalInfoViewModel.decrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(extraDetailsView)

      val decryptedResult = underTest.decrypted
      decryptedResult.items shouldBe extraDetailsView.items
    }
  }

  "validatePayrollHasPaidNoneSomeAll" should {
    "return true for a payroll paid type that exist in validPayrollAnswers" in {
      TaxableLumpSumItemModel(None, None, Some(PayrollPaymentType.SomePaid)).validatePayrollHasPaidNoneSomeAll shouldBe true
    }

    "return false for a payroll paid type that doesn't exist in validPayrollAnswers" in {
      TaxableLumpSumItemModel(None, None, Some(PayrollPaymentType.Error)).validatePayrollHasPaidNoneSomeAll shouldBe false
    }

    "return false for no payroll paid type" in {
      TaxableLumpSumItemModel(None, None, None).validatePayrollHasPaidNoneSomeAll shouldBe false
    }
  }
}
