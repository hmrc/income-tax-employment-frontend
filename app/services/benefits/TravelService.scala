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
import models.benefits.TravelEntertainmentModel
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService

import scala.concurrent.{ExecutionContext, Future}

class TravelService @Inject()(employmentSessionService: EmploymentSessionService,
                              implicit val ec: ExecutionContext) {

  def updateSectionQuestion(user: User,
                            taxYear: Int,
                            employmentId: String,
                            originalEmploymentUserData: EmploymentUserData,
                            questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val travelOrEntertainment = cya.employmentBenefits.flatMap(_.travelEntertainmentModel)

    val updatedEmployment: EmploymentCYAModel = {
      travelOrEntertainment match {
        case Some(_) if !questionValue => cya.copy(employmentBenefits = benefits.map(_.copy(travelEntertainmentModel = Some(TravelEntertainmentModel.clear))))
        case travelOrEntertainment => cya.copy(employmentBenefits = benefits.map(_.copy(travelEntertainmentModel = Some(
          travelOrEntertainment.map(_.copy(sectionQuestion = Some(questionValue))).getOrElse
          (TravelEntertainmentModel(sectionQuestion = Some(questionValue)))
        ))))
      }
    }

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateTravelAndSubsistenceQuestion(user: User,
                                         taxYear: Int,
                                         employmentId: String,
                                         originalEmploymentUserData: EmploymentUserData,
                                         questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val travelEntertainmentModel = cya.employmentBenefits.flatMap(_.travelEntertainmentModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(travelEntertainmentModel =
        travelEntertainmentModel.map(_.copy(travelAndSubsistenceQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(travelEntertainmentModel = travelEntertainmentModel.map(
        _.copy(travelAndSubsistenceQuestion = Some(false), travelAndSubsistence = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateTravelAndSubsistence(user: User,
                                 taxYear: Int,
                                 employmentId: String,
                                 originalEmploymentUserData: EmploymentUserData,
                                 amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val travelEntertainment: Option[TravelEntertainmentModel] = cyaModel.employmentBenefits.flatMap(_.travelEntertainmentModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = cyaModel.employmentBenefits.map(_.copy(travelEntertainmentModel =
      travelEntertainment.map(_.copy(travelAndSubsistence = Some(amount))))))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updatePersonalIncidentalExpensesQuestion(user: User,
                                               taxYear: Int,
                                               employmentId: String,
                                               originalEmploymentUserData: EmploymentUserData,
                                               questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val travelEntertainmentModel = cya.employmentBenefits.flatMap(_.travelEntertainmentModel)

    val updatedEmployment: EmploymentCYAModel = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(
        travelEntertainmentModel = travelEntertainmentModel.map(_.copy(personalIncidentalExpensesQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(travelEntertainmentModel = travelEntertainmentModel.map(
        _.copy(personalIncidentalExpensesQuestion = Some(false), personalIncidentalExpenses = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updatePersonalIncidentalExpenses(user: User,
                                       taxYear: Int,
                                       employmentId: String,
                                       originalEmploymentUserData: EmploymentUserData,
                                       amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val travelEntertainment = benefits.flatMap(_.travelEntertainmentModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(travelEntertainmentModel = travelEntertainment.map(_.copy(personalIncidentalExpenses = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateEntertainingQuestion(user: User,
                                 taxYear: Int,
                                 employmentId: String,
                                 originalEmploymentUserData: EmploymentUserData,
                                 questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val travelEntertainment = benefits.flatMap(_.travelEntertainmentModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(travelEntertainmentModel =
        travelEntertainment.map(_.copy(entertainingQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(
        travelEntertainmentModel = travelEntertainment.map(_.copy(entertainingQuestion = Some(false), entertaining = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateEntertaining(user: User,
                         taxYear: Int,
                         employmentId: String,
                         originalEmploymentUserData: EmploymentUserData,
                         amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val travelEntertainment = cyaModel.employmentBenefits.flatMap(_.travelEntertainmentModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(travelEntertainmentModel = travelEntertainment.map(_.copy(entertaining = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }
}
