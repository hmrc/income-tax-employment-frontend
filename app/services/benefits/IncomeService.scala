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
import models.benefits.IncomeTaxAndCostsModel
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService
import utils.Clock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeService @Inject()(employmentSessionService: EmploymentSessionService,
                              implicit val ec: ExecutionContext) {

  def updateSectionQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                           (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val incomeTaxOrCosts = cya.employmentBenefits.flatMap(_.incomeTaxAndCostsModel)

    val updatedEmployment: EmploymentCYAModel = {
      incomeTaxOrCosts match {
        case Some(incomeTaxOrCosts) if questionValue =>
          cya.copy(employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel =
            Some(incomeTaxOrCosts.copy(sectionQuestion = Some(true))))))
        case Some(_) =>
          cya.copy(employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel =
            Some(IncomeTaxAndCostsModel.clear))))
        case _ =>
          cya.copy(employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel =
            Some(IncomeTaxAndCostsModel(sectionQuestion = Some(questionValue))))))
      }
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateIncomeTaxPaidByDirectorQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                                           (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val incomeTaxModel = cya.employmentBenefits.flatMap(_.incomeTaxAndCostsModel)

    val updatedEmployment =
      if (questionValue) {
        cya.copy(employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel = incomeTaxModel.map(_.copy(incomeTaxPaidByDirectorQuestion = Some(true))))))
      } else {
        cya.copy(employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel = incomeTaxModel.map(_.copy(
          incomeTaxPaidByDirectorQuestion = Some(false), incomeTaxPaidByDirector = None)))))
      }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateIncomeTaxPaidByDirector(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                                   (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val incomeTaxModel = benefits.flatMap(_.incomeTaxAndCostsModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel = incomeTaxModel.map(_.copy(incomeTaxPaidByDirector = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updatePaymentsOnEmployeesBehalfQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                                             (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val incomeTaxModel = cya.employmentBenefits.flatMap(_.incomeTaxAndCostsModel)

    val updatedEmployment: EmploymentCYAModel = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel = incomeTaxModel.map(_.copy(paymentsOnEmployeesBehalfQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(
        incomeTaxAndCostsModel = incomeTaxModel.map(_.copy(paymentsOnEmployeesBehalfQuestion = Some(false), paymentsOnEmployeesBehalf = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updatePaymentsOnEmployeesBehalf(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                                     (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val incomeTaxModel = benefits.flatMap(_.incomeTaxAndCostsModel)

    val updatedEmployment = cyaModel.copy(
      employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel = incomeTaxModel.map(_.copy(paymentsOnEmployeesBehalf = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }
}