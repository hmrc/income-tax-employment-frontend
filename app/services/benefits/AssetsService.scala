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

package services.benefits

import models.User
import models.benefits.AssetsModel
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService
import utils.Clock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AssetsService @Inject()(employmentSessionService: EmploymentSessionService,
                              implicit val ec: ExecutionContext) {

  def updateSectionQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                           (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val assetsModel = benefits.flatMap(_.assetsModel)

    val updatedEmployment = assetsModel match {
      case Some(assetsModel) if questionValue => cya.copy(employmentBenefits = benefits.map(_.copy(
        assetsModel = Some(assetsModel.copy(sectionQuestion = Some(true))))))
      case Some(_) => cya.copy(employmentBenefits = benefits.map(_.copy(assetsModel = Some(AssetsModel.clear))))
      case _ => cya.copy(employmentBenefits = benefits.map(_.copy(assetsModel = Some(AssetsModel(sectionQuestion = Some(questionValue))))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateAssetsQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                          (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val assetsModel = benefits.flatMap(_.assetsModel)

    val updatedEmployment: EmploymentCYAModel = {
      if (questionValue) {
        cya.copy(employmentBenefits = benefits.map(_.copy(assetsModel = assetsModel.map(_.copy(assetsQuestion = Some(true))))))
      } else {
        cya.copy(employmentBenefits = benefits.map(_.copy(
          assetsModel = assetsModel.map(_.copy(assetsQuestion = Some(false), assets = None)))))
      }
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateAssets(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                  (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val assetsModel = benefits.flatMap(_.assetsModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(assetsModel = assetsModel.map(_.copy(assets = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateAssetTransferQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                                 (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val assetsModel = benefits.flatMap(_.assetsModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(assetsModel = assetsModel.map(_.copy(assetTransferQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(
        assetsModel = assetsModel.map(_.copy(assetTransferQuestion = Some(false), assetTransfer = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateAssetTransfer(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                         (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val assetsModel = benefits.flatMap(_.assetsModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(assetsModel = assetsModel.map(_.copy(assetTransfer = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }
}
