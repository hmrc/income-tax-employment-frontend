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

import models.benefits.Benefits
import models.employment.EmploymentDetailsViewModel
import models.expenses.Expenses
import play.api.libs.json.{Json, OWrites}

case class DeleteEmploymentAudit(taxYear: Int,
                                 userType: String,
                                 nino: String,
                                 mtditid: String,
                                 employmentData: EmploymentDetailsViewModel,
                                 benefits: Option[Benefits],
                                 expenses: Option[Expenses]) {

  private def name = "DeleteEmployment"

  def toAuditModel: AuditModel[DeleteEmploymentAudit] = {
    val maybeExpenses = expenses.map(_.copy(
      businessTravelCosts = None,
      hotelAndMealExpenses = None,
      vehicleExpenses = None,
      mileageAllowanceRelief = None
    ))

    AuditModel(name, name, this.copy(expenses = maybeExpenses))
  }
}

object DeleteEmploymentAudit {
  implicit def writes: OWrites[DeleteEmploymentAudit] = Json.writes[DeleteEmploymentAudit]
}