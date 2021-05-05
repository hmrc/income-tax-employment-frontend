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

package models

import play.api.libs.json.Json

case class ExpensesType(
                         businessTravelCosts: Option[BigDecimal],
                         jobExpenses: Option[BigDecimal],
                         flatRateJobExpenses: Option[BigDecimal],
                         professionalSubscriptions: Option[BigDecimal],
                         hotelAndMealExpenses: Option[BigDecimal],
                         otherAndCapitalAllowances: Option[BigDecimal],
                         vehicleExpenses: Option[BigDecimal],
                         mileageAllowanceRelief: Option[BigDecimal]
                       )

object ExpensesType {
  implicit val format = Json.format[ExpensesType]
}

case class GetEmploymentExpensesModel(
                                       submittedOn: Option[String],
                                       dateIgnored: Option[String],
                                       source: Option[String],
                                       totalExpenses: Option[BigDecimal],
                                       expenses: Option[ExpensesType]
                                     )

object GetEmploymentExpensesModel {
  implicit val format = Json.format[GetEmploymentExpensesModel]
}
