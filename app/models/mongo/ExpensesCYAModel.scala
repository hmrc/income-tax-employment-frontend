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

import models.expenses.{EncryptedExpensesViewModel, Expenses, ExpensesViewModel}
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

case class ExpensesCYAModel(expenses: ExpensesViewModel) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedExpensesCYAModel = EncryptedExpensesCYAModel(
    expenses = expenses.encrypted
  )
}

object ExpensesCYAModel {
  implicit val format: OFormat[ExpensesCYAModel] = Json.format[ExpensesCYAModel]

  def makeModel(expenses: Expenses, isUsingCustomerData: Boolean, submittedOn: Option[String]): ExpensesCYAModel =
    ExpensesCYAModel(expenses = expenses.toExpensesViewModel(isUsingCustomerData, submittedOn))
}

case class EncryptedExpensesCYAModel(expenses: EncryptedExpensesViewModel) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): ExpensesCYAModel =
    ExpensesCYAModel(expenses = expenses.decrypted)
}

object EncryptedExpensesCYAModel {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedExpensesCYAModel] = Json.format[EncryptedExpensesCYAModel]
}
