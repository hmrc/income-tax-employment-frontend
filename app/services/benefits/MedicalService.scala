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
import models.benefits.MedicalChildcareEducationModel
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService
import utils.Clock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MedicalService @Inject()(employmentSessionService: EmploymentSessionService,
                               implicit val ec: ExecutionContext) {

  def updateSectionQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                           (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val medicalChildcareEducationModel = benefits.flatMap(_.medicalChildcareEducationModel)

    val updatedEmployment = medicalChildcareEducationModel match {
      case Some(_) if !questionValue =>
        cya.copy(employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel = Some(MedicalChildcareEducationModel.clear))))
      case medicalChildcareEducation => cya.copy(employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel = Some(
        medicalChildcareEducation.map(_.copy(sectionQuestion = Some(questionValue)))
          .getOrElse(MedicalChildcareEducationModel(sectionQuestion = Some(questionValue)))))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateMedicalInsuranceQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                                    (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val medicalChildcareEducationModel = benefits.flatMap(_.medicalChildcareEducationModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel =
        medicalChildcareEducationModel.map(_.copy(medicalInsuranceQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel =
        medicalChildcareEducationModel.map(_.copy(medicalInsuranceQuestion = Some(false), medicalInsurance = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateMedicalInsurance(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                            (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val medicalAndChildcare: Option[MedicalChildcareEducationModel] = cyaModel.employmentBenefits.flatMap(_.medicalChildcareEducationModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = cyaModel.employmentBenefits.map(_.copy(medicalChildcareEducationModel =
      medicalAndChildcare.map(_.copy(medicalInsurance = Some(amount))))))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateNurseryPlacesQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                                 (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val medicalChildcareEducationModel = cya.employmentBenefits.flatMap(_.medicalChildcareEducationModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel =
        medicalChildcareEducationModel.map(_.copy(nurseryPlacesQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel =
        medicalChildcareEducationModel.map(_.copy(nurseryPlacesQuestion = Some(false), nurseryPlaces = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateNurseryPlaces(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                         (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val medicalChildcareEducationModel = benefits.flatMap(_.medicalChildcareEducationModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel = medicalChildcareEducationModel.map(_.copy(nurseryPlaces = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateEducationalServicesQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                                       (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val medicalChildcareEducationModel = benefits.flatMap(_.medicalChildcareEducationModel)

    val updatedEmployment =
      if (questionValue) {
        cya.copy(employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel =
          medicalChildcareEducationModel.map(_.copy(educationalServicesQuestion = Some(true))))))
      } else {
        cya.copy(employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel =
          medicalChildcareEducationModel.map(_.copy(educationalServicesQuestion = Some(false), educationalServices = None)))))
      }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateEducationalServices(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val medicalChildcareEducationModel = cyaModel.employmentBenefits.flatMap(_.medicalChildcareEducationModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = benefits.map(_.copy(
      medicalChildcareEducationModel = medicalChildcareEducationModel.map(_.copy(educationalServices = Some(amount)))
    )))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateBeneficialLoanQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                                  (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val medicalChildcareEducationModel = benefits.flatMap(_.medicalChildcareEducationModel)

    val updatedEmployment: EmploymentCYAModel = {
      if (questionValue) {
        cya.copy(employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel =
          medicalChildcareEducationModel.map(_.copy(beneficialLoanQuestion = Some(true))))))
      } else {
        cya.copy(employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel =
          medicalChildcareEducationModel.map(_.copy(beneficialLoanQuestion = Some(false), beneficialLoan = None)))))
      }
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateBeneficialLoan(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                          (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val medicalChildcareEducationModel = benefits.flatMap(_.medicalChildcareEducationModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(medicalChildcareEducationModel = medicalChildcareEducationModel.map(_.copy(beneficialLoan = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }
}
