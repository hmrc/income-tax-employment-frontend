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
import models.benefits.UtilitiesAndServicesModel
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UtilitiesService @Inject()(employmentSessionService: EmploymentSessionService,
                                 implicit val ec: ExecutionContext) {

  def updateSectionQuestion(user: User,
                            taxYear: Int,
                            employmentId: String,
                            originalEmploymentUserData: EmploymentUserData,
                            questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val utilitiesAndServices = cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel)

    val updatedEmployment = utilitiesAndServices match {
      case Some(_) if !questionValue =>
        cya.copy(employmentBenefits = benefits.map(_.copy(utilitiesAndServicesModel = Some(UtilitiesAndServicesModel.clear))))
      case utilitiesAndServices => cya.copy(employmentBenefits = benefits.map(_.copy(utilitiesAndServicesModel = Some(utilitiesAndServices
        .map(_.copy(sectionQuestion = Some(questionValue)))
        .getOrElse(UtilitiesAndServicesModel(sectionQuestion = Some(questionValue)))
      ))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateTelephoneQuestion(user: User,
                              taxYear: Int,
                              employmentId: String,
                              originalEmploymentUserData: EmploymentUserData,
                              questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val utilitiesModel = benefits.flatMap(_.utilitiesAndServicesModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(utilitiesAndServicesModel =
        utilitiesModel.map(_.copy(telephoneQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(
        utilitiesAndServicesModel = utilitiesModel.map(_.copy(telephoneQuestion = Some(false), telephone = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateTelephone(user: User,
                      taxYear: Int,
                      employmentId: String,
                      originalEmploymentUserData: EmploymentUserData,
                      amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val utilitiesAndServices = cyaModel.employmentBenefits.flatMap(_.utilitiesAndServicesModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = cyaModel.employmentBenefits.map(_.copy(utilitiesAndServicesModel =
      utilitiesAndServices.map(_.copy(telephone = Some(amount))))))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateEmployerProvidedServicesQuestion(user: User,
                                             taxYear: Int,
                                             employmentId: String,
                                             originalEmploymentUserData: EmploymentUserData,
                                             questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val utilitiesModel = benefits.flatMap(_.utilitiesAndServicesModel)

    val updatedEmployment: EmploymentCYAModel = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(utilitiesAndServicesModel =
        utilitiesModel.map(_.copy(employerProvidedServicesQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(utilitiesAndServicesModel =
        utilitiesModel.map(_.copy(employerProvidedServicesQuestion = Some(false), employerProvidedServices = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateEmployerProvidedServices(user: User,
                                     taxYear: Int,
                                     employmentId: String,
                                     originalEmploymentUserData: EmploymentUserData,
                                     amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val utilitiesAndServices = cyaModel.employmentBenefits.flatMap(_.utilitiesAndServicesModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = cyaModel.employmentBenefits.map(_.copy(utilitiesAndServicesModel =
      utilitiesAndServices.map(_.copy(employerProvidedServices = Some(amount))))))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateEmployerProvidedProfessionalSubscriptionsQuestion(user: User,
                                                              taxYear: Int,
                                                              employmentId: String,
                                                              originalEmploymentUserData: EmploymentUserData,
                                                              questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val utilitiesModel = benefits.flatMap(_.utilitiesAndServicesModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(utilitiesAndServicesModel =
        utilitiesModel.map(_.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(utilitiesAndServicesModel = utilitiesModel.map(_.copy(
        employerProvidedProfessionalSubscriptionsQuestion = Some(false), employerProvidedProfessionalSubscriptions = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateEmployerProvidedProfessionalSubscriptions(user: User,
                                                      taxYear: Int,
                                                      employmentId: String,
                                                      originalEmploymentUserData: EmploymentUserData,
                                                      amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val utilitiesAndServices: Option[UtilitiesAndServicesModel] = cyaModel.employmentBenefits.flatMap(_.utilitiesAndServicesModel)
    val updatedEmployment = cyaModel.copy(employmentBenefits = cyaModel.employmentBenefits.map(_.copy(utilitiesAndServicesModel =
      utilitiesAndServices.map(_.copy(employerProvidedProfessionalSubscriptions = Some(amount))))))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateServiceQuestion(user: User,
                            taxYear: Int,
                            employmentId: String,
                            originalEmploymentUserData: EmploymentUserData,
                            questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val utilitiesAndServicesModel = cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(utilitiesAndServicesModel =
        utilitiesAndServicesModel.map(_.copy(serviceQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(utilitiesAndServicesModel =
        utilitiesAndServicesModel.map(_.copy(serviceQuestion = Some(false), service = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateService(user: User,
                    taxYear: Int,
                    employmentId: String,
                    originalEmploymentUserData: EmploymentUserData,
                    amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val utilitiesServices = cyaModel.employmentBenefits.flatMap(_.utilitiesAndServicesModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = benefits.map(_.copy(
      utilitiesAndServicesModel = utilitiesServices.map(_.copy(service = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }
}
