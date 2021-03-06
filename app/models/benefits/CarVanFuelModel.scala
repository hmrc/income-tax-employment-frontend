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

import controllers.benefits.fuel.routes._
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.EncryptedValue

case class CarVanFuelModel(sectionQuestion: Option[Boolean] = None,
                           carQuestion: Option[Boolean] = None,
                           car: Option[BigDecimal] = None,
                           carFuelQuestion: Option[Boolean] = None,
                           carFuel: Option[BigDecimal] = None,
                           vanQuestion: Option[Boolean] = None,
                           van: Option[BigDecimal] = None,
                           vanFuelQuestion: Option[Boolean] = None,
                           vanFuel: Option[BigDecimal] = None,
                           mileageQuestion: Option[Boolean] = None,
                           mileage: Option[BigDecimal] = None
                          ) {
  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    sectionQuestion match {
      case Some(true) =>
        (fullCarSectionFinished, fullVanSectionFinished, mileageSectionFinished) match {
          case (call@Some(_), _, _) => call
          case (_, call@Some(_), _) => call
          case (_, _, call@Some(_)) => call
          case _ => None
        }
      case Some(false) => None
      case None => Some(CarVanFuelBenefitsController.show(taxYear, employmentId))
    }
  }

  def fullCarSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    (carSectionFinished, carFuelSectionFinished) match {
      case (call@Some(_), _) => call
      case (_, call@Some(_)) => call
      case _ => None
    }
  }

  def carSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    carQuestion match {
      case Some(true) => if (car.isDefined) None else Some(CompanyCarBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(CompanyCarBenefitsController.show(taxYear, employmentId))
    }
  }

  def carFuelSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    carFuelQuestion match {
      case Some(true) => if (carFuel.isDefined) None else Some(CarFuelBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => if (carQuestion.contains(false)) None else Some(CompanyCarFuelBenefitsController.show(taxYear, employmentId))
    }
  }

  def fullVanSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    (vanSectionFinished, vanFuelSectionFinished) match {
      case (call@Some(_), _) => call
      case (_, call@Some(_)) => call
      case _ => None
    }
  }

  def vanSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    vanQuestion match {
      case Some(true) => if (van.isDefined) None else Some(CompanyVanBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(CompanyVanBenefitsController.show(taxYear, employmentId))
    }
  }

  def vanFuelSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    vanFuelQuestion match {
      case Some(true) => if (vanFuel.isDefined) None else Some(CompanyVanFuelBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => if (vanQuestion.contains(false)) None else Some(CompanyVanFuelBenefitsController.show(taxYear, employmentId))
    }
  }

  def mileageSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    mileageQuestion match {
      case Some(true) => if (mileage.isDefined) None else Some(MileageBenefitAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(ReceiveOwnCarMileageBenefitController.show(taxYear, employmentId))
    }
  }
}

object CarVanFuelModel {
  implicit val formats: OFormat[CarVanFuelModel] = Json.format[CarVanFuelModel]

  def clear: CarVanFuelModel = CarVanFuelModel(sectionQuestion = Some(false))
}

case class EncryptedCarVanFuelModel(sectionQuestion: Option[EncryptedValue] = None,
                                    carQuestion: Option[EncryptedValue] = None,
                                    car: Option[EncryptedValue] = None,
                                    carFuelQuestion: Option[EncryptedValue] = None,
                                    carFuel: Option[EncryptedValue] = None,
                                    vanQuestion: Option[EncryptedValue] = None,
                                    van: Option[EncryptedValue] = None,
                                    vanFuelQuestion: Option[EncryptedValue] = None,
                                    vanFuel: Option[EncryptedValue] = None,
                                    mileageQuestion: Option[EncryptedValue] = None,
                                    mileage: Option[EncryptedValue] = None)

object EncryptedCarVanFuelModel {
  implicit val formats: OFormat[EncryptedCarVanFuelModel] = Json.format[EncryptedCarVanFuelModel]
}
