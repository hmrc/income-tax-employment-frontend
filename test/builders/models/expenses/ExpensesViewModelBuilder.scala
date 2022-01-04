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

package builders.models.expenses

import models.expenses.ExpensesViewModel

object ExpensesViewModelBuilder {

  val anExpensesViewModel: ExpensesViewModel = ExpensesViewModel(
    claimingEmploymentExpenses = true,
    jobExpensesQuestion = Some(true),
    jobExpenses = Some(200.00),
    flatRateJobExpensesQuestion = Some(true),
    flatRateJobExpenses = Some(300.00),
    professionalSubscriptionsQuestion = Some(true),
    professionalSubscriptions = Some(400.00),
    otherAndCapitalAllowancesQuestion = Some(true),
    otherAndCapitalAllowances = Some(600.00),
    businessTravelCosts = Some(100.00),
    hotelAndMealExpenses = Some(500.00),
    vehicleExpenses = Some(700.00),
    mileageAllowanceRelief = Some(800.00),
    submittedOn = None,
    isUsingCustomerData = true
  )
}
