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

import controllers.employment.routes.CheckYourBenefitsController
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.EncryptedValue

case class TravelEntertainmentModel(
                            travelEntertainmentQuestion: Option[Boolean] = None,
                            travelAndSubsistenceQuestion: Option[Boolean] = None,
                            travelAndSubsistence: Option[BigDecimal] = None,
                            personalIncidentalExpensesQuestion: Option[Boolean] = None,
                            personalIncidentalExpenses: Option[BigDecimal] = None,
                            entertainingQuestion: Option[Boolean] = None,
                            entertaining: Option[BigDecimal] = None
                          ){
  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] ={

    travelEntertainmentQuestion match {
      case Some(true) =>
        (travelSectionFinished,personalIncidentalSectionFinished,entertainingSectionFinished) match {
          case (call@Some(_), _, _) => call
          case (_, call@Some(_), _) => call
          case (_, _, call@Some(_)) => call
          case _ => None
        }
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO update to my new page
    }
  }

  def travelSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] ={
    travelAndSubsistenceQuestion match {
      case Some(true) => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO update to travel amount page
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO update to first travel page
    }
  }

  def personalIncidentalSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] ={
    personalIncidentalExpensesQuestion match {
      case Some(true) => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO update to personal incidental amount page
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO update to first personal incidental page
    }
  }

  def entertainingSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] ={
    entertainingQuestion match {
      case Some(true) => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO update to entertaining amount page
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO update to first entertaining page
    }
  }

}

object TravelEntertainmentModel {
  implicit val formats: OFormat[TravelEntertainmentModel] = Json.format[TravelEntertainmentModel]

  def clear: TravelEntertainmentModel = TravelEntertainmentModel(travelEntertainmentQuestion = Some(false))
}

case class EncryptedTravelEntertainmentModel(travelEntertainmentQuestion: Option[EncryptedValue] = None,
                                             travelAndSubsistenceQuestion: Option[EncryptedValue] = None,
                                             travelAndSubsistence: Option[EncryptedValue] = None,
                                             personalIncidentalExpensesQuestion: Option[EncryptedValue] = None,
                                             personalIncidentalExpenses: Option[EncryptedValue] = None,
                                             entertainingQuestion: Option[EncryptedValue] = None,
                                             entertaining: Option[EncryptedValue] = None)

object EncryptedTravelEntertainmentModel {
  implicit val formats: OFormat[EncryptedTravelEntertainmentModel] = Json.format[EncryptedTravelEntertainmentModel]
}
