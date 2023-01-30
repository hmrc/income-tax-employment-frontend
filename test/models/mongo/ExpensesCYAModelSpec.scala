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

import models.expenses.{EncryptedExpensesViewModel, ExpensesViewModel}
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import utils.AesGcmAdCrypto

class ExpensesCYAModelSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  private val expensesViewModel = mock[ExpensesViewModel]
  private val encryptedExpensesViewModel = mock[EncryptedExpensesViewModel]

  "ExpensesCYAModel.encrypted" should {
    "return EncryptedExpensesCYAModel instance" in {
      val underTest = ExpensesCYAModel(expenses = expensesViewModel)

      (expensesViewModel.encrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(encryptedExpensesViewModel)

      val encryptedResult = underTest.encrypted

      encryptedResult.expenses shouldBe encryptedExpensesViewModel
    }
  }

  "EncryptedExpensesCYAModel.decrypted" should {
    "return ExpensesCYAModel instance" in {
      val underTest = EncryptedExpensesCYAModel(expenses = encryptedExpensesViewModel)

      (encryptedExpensesViewModel.decrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(expensesViewModel)

      val decryptedResult = underTest.decrypted

      decryptedResult.expenses shouldBe expensesViewModel
    }
  }
}
