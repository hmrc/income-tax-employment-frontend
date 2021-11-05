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

package models.benefits

import controllers.benefits.accommodationAndRelocation.routes._
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.EncryptedValue

case class AccommodationRelocationModel(
                                         accommodationRelocationQuestion: Option[Boolean] = None,
                                         accommodationQuestion: Option[Boolean] = None,
                                         accommodation: Option[BigDecimal] = None,
                                         qualifyingRelocationExpensesQuestion: Option[Boolean] = None,
                                         qualifyingRelocationExpenses: Option[BigDecimal] = None,
                                         nonQualifyingRelocationExpensesQuestion: Option[Boolean] = None,
                                         nonQualifyingRelocationExpenses: Option[BigDecimal] = None
                                       ) {

  def qualifyingRelocationSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    qualifyingRelocationExpensesQuestion match {
      case Some(true) => if (qualifyingRelocationExpenses.isDefined) None else Some(QualifyingRelocationBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(QualifyingRelocationBenefitsController.show(taxYear, employmentId))
    }
  }

  //scalastyle:off
  def accommodationSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    accommodationQuestion match {
      case Some(true) => if (accommodation.isDefined) None else Some(LivingAccommodationBenefitAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(LivingAccommodationBenefitsController.show(taxYear, employmentId))
    }
  }

  def nonQualifyingRelocationSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    nonQualifyingRelocationExpensesQuestion match {
      case Some(true) => if (nonQualifyingRelocationExpenses.isDefined) None else Some(AccommodationRelocationBenefitsController.show(taxYear, employmentId)) // TODO non qual relocation amount page
      case Some(false) => None
      case None => Some(NonQualifyingRelocationBenefitsController.show(taxYear, employmentId))
    }
  }
  //scalastyle:on

  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    accommodationRelocationQuestion match {
      case Some(true) =>
        (accommodationSectionFinished, qualifyingRelocationSectionFinished, nonQualifyingRelocationSectionFinished) match {
          case (call@Some(_), _, _) => call
          case (_, call@Some(_), _) => call
          case (_, _, call@Some(_)) => call
          case _ => None
        }
      case Some(false) => None
      case None => Some(AccommodationRelocationBenefitsController.show(taxYear, employmentId))
    }
  }
}

object AccommodationRelocationModel {
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

object EncryptedAccommodationRelocationModel {
  implicit val formats: OFormat[EncryptedAccommodationRelocationModel] = Json.format[EncryptedAccommodationRelocationModel]
}
