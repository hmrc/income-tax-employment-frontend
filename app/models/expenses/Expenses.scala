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

package models.expenses

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class Expenses(businessTravelCosts: Option[BigDecimal] = None,
                    jobExpenses: Option[BigDecimal] = None,
                    flatRateJobExpenses: Option[BigDecimal] = None,
                    professionalSubscriptions: Option[BigDecimal] = None,
                    hotelAndMealExpenses: Option[BigDecimal] = None,
                    otherAndCapitalAllowances: Option[BigDecimal] = None,
                    vehicleExpenses: Option[BigDecimal] = None,
                    mileageAllowanceRelief: Option[BigDecimal] = None) {

  def expensesPopulated(cyaExpenses: Option[ExpensesViewModel] = None): Boolean = {
    val hasExpenses: Boolean = cyaExpenses.exists(_.claimingEmploymentExpenses)
    hasExpenses || businessTravelCosts.isDefined || jobExpenses.isDefined || flatRateJobExpenses.isDefined ||
      professionalSubscriptions.isDefined || hotelAndMealExpenses.isDefined || otherAndCapitalAllowances.isDefined ||
      vehicleExpenses.isDefined || mileageAllowanceRelief.isDefined
  }

  def isEmpty: Boolean = {
    businessTravelCosts.isEmpty &&
      jobExpenses.isEmpty &&
      flatRateJobExpenses.isEmpty &&
      professionalSubscriptions.isEmpty &&
      hotelAndMealExpenses.isEmpty &&
      otherAndCapitalAllowances.isEmpty &&
      vehicleExpenses.isEmpty &&
      mileageAllowanceRelief.isEmpty
  }

  def toExpensesViewModel(isUsingCustomerData: Boolean, submittedOn: Option[String] = None,
                          cyaExpenses: Option[ExpensesViewModel] = None): ExpensesViewModel = {
    cyaExpenses.fold(
      ExpensesViewModel(claimingEmploymentExpenses = expensesPopulated(cyaExpenses),
        jobExpensesQuestion = Some(jobExpenses.isDefined),
        jobExpenses,
        flatRateJobExpensesQuestion = Some(flatRateJobExpenses.isDefined),
        flatRateJobExpenses,
        professionalSubscriptionsQuestion = Some(professionalSubscriptions.isDefined),
        professionalSubscriptions,
        otherAndCapitalAllowancesQuestion = Some(otherAndCapitalAllowances.isDefined),
        otherAndCapitalAllowances,
        businessTravelCosts,
        hotelAndMealExpenses,
        vehicleExpenses,
        mileageAllowanceRelief,
        submittedOn = submittedOn,
        isUsingCustomerData = isUsingCustomerData)
    )(x => x)
  }
}

object Expenses {
  implicit val format: OFormat[Expenses] = Json.format[Expenses]
}

case class EncryptedExpenses(businessTravelCosts: Option[EncryptedValue] = None,
                             jobExpenses: Option[EncryptedValue] = None,
                             flatRateJobExpenses: Option[EncryptedValue] = None,
                             professionalSubscriptions: Option[EncryptedValue] = None,
                             hotelAndMealExpenses: Option[EncryptedValue] = None,
                             otherAndCapitalAllowances: Option[EncryptedValue] = None,
                             vehicleExpenses: Option[EncryptedValue] = None,
                             mileageAllowanceRelief: Option[EncryptedValue] = None)

object EncryptedExpenses {
  implicit val format: OFormat[EncryptedExpenses] = Json.format[EncryptedExpenses]
}
