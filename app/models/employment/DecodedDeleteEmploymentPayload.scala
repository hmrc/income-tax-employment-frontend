
package models.employment

import models.benefits.Benefits
import models.expenses.DecodedExpensesData
import play.api.libs.json.{Json, OFormat}

case class DecodedDeleteEmploymentPayload(employmentData: EmploymentDetailsViewModel,
                                          benefits: Benefits,
                                          expenses: DecodedExpensesData)

object DecodedDeleteEmploymentPayload {
  implicit val format: OFormat[DecodedDeleteEmploymentPayload] = Json.format[DecodedDeleteEmploymentPayload]
}
