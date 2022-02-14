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
import models.benefits.{AccommodationRelocationModel, BenefitsViewModel}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AccommodationService @Inject()(employmentSessionService: EmploymentSessionService,
                                     implicit val ec: ExecutionContext) {

  def updateSectionQuestion(user: User,
                            taxYear: Int,
                            employmentId: String,
                            originalEmploymentUserData: EmploymentUserData,
                            questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val accommodationRelocation = cya.employmentBenefits.flatMap(_.accommodationRelocationModel)

    val updatedEmployment: EmploymentCYAModel = {
      accommodationRelocation match {
        case Some(accommodationRelocationModel) if questionValue => cya.copy(employmentBenefits = benefits.map(_.copy(accommodationRelocationModel =
          Some(accommodationRelocationModel.copy(sectionQuestion = Some(true))))))
        case Some(_) => cya.copy(employmentBenefits = benefits.map(_.copy(accommodationRelocationModel =
          Some(AccommodationRelocationModel.clear))))
        case _ => cya.copy(employmentBenefits = benefits.map(_.copy(accommodationRelocationModel =
          Some(AccommodationRelocationModel(sectionQuestion = Some(questionValue))))))
      }
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateAccommodationQuestion(user: User,
                                  taxYear: Int,
                                  employmentId: String,
                                  originalEmploymentUserData: EmploymentUserData,
                                  questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val accommodationRelocation = benefits.flatMap(_.accommodationRelocationModel)

    val updatedEmployment: EmploymentCYAModel = {
      if (questionValue) {
        cya.copy(employmentBenefits = benefits.map(_.copy(accommodationRelocationModel =
          accommodationRelocation.map(_.copy(accommodationQuestion = Some(true))))))
      } else {
        cya.copy(employmentBenefits = benefits.map(_.copy(
          accommodationRelocationModel = accommodationRelocation.map(_.copy(accommodationQuestion = Some(false), accommodation = None)))))
      }
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateAccommodation(user: User,
                          taxYear: Int,
                          employmentId: String,
                          originalEmploymentUserData: EmploymentUserData,
                          amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val accommodationRelocation = benefits.flatMap(_.accommodationRelocationModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(accommodationRelocationModel = accommodationRelocation.map(_.copy(accommodation = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateQualifyingExpensesQuestion(user: User,
                                       taxYear: Int,
                                       employmentId: String,
                                       originalEmploymentUserData: EmploymentUserData,
                                       questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel: EmploymentCYAModel = originalEmploymentUserData.employment
    val benefits: Option[BenefitsViewModel] = cyaModel.employmentBenefits
    val accommodationRelocationModel: Option[AccommodationRelocationModel] = cyaModel.employmentBenefits.flatMap(_.accommodationRelocationModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(
        accommodationRelocationModel =
          if (questionValue) {
            accommodationRelocationModel.map(_.copy(
              qualifyingRelocationExpensesQuestion = Some(true)
            ))
          } else {
            accommodationRelocationModel.map(_.copy(
              qualifyingRelocationExpensesQuestion = Some(false), qualifyingRelocationExpenses = None
            ))
          }
      )))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateQualifyingExpenses(user: User,
                               taxYear: Int,
                               employmentId: String,
                               originalEmploymentUserData: EmploymentUserData,
                               amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val accommodationRelocation: Option[AccommodationRelocationModel] = cyaModel.employmentBenefits.flatMap(_.accommodationRelocationModel)
    val updatedEmployment = cyaModel.copy(employmentBenefits = cyaModel.employmentBenefits.map(_.copy(accommodationRelocationModel =
      accommodationRelocation.map(_.copy(qualifyingRelocationExpenses = Some(amount))))))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateNonQualifyingExpensesQuestion(user: User,
                                          taxYear: Int,
                                          employmentId: String,
                                          originalEmploymentUserData: EmploymentUserData,
                                          questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val accommodationRelocation = cya.employmentBenefits.flatMap(_.accommodationRelocationModel)

    val updatedEmployment: EmploymentCYAModel = {
      if (questionValue) {
        cya.copy(employmentBenefits = benefits.map(_.copy(
          accommodationRelocationModel = accommodationRelocation.map(_.copy(nonQualifyingRelocationExpensesQuestion = Some(true))))))
      } else {
        cya.copy(employmentBenefits =
          benefits.map(_.copy(
            accommodationRelocationModel = accommodationRelocation.map(_.copy(
              nonQualifyingRelocationExpensesQuestion = Some(false), nonQualifyingRelocationExpenses = None)))))
      }
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }

  def updateNonQualifyingExpenses(user: User,
                                  taxYear: Int,
                                  employmentId: String,
                                  originalEmploymentUserData: EmploymentUserData,
                                  amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val accommodationRelocation = cyaModel.employmentBenefits.flatMap(_.accommodationRelocationModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(
        accommodationRelocationModel = accommodationRelocation.map(_.copy(nonQualifyingRelocationExpenses = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(taxYear, employmentId, user, originalEmploymentUserData, updatedEmployment)
  }
}