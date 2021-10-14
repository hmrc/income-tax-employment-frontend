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

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class AccommodationRelocationModel(
                                       accommodationRelocationQuestion: Option[Boolean] = None,
                                       accommodationQuestion: Option[Boolean] = None,
                                       accommodation: Option[BigDecimal] = None,
                                       qualifyingRelocationExpensesQuestion: Option[Boolean] = None,
                                       qualifyingRelocationExpenses: Option[BigDecimal] = None,
                                       nonQualifyingRelocationExpensesQuestion: Option[Boolean] = None,
                                       nonQualifyingRelocationExpenses: Option[BigDecimal] = None
                                       )

object AccommodationRelocationModel{
  implicit val formats: OFormat[AccommodationRelocationModel] = Json.format[AccommodationRelocationModel]

  def clear: AccommodationRelocationModel = AccommodationRelocationModel(accommodationRelocationQuestion = Some(false))
}


case class EncryptedAccommodationRelocationModel(
                                         accommodationRelocationQuestion: Option[EncryptedValue] = None,
                                         accommodationQuestion: Option[EncryptedValue] = None,
                                         accommodation: Option[EncryptedValue] = None,
                                         qualifyingRelocationExpensesQuestion: Option[EncryptedValue] = None,
                                         qualifyingRelocationExpenses: Option[EncryptedValue] = None,
                                         nonQualifyingRelocationExpensesQuestion: Option[EncryptedValue] = None,
                                         nonQualifyingRelocationExpenses: Option[EncryptedValue] = None
                                       )

object EncryptedAccommodationRelocationModel{
  implicit val formats: OFormat[EncryptedAccommodationRelocationModel] = Json.format[EncryptedAccommodationRelocationModel]
}
