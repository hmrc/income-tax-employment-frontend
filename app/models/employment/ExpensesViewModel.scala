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

package models.employment

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{OFormat, __}
import utils.EncryptedValue

case class ExpensesViewModel(
                              businessTravelCosts: Option[BigDecimal] = None,
                              jobExpenses: Option[BigDecimal] = None,
                              flatRateJobExpenses: Option[BigDecimal] = None,
                              professionalSubscriptions: Option[BigDecimal] = None,
                              hotelAndMealExpenses: Option[BigDecimal] = None,
                              otherAndCapitalAllowances: Option[BigDecimal] = None,
                              vehicleExpenses: Option[BigDecimal] = None,
                              mileageAllowanceRelief: Option[BigDecimal] = None,
                              jobExpensesQuestion: Option[Boolean] = None,
                              flatRateJobExpensesQuestion: Option[Boolean] = None,
                              professionalSubscriptionsQuestion: Option[Boolean] = None,
                              otherAndCapitalAllowancesQuestion: Option[Boolean] = None,
                              submittedOn: Option[String] = None,
                              isUsingCustomerData: Boolean,
                              claimingEmploymentExpenses: Boolean = false
                            ) {

  def toExpenses: Expenses = {
    Expenses(
      businessTravelCosts, jobExpenses, flatRateJobExpenses, professionalSubscriptions, hotelAndMealExpenses,
      otherAndCapitalAllowances, vehicleExpenses, mileageAllowanceRelief
    )
  }
}

object ExpensesViewModel {

  def clear(isUsingCustomerData:Boolean): ExpensesViewModel = ExpensesViewModel(isUsingCustomerData = isUsingCustomerData)

  val firstSetOfFields: OFormat[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal])] = (
    (__ \ "businessTravelCosts").formatNullable[BigDecimal] and
      (__ \ "jobExpenses").formatNullable[BigDecimal] and
      (__ \ "flatRateJobExpenses").formatNullable[BigDecimal] and
      (__ \ "professionalSubscriptions").formatNullable[BigDecimal] and
      (__ \ "hotelAndMealExpenses").formatNullable[BigDecimal] and
      (__ \ "otherAndCapitalAllowances").formatNullable[BigDecimal] and
      (__ \ "vehicleExpenses").formatNullable[BigDecimal] and
      (__ \ "mileageAllowanceRelief").formatNullable[BigDecimal]
    ).tupled

  val secondSetOfFields: OFormat[(Option[Boolean], Option[Boolean], Option[Boolean],
    Option[Boolean], Option[String], Boolean, Boolean)] = (
    (__ \ "jobExpensesQuestion").formatNullable[Boolean] and
      (__ \ "flatRateJobExpensesQuestion").formatNullable[Boolean] and
      (__ \ "professionalSubscriptionsQuestion").formatNullable[Boolean] and
      (__ \ "otherAndCapitalAllowancesQuestion").formatNullable[Boolean] and
      (__ \ "submittedOn").formatNullable[String] and
      (__ \ "isUsingCustomerData").format[Boolean] and
      (__ \ "claimEmploymentExpenses").format[Boolean]
    ).tupled

  implicit val format: OFormat[ExpensesViewModel] = {
    (firstSetOfFields and secondSetOfFields).apply({
      case (
        (businessTravelCosts, jobExpenses, flatRateJobExpenses, professionalSubscriptions, hotelAndMealExpenses,
        otherAndCapitalAllowances, vehicleExpenses, mileageAllowanceRelief),
        (jobExpensesQuestion, flatRateJobExpensesQuestion, professionalSubscriptionsQuestion,
        otherAndCapitalAllowancesQuestion, submittedOn, isUsingCustomerData, claimEmploymentExpenses)
        ) =>
        ExpensesViewModel(
          businessTravelCosts, jobExpenses, flatRateJobExpenses, professionalSubscriptions, hotelAndMealExpenses,
          otherAndCapitalAllowances, vehicleExpenses, mileageAllowanceRelief, jobExpensesQuestion, flatRateJobExpensesQuestion,
          professionalSubscriptionsQuestion, otherAndCapitalAllowancesQuestion, submittedOn, isUsingCustomerData, claimEmploymentExpenses
        )
    }, {
      expenses =>
        (
          (expenses.businessTravelCosts, expenses.jobExpenses, expenses.flatRateJobExpenses, expenses.professionalSubscriptions,
            expenses.hotelAndMealExpenses, expenses.otherAndCapitalAllowances, expenses.vehicleExpenses, expenses.mileageAllowanceRelief),
          (expenses.jobExpensesQuestion, expenses.flatRateJobExpensesQuestion, expenses.professionalSubscriptionsQuestion,
            expenses.otherAndCapitalAllowancesQuestion, expenses.submittedOn, expenses.isUsingCustomerData, expenses.claimingEmploymentExpenses)
        )
    })

  }
}

case class EncryptedExpensesViewModel(
                                       businessTravelCosts: Option[EncryptedValue] = None,
                                       jobExpenses: Option[EncryptedValue] = None,
                                       flatRateJobExpenses: Option[EncryptedValue] = None,
                                       professionalSubscriptions: Option[EncryptedValue] = None,
                                       hotelAndMealExpenses: Option[EncryptedValue] = None,
                                       otherAndCapitalAllowances: Option[EncryptedValue] = None,
                                       vehicleExpenses: Option[EncryptedValue] = None,
                                       mileageAllowanceRelief: Option[EncryptedValue] = None,
                                       jobExpensesQuestion: Option[EncryptedValue] = None,
                                       flatRateJobExpensesQuestion: Option[EncryptedValue] = None,
                                       professionalSubscriptionsQuestion: Option[EncryptedValue] = None,
                                       otherAndCapitalAllowancesQuestion: Option[EncryptedValue] = None,
                                       submittedOn: Option[EncryptedValue],
                                       isUsingCustomerData: EncryptedValue,
                                       claimingEmploymentExpenses: EncryptedValue
                                     )

object EncryptedExpensesViewModel {

  val firstSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue])] = (
    (__ \ "businessTravelCosts").formatNullable[EncryptedValue] and
      (__ \ "jobExpenses").formatNullable[EncryptedValue] and
      (__ \ "flatRateJobExpenses").formatNullable[EncryptedValue] and
      (__ \ "professionalSubscriptions").formatNullable[EncryptedValue] and
      (__ \ "hotelAndMealExpenses").formatNullable[EncryptedValue] and
      (__ \ "otherAndCapitalAllowances").formatNullable[EncryptedValue] and
      (__ \ "vehicleExpenses").formatNullable[EncryptedValue] and
      (__ \ "mileageAllowanceRelief").formatNullable[EncryptedValue]
    ).tupled

  val secondSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], EncryptedValue, EncryptedValue)] = (
    (__ \ "jobExpensesQuestion").formatNullable[EncryptedValue] and
      (__ \ "flatRateJobExpensesQuestion").formatNullable[EncryptedValue] and
      (__ \ "professionalSubscriptionsQuestion").formatNullable[EncryptedValue] and
      (__ \ "otherAndCapitalAllowancesQuestion").formatNullable[EncryptedValue] and
      (__ \ "submittedOn").formatNullable[EncryptedValue] and
      (__ \ "isUsingCustomerData").format[EncryptedValue] and
      (__ \ "claimEmploymentExpenses").format[EncryptedValue]
    ).tupled

  implicit val format: OFormat[EncryptedExpensesViewModel] = {
    (firstSetOfFields and secondSetOfFields).apply({
      case (
        (businessTravelCosts, jobExpenses, flatRateJobExpenses, professionalSubscriptions, hotelAndMealExpenses,
        otherAndCapitalAllowances, vehicleExpenses, mileageAllowanceRelief),
        (jobExpensesQuestion, flatRateJobExpensesQuestion, professionalSubscriptionsQuestion,
        otherAndCapitalAllowancesQuestion, submittedOn, isUsingCustomerData, claimEmploymentExpenses)
        ) =>
        EncryptedExpensesViewModel(
          businessTravelCosts, jobExpenses, flatRateJobExpenses, professionalSubscriptions, hotelAndMealExpenses,
          otherAndCapitalAllowances, vehicleExpenses, mileageAllowanceRelief, jobExpensesQuestion, flatRateJobExpensesQuestion,
          professionalSubscriptionsQuestion, otherAndCapitalAllowancesQuestion, submittedOn, isUsingCustomerData, claimEmploymentExpenses
        )
    }, {
      expenses =>
        (
          (expenses.businessTravelCosts, expenses.jobExpenses, expenses.flatRateJobExpenses, expenses.professionalSubscriptions,
            expenses.hotelAndMealExpenses, expenses.otherAndCapitalAllowances, expenses.vehicleExpenses, expenses.mileageAllowanceRelief),
          (expenses.jobExpensesQuestion, expenses.flatRateJobExpensesQuestion, expenses.professionalSubscriptionsQuestion,
            expenses.otherAndCapitalAllowancesQuestion, expenses.submittedOn, expenses.isUsingCustomerData, expenses.claimingEmploymentExpenses)
        )
    })

  }
}



