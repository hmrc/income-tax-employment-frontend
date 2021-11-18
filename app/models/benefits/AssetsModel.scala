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

import controllers.employment.routes.CheckYourBenefitsController
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.EncryptedValue

case class AssetsModel(assetsAndAssetsTransferQuestion: Option[Boolean] = None,
                       assetsQuestion: Option[Boolean] = None,
                       assets: Option[BigDecimal] = None,
                       assetTransferQuestion: Option[Boolean] = None,
                       assetTransfer: Option[BigDecimal] = None) {

  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    assetsAndAssetsTransferQuestion match {
      case Some(true) =>
        (assetsSectionFinished, assetTransferSectionFinished) match {
          case (call@Some(_), _) => call
          case (_, call@Some(_)) => call
          case _ => None
        }
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }
  }

  def assetsSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    assetsQuestion match {
      case Some(true) => if (assets.isDefined) None else Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO assets amount page
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO assets yes no page
    }
  }

  def assetTransferSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    assetTransferQuestion match {
      case Some(true) => if (assetTransfer.isDefined) None else Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO assetTransfer amount page
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO assetTransfer yes no page
    }
  }
}

object AssetsModel {
  implicit val formats: OFormat[AssetsModel] = Json.format[AssetsModel]

  def clear: AssetsModel = AssetsModel(assetsAndAssetsTransferQuestion = Some(false))
}

case class EncryptedAssetsModel(assetsAndAssetsTransferQuestion: Option[EncryptedValue] = None,
                                assetsQuestion: Option[EncryptedValue] = None,
                                assets: Option[EncryptedValue] = None,
                                assetTransferQuestion: Option[EncryptedValue] = None,
                                assetTransfer: Option[EncryptedValue] = None)

object EncryptedAssetsModel {
  implicit val formats: OFormat[EncryptedAssetsModel] = Json.format[EncryptedAssetsModel]
}
