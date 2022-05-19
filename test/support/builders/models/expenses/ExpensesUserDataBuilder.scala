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

package support.builders.models.expenses

import support.builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import utils.TestTaxYearHelper

object ExpensesUserDataBuilder extends TestTaxYearHelper {

  val anExpensesUserData: ExpensesUserData = ExpensesUserData(
    sessionId = "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe",
    mtdItId = "1234567890",
    nino = "AA123456A",
    taxYear = taxYearEOY,
    isPriorSubmission = true,
    hasPriorExpenses = true,
    expensesCya = anExpensesCYAModel
  )

  def anExpensesUserDataWithBenefits(expensesViewModel: ExpensesViewModel,
                                       isPriorSubmission: Boolean = true): ExpensesUserData = {
    anExpensesUserData.copy(
      isPriorSubmission = isPriorSubmission,
      hasPriorExpenses = isPriorSubmission,
      expensesCya = ExpensesCYAModel(expensesViewModel)
    )
  }
}
