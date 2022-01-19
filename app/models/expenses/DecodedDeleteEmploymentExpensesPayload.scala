
package models.expenses

import play.api.libs.json.{Json, OFormat}

case class DecodedDeleteEmploymentExpensesPayload(expenses: DecodedExpensesData)

object DecodedDeleteEmploymentExpensesPayload {
  implicit val format: OFormat[DecodedDeleteEmploymentExpensesPayload] = Json.format[DecodedDeleteEmploymentExpensesPayload]
}
