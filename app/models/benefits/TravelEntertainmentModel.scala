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

package models.benefits

import controllers.benefits.travel.routes._
import controllers.employment.routes.CheckYourBenefitsController
import models.mongo.TextAndKey
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

case class TravelEntertainmentModel(sectionQuestion: Option[Boolean] = None,
                                    travelAndSubsistenceQuestion: Option[Boolean] = None,
                                    travelAndSubsistence: Option[BigDecimal] = None,
                                    personalIncidentalExpensesQuestion: Option[Boolean] = None,
                                    personalIncidentalExpenses: Option[BigDecimal] = None,
                                    entertainingQuestion: Option[Boolean] = None,
                                    entertaining: Option[BigDecimal] = None) {

  def travelSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    travelAndSubsistenceQuestion match {
      case Some(true) => if (travelAndSubsistence.isDefined) None else Some(TravelOrSubsistenceBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(TravelAndSubsistenceBenefitsController.show(taxYear, employmentId))
    }
  }

  def personalIncidentalSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    personalIncidentalExpensesQuestion match {
      case Some(true) => if (personalIncidentalExpenses.isDefined) None else Some(IncidentalCostsBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO update to first personal incidental page
    }
  }

  //scalastyle:off
  def entertainingSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    entertainingQuestion match {
      case Some(true) => if (entertaining.isDefined) None else Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO update to entertaining amount page
      case Some(false) => None
      case None => Some(EntertainingBenefitsController.show(taxYear, employmentId))
    }
  }
  //scalastyle:on

  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    sectionQuestion match {
      case Some(true) =>
        (travelSectionFinished, personalIncidentalSectionFinished, entertainingSectionFinished) match {
          case (call@Some(_), _, _) => call
          case (_, call@Some(_), _) => call
          case (_, _, call@Some(_)) => call
          case _ => None
        }
      case Some(false) => None
      case None => Some(TravelOrEntertainmentBenefitsController.show(taxYear, employmentId))
    }
  }

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedTravelEntertainmentModel = EncryptedTravelEntertainmentModel(
    sectionQuestion = sectionQuestion.map(_.encrypted),
    travelAndSubsistenceQuestion = travelAndSubsistenceQuestion.map(_.encrypted),
    travelAndSubsistence = travelAndSubsistence.map(_.encrypted),
    personalIncidentalExpensesQuestion = personalIncidentalExpensesQuestion.map(_.encrypted),
    personalIncidentalExpenses = personalIncidentalExpenses.map(_.encrypted),
    entertainingQuestion = entertainingQuestion.map(_.encrypted),
    entertaining = entertaining.map(_.encrypted)
  )
}

object TravelEntertainmentModel {
  implicit val formats: OFormat[TravelEntertainmentModel] = Json.format[TravelEntertainmentModel]

  def clear: TravelEntertainmentModel = TravelEntertainmentModel(sectionQuestion = Some(false))
}

case class EncryptedTravelEntertainmentModel(sectionQuestion: Option[EncryptedValue] = None,
                                             travelAndSubsistenceQuestion: Option[EncryptedValue] = None,
                                             travelAndSubsistence: Option[EncryptedValue] = None,
                                             personalIncidentalExpensesQuestion: Option[EncryptedValue] = None,
                                             personalIncidentalExpenses: Option[EncryptedValue] = None,
                                             entertainingQuestion: Option[EncryptedValue] = None,
                                             entertaining: Option[EncryptedValue] = None) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher,
                  textAndKey: TextAndKey): TravelEntertainmentModel = TravelEntertainmentModel(
    sectionQuestion = sectionQuestion.map(_.decrypted[Boolean]),
    travelAndSubsistenceQuestion = travelAndSubsistenceQuestion.map(_.decrypted[Boolean]),
    travelAndSubsistence = travelAndSubsistence.map(_.decrypted[BigDecimal]),
    personalIncidentalExpensesQuestion = personalIncidentalExpensesQuestion.map(_.decrypted[Boolean]),
    personalIncidentalExpenses = personalIncidentalExpenses.map(_.decrypted[BigDecimal]),
    entertainingQuestion = entertainingQuestion.map(_.decrypted[Boolean]),
    entertaining = entertaining.map(_.decrypted[BigDecimal])
  )
}

object EncryptedTravelEntertainmentModel {
  implicit val formats: OFormat[EncryptedTravelEntertainmentModel] = Json.format[EncryptedTravelEntertainmentModel]
}
