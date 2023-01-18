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
import support.builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserData
import utils.SecureGCMCipher

class ExpensesUserDataSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val encryptedExpensesCYAModel = mock[EncryptedExpensesCYAModel]
  private val expensesCya = mock[ExpensesCYAModel]

  "ExpensesUserData.encrypted" should {
    "return EncryptedExpensesUserData instance" in {
      val underTest = anExpensesUserData.copy(expensesCya = expensesCya)

      (expensesCya.encrypted(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedExpensesCYAModel)

      val encryptedResponse = underTest.encrypted

      encryptedResponse.sessionId shouldBe anExpensesUserData.sessionId
      encryptedResponse.mtdItId shouldBe anExpensesUserData.mtdItId
      encryptedResponse.nino shouldBe anExpensesUserData.nino
      encryptedResponse.taxYear shouldBe anExpensesUserData.taxYear
      encryptedResponse.isPriorSubmission shouldBe anExpensesUserData.isPriorSubmission
      encryptedResponse.hasPriorExpenses shouldBe anExpensesUserData.hasPriorExpenses
      encryptedResponse.expensesCya shouldBe encryptedExpensesCYAModel
      encryptedResponse.lastUpdated shouldBe anExpensesUserData.lastUpdated
    }
  }

  "EncryptedExpensesUserData.decrypted" should {
    "return ExpensesUserData instance" in {
      val underTest = EncryptedExpensesUserData(
        sessionId = anExpensesUserData.sessionId,
        mtdItId = anExpensesUserData.mtdItId,
        nino = anExpensesUserData.nino,
        taxYear = anExpensesUserData.taxYear,
        isPriorSubmission = anExpensesUserData.isPriorSubmission,
        hasPriorExpenses = anExpensesUserData.hasPriorExpenses,
        expensesCya = encryptedExpensesCYAModel,
        lastUpdated = anExpensesUserData.lastUpdated,
      )

      (encryptedExpensesCYAModel.decrypted(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(expensesCya)

      val decryptedResult = underTest.decrypted

      decryptedResult.sessionId shouldBe anExpensesUserData.sessionId
      decryptedResult.mtdItId shouldBe anExpensesUserData.mtdItId
      decryptedResult.nino shouldBe anExpensesUserData.nino
      decryptedResult.taxYear shouldBe anExpensesUserData.taxYear
      decryptedResult.isPriorSubmission shouldBe anExpensesUserData.isPriorSubmission
      decryptedResult.hasPriorExpenses shouldBe anExpensesUserData.hasPriorExpenses
      decryptedResult.expensesCya shouldBe expensesCya
      decryptedResult.lastUpdated shouldBe anExpensesUserData.lastUpdated
    }
  }
}
