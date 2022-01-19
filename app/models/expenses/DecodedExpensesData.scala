
package models.expenses

import play.api.libs.json.{Json, OFormat}

case class DecodedExpensesData(jobExpenses: Option[BigDecimal],
                               flatRateJobExpenses: Option[BigDecimal],
                               professionalSubscriptions: Option[BigDecimal],
                               otherAndCapitalAllowances: Option[BigDecimal])

object DecodedExpensesData {
  implicit val format: OFormat[DecodedExpensesData] = Json.format[DecodedExpensesData]
}
