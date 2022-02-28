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

package audit

import models.expenses.Expenses
import play.api.libs.json.{Json, OWrites}

case class DeleteEmploymentExpensesAudit(taxYear: Int,
                                         userType: String,
                                         nino: String,
                                         mtditid: String,
                                         expenses: Expenses) {

  private def name = "DeleteEmploymentExpenses"

  def toAuditModel: AuditModel[DeleteEmploymentExpensesAudit] =
    AuditModel(
      auditType = name,
      transactionName = name,
      detail = this.copy(expenses = expenses.copy(
        businessTravelCosts = None,
        hotelAndMealExpenses = None,
        vehicleExpenses = None,
        mileageAllowanceRelief = None
      ))
    )
}

object DeleteEmploymentExpensesAudit {
  implicit def writes: OWrites[DeleteEmploymentExpensesAudit] = Json.writes[DeleteEmploymentExpensesAudit]
}
