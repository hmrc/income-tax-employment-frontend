/*
 * Copyright 2021 HM Revenue & Customs
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

import models.employment.{EncryptedExpenses, Expenses}
import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class ExpensesCYAModel(expenses: Expenses,
                            currentDataIsHmrcHeld: Boolean)

object ExpensesCYAModel {
  implicit val format: OFormat[ExpensesCYAModel] = Json.format[ExpensesCYAModel]

  def makeModel(expenses: Expenses, isUsingCustomerData: Boolean): ExpensesCYAModel = {
    ExpensesCYAModel(
      expenses,
      !isUsingCustomerData
    )
  }

}

case class EncryptedExpensesCYAModel(expenses: EncryptedExpenses, currentDataIsHmrcHeld: EncryptedValue)

object EncryptedExpensesCYAModel {
  implicit val format: OFormat[EncryptedExpensesCYAModel] = Json.format[EncryptedExpensesCYAModel]
}
