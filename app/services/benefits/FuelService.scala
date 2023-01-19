/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.Inject
import models.User
import models.benefits.{BenefitsViewModel, CarVanFuelModel}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService

import scala.concurrent.{ExecutionContext, Future}

class FuelService @Inject()(employmentSessionService: EmploymentSessionService,
                            implicit val ec: ExecutionContext) {

  def updateSectionQuestion(user: User,
                            taxYear: Int,
                            employmentId: String,
                            originalEmploymentUserData: EmploymentUserData,
                            questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val carVanFuel = cya.employmentBenefits.flatMap(_.carVanFuelModel)

    val updatedEmployment: EmploymentCYAModel = {
      carVanFuel match {
        case Some(carVanFuelModel) if questionValue =>
          cya.copy(employmentBenefits = benefits.map(_.copy(carVanFuelModel = Some(carVanFuelModel.copy(sectionQuestion = Some(true))))))
        case Some(_) =>
          cya.copy(employmentBenefits = benefits.map(_.copy(carVanFuelModel = Some(CarVanFuelModel.clear))))
        case _ =>
          cya.copy(employmentBenefits = benefits.map(_.copy(carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(questionValue))))))
      }
    }

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateCarQuestion(user: User,
                        taxYear: Int,
                        employmentId: String,
                        originalEmploymentUserData: EmploymentUserData,
                        questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val carVanFuel = cya.employmentBenefits.flatMap(_.carVanFuelModel)

    val updatedEmployment: EmploymentCYAModel = {
      if (questionValue) {
        cya.copy(employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(carQuestion = Some(true))))))
      } else {
        cya.copy(employmentBenefits = benefits.map(_.copy(
          carVanFuelModel = carVanFuel.map(_.copy(carQuestion = Some(false), car = None, carFuelQuestion = None, carFuel = None)))))
      }
    }

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateCar(user: User,
                taxYear: Int,
                employmentId: String,
                originalEmploymentUserData: EmploymentUserData,
                amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val carVanFuel = benefits.flatMap(_.carVanFuelModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(car = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateCarFuelQuestion(user: User,
                            taxYear: Int,
                            employmentId: String,
                            originalEmploymentUserData: EmploymentUserData,
                            questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val carVanFuel = benefits.flatMap(_.carVanFuelModel)

    val updatedEmployment: EmploymentCYAModel = {
      if (questionValue) {
        cya.copy(employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(carFuelQuestion = Some(true))))))
      } else {
        cya.copy(employmentBenefits = benefits.map(_.copy(
          carVanFuelModel = carVanFuel.map(_.copy(carFuelQuestion = Some(false), carFuel = None)))))
      }
    }

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateCarFuel(user: User,
                    taxYear: Int,
                    employmentId: String,
                    originalEmploymentUserData: EmploymentUserData,
                    amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val carVanFuel = benefits.flatMap(_.carVanFuelModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(carFuel = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateVanQuestion(user: User,
                        taxYear: Int,
                        employmentId: String,
                        originalEmploymentUserData: EmploymentUserData,
                        questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val carVanFuel = benefits.flatMap(_.carVanFuelModel)

    val updatedEmployment =
      if (questionValue) {
        cya.copy(employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(vanQuestion = Some(true))))))
      } else {
        cya.copy(employmentBenefits = benefits.map(_.copy(
          carVanFuelModel = carVanFuel.map(_.copy(vanQuestion = Some(false), van = None, vanFuelQuestion = None, vanFuel = None)))))
      }

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateVan(user: User,
                taxYear: Int,
                employmentId: String,
                originalEmploymentUserData: EmploymentUserData,
                amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val carVanFuel = cyaModel.employmentBenefits.flatMap(_.carVanFuelModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(van = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateVanFuelQuestion(user: User,
                            taxYear: Int,
                            employmentId: String,
                            originalEmploymentUserData: EmploymentUserData,
                            questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel: EmploymentCYAModel = originalEmploymentUserData.employment
    val benefits: Option[BenefitsViewModel] = cyaModel.employmentBenefits
    val carVanFuelModel: Option[CarVanFuelModel] = cyaModel.employmentBenefits.flatMap(_.carVanFuelModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(
        carVanFuelModel =
          if (questionValue) {
            carVanFuelModel.map(_.copy(vanFuelQuestion = Some(true)))
          } else {
            carVanFuelModel.map(_.copy(vanFuelQuestion = Some(false), vanFuel = None))
          }
      )))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateVanFuel(user: User,
                    taxYear: Int,
                    employmentId: String,
                    originalEmploymentUserData: EmploymentUserData,
                    amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val carVanFuel = cyaModel.employmentBenefits.flatMap(_.carVanFuelModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(vanFuel = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateMileageQuestion(user: User,
                            taxYear: Int,
                            employmentId: String,
                            originalEmploymentUserData: EmploymentUserData,
                            questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel: EmploymentCYAModel = originalEmploymentUserData.employment
    val benefits: Option[BenefitsViewModel] = cyaModel.employmentBenefits
    val carVanFuelModel: Option[CarVanFuelModel] = cyaModel.employmentBenefits.flatMap(_.carVanFuelModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(
        carVanFuelModel = if (questionValue) {
          carVanFuelModel.map(_.copy(mileageQuestion = Some(true)))
        } else {
          carVanFuelModel.map(_.copy(mileageQuestion = Some(false), mileage = None))
        }
      )))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateMileage(user: User,
                    taxYear: Int,
                    employmentId: String,
                    originalEmploymentUserData: EmploymentUserData,
                    amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val carVanFuel = cyaModel.employmentBenefits.flatMap(_.carVanFuelModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(mileage = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }
}
