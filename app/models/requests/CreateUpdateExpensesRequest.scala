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

package models.requests

import audit.{AmendEmploymentExpensesUpdateAudit, AuditEmploymentExpensesData, AuditNewEmploymentExpensesData, CreateNewEmploymentExpensesAudit}
import models.User
import models.employment.EmploymentExpenses
import models.expenses.Expenses
import play.api.libs.json.{Json, OFormat}

case class CreateUpdateExpensesRequest(ignoreExpenses: Option[Boolean], expenses: Expenses) {
  def toAmendAuditModel(taxYear: Int, priorData: EmploymentExpenses)(implicit user: User[_]): AmendEmploymentExpensesUpdateAudit = {

    def currentOrPrior[T](data: Option[T], priorData: Option[T]): Option[T] = {
      (data, priorData) match {
        case (data@Some(_), _) => data
        case (_, priorData@Some(_)) => priorData
        case _ => None
      }
    }

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
        jobExpenses = currentOrPrior(expenses.jobExpenses, priorData.expenses.flatMap(_.jobExpenses)),
        flatRateJobExpenses = currentOrPrior(expenses.flatRateJobExpenses, priorData.expenses.flatMap(_.flatRateJobExpenses)),
        professionalSubscriptions = currentOrPrior(expenses.professionalSubscriptions, priorData.expenses.flatMap(_.professionalSubscriptions)),
        otherAndCapitalAllowances = currentOrPrior(expenses.otherAndCapitalAllowances, priorData.expenses.flatMap(_.otherAndCapitalAllowances))
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
}

object CreateUpdateExpensesRequest {
  implicit val format: OFormat[CreateUpdateExpensesRequest] = Json.format[CreateUpdateExpensesRequest]
}
