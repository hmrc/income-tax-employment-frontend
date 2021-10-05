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
import play.api.mvc.Call
import utils.EncryptedValue
import controllers.benefits.routes._
import controllers.employment.routes._

case class CarVanFuelModel(
                            carVanFuelQuestion: Option[Boolean] = None,
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
                          ){
//  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] ={
//
//    carVanFuelQuestion match {
//      case Some(true) => carSectionFinished && vanSectionFinished && mileageSectionFinished
//      case Some(false) => None
//      case None => false
//    }
//  }
//
//  def carSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] ={
//    carQuestion match {
//      case Some(true) => carFuelSectionFinished
//      case Some(false) => None
//      case None => false
//    }
//  }
//
//  def carFuelSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
//    carFuelQuestion match {
//      case Some(true) => carFuel.isDefined
//      case Some(false) => None
//      case None => false
//    }
//  }
//
//  def vanSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] ={
//    vanQuestion match {
//      case Some(true) => vanFuelSectionFinished
//      case Some(false) => None
//      case None => false
//    }
//  }
//
//  def vanFuelSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
//    vanFuelQuestion match {
//      case Some(true) => vanFuel.isDefined
//      case Some(false) => None
//      case None => false
//    }
//  }
//
//  def mileageSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
//    mileageQuestion match {
//      case Some(true) => mileage.isDefined
//      case Some(false) => None
//      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) // TODO Mileage yes no
//    }
//  }
}

object CarVanFuelModel {
  implicit val formats: OFormat[CarVanFuelModel] = Json.format[CarVanFuelModel]

  def clear: CarVanFuelModel = CarVanFuelModel(carVanFuelQuestion = Some(false))
}

case class EncryptedCarVanFuelModel(carVanFuelQuestion: Option[EncryptedValue] = None,
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
