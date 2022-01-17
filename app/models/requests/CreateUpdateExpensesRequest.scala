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

package models.requests

import audit.{AmendEmploymentExpensesUpdateAudit, AuditEmploymentExpensesData, AuditNewEmploymentExpensesData, CreateNewEmploymentExpensesAudit}
import models.User
import models.employment.EmploymentExpenses
import models.expenses.{DecodedAmendExpensesPayload, DecodedCreateNewExpensesPayload, Expenses}
import play.api.libs.json.{Json, OFormat}

case class CreateUpdateExpensesRequest(ignoreExpenses: Option[Boolean], expenses: Expenses) {

  def toAmendAuditModel(taxYear: Int, priorData: EmploymentExpenses)(implicit user: User[_]): AmendEmploymentExpensesUpdateAudit = {

    AmendEmploymentExpensesUpdateAudit(
      taxYear = taxYear,
      userType = user.affinityGroup.toLowerCase,
      nino = user.nino,
      mtditid = user.mtditid,
      priorEmploymentExpensesData = AuditEmploymentExpensesData(
        jobExpenses = priorData.expenses.flatMap(_.jobExpenses),
        flatRateJobExpenses = priorData.expenses.flatMap(_.flatRateJobExpenses),
        professionalSubscriptions = priorData.expenses.flatMap(_.professionalSubscriptions),
        otherAndCapitalAllowances = priorData.expenses.flatMap(_.otherAndCapitalAllowances)
      ),
      employmentExpensesData = AuditEmploymentExpensesData(
        jobExpenses = expenses.jobExpenses,
        flatRateJobExpenses = expenses.flatRateJobExpenses,
        professionalSubscriptions = expenses.professionalSubscriptions,
        otherAndCapitalAllowances = expenses.otherAndCapitalAllowances
      )
    )
  }

  def toCreateAuditModel(taxYear: Int)(implicit user: User[_]): CreateNewEmploymentExpensesAudit = {

    CreateNewEmploymentExpensesAudit(
      taxYear = taxYear,
      userType = user.affinityGroup.toLowerCase,
      nino = user.nino,
      mtditid = user.mtditid,
      employmentExpensesData = AuditNewEmploymentExpensesData(
        jobExpenses = expenses.jobExpenses,
        flatRateJobExpenses = expenses.flatRateJobExpenses,
        professionalSubscriptions = expenses.professionalSubscriptions,
        otherAndCapitalAllowances = expenses.otherAndCapitalAllowances
      )
    )
  }

  def toCreateDecodedExpensesPayloadModel()(implicit user: User[_]): DecodedCreateNewExpensesPayload = {

    DecodedCreateNewExpensesPayload(
      employmentExpensesData = Expenses(
        businessTravelCosts = expenses.businessTravelCosts,
        jobExpenses = expenses.jobExpenses,
        flatRateJobExpenses = expenses.flatRateJobExpenses,
        professionalSubscriptions = expenses.professionalSubscriptions,
        hotelAndMealExpenses = expenses.hotelAndMealExpenses,
        otherAndCapitalAllowances = expenses.otherAndCapitalAllowances,
        vehicleExpenses = expenses.vehicleExpenses,
        mileageAllowanceRelief = expenses.mileageAllowanceRelief
      )
    )
  }


  def toAmendDecodedExpensesPayloadModel(priorData: EmploymentExpenses)(implicit user: User[_]): DecodedAmendExpensesPayload = {

    DecodedAmendExpensesPayload(
      priorEmploymentExpensesData = Expenses(
        businessTravelCosts = priorData.expenses.flatMap(_.businessTravelCosts),
        jobExpenses = priorData.expenses.flatMap(_.jobExpenses),
        flatRateJobExpenses = priorData.expenses.flatMap(_.flatRateJobExpenses),
        professionalSubscriptions = priorData.expenses.flatMap(_.professionalSubscriptions),
        hotelAndMealExpenses = priorData.expenses.flatMap(_.hotelAndMealExpenses),
        otherAndCapitalAllowances = priorData.expenses.flatMap(_.otherAndCapitalAllowances),
        vehicleExpenses =  priorData.expenses.flatMap(_.vehicleExpenses),
        mileageAllowanceRelief = priorData.expenses.flatMap(_.mileageAllowanceRelief)
      ),
      employmentExpensesData = Expenses(
        businessTravelCosts = expenses.businessTravelCosts,
        jobExpenses = expenses.jobExpenses,
        flatRateJobExpenses = expenses.flatRateJobExpenses,
        professionalSubscriptions = expenses.professionalSubscriptions,
        hotelAndMealExpenses = expenses.hotelAndMealExpenses,
        otherAndCapitalAllowances = expenses.otherAndCapitalAllowances,
        vehicleExpenses = expenses.vehicleExpenses,
        mileageAllowanceRelief = expenses.mileageAllowanceRelief
      )
    )
  }
}

object CreateUpdateExpensesRequest {
  implicit val format: OFormat[CreateUpdateExpensesRequest] = Json.format[CreateUpdateExpensesRequest]
}
