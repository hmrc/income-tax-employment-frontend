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
import models.benefits.ReimbursedCostsVouchersAndNonCashModel
import models.mongo.EmploymentUserData
import services.EmploymentSessionService
import utils.Clock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReimbursedService @Inject()(employmentSessionService: EmploymentSessionService,
                                  implicit val ec: ExecutionContext) {

  def updateSectionQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                           (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val reimbursedCostsModel = cya.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = reimbursedCostsModel match {
      case Some(reimbursedCosts) if questionValue => cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        Some(reimbursedCosts.copy(sectionQuestion = Some(true))))))
      case Some(_) => cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        Some(ReimbursedCostsVouchersAndNonCashModel.clear))))
      case _ => cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(questionValue))))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateExpensesQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                            (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val reimbursedCostsVouchersAndNonCashModel = benefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        reimbursedCostsVouchersAndNonCashModel.map(_.copy(expensesQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        reimbursedCostsVouchersAndNonCashModel.map(_.copy(expensesQuestion = Some(false), expenses = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateExpenses(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                    (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val reimbursedCostsVouchersAndNonCashModel = benefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
      reimbursedCostsVouchersAndNonCashModel.map(_.copy(expenses = Some(amount)))))
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateTaxableExpensesQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                                   (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val reimbursedModel = cya.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        reimbursedModel.map(_.copy(taxableExpensesQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        reimbursedModel.map(_.copy(taxableExpensesQuestion = Some(false), taxableExpenses = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateTaxableExpenses(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                           (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val reimbursedCostsVouchersAndNonCashModel = cyaModel.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = benefits.map(_.copy(
      reimbursedCostsVouchersAndNonCashModel = reimbursedCostsVouchersAndNonCashModel.map(_.copy(taxableExpenses = Some(amount)))
    )))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateVouchersAndCreditCardsQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                                          (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val reimbursedCostsVouchersAndNonCashModel = benefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        reimbursedCostsVouchersAndNonCashModel.map(_.copy(vouchersAndCreditCardsQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        reimbursedCostsVouchersAndNonCashModel.map(_.copy(vouchersAndCreditCardsQuestion = Some(false), vouchersAndCreditCards = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateVouchersAndCreditCards(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                                  (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val reimbursedCostsVouchersAndNonCashModel = cyaModel.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = benefits.map(_.copy(
      reimbursedCostsVouchersAndNonCashModel = reimbursedCostsVouchersAndNonCashModel.map(_.copy(vouchersAndCreditCards = Some(amount)))
    )))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateNonCashQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                           (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val reimbursedCostsVouchersAndNonCashModel = benefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        reimbursedCostsVouchersAndNonCashModel.map(_.copy(nonCashQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        reimbursedCostsVouchersAndNonCashModel.map(_.copy(nonCashQuestion = Some(false), nonCash = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateNonCash(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                   (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val reimbursedCostsVouchersAndNonCashModel = benefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = benefits.map(_.copy(
      reimbursedCostsVouchersAndNonCashModel = reimbursedCostsVouchersAndNonCashModel.map(_.copy(nonCash = Some(amount)))
    )))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateOtherItemsQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                              (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val benefits = cya.employmentBenefits
    val reimbursedCostsVouchersAndNonCashModel = benefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = if (questionValue) {
      cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        reimbursedCostsVouchersAndNonCashModel.map(_.copy(otherItemsQuestion = Some(true))))))
    } else {
      cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
        reimbursedCostsVouchersAndNonCashModel.map(_.copy(otherItemsQuestion = Some(false), otherItems = None)))))
    }

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateOtherItems(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                      (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cyaModel = originalEmploymentUserData.employment
    val benefits = cyaModel.employmentBenefits
    val reimbursedCostsVouchersAndNonCashModel = cyaModel.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

    val updatedEmployment = cyaModel.copy(employmentBenefits = benefits.map(_.copy(
      reimbursedCostsVouchersAndNonCashModel = reimbursedCostsVouchersAndNonCashModel.map(_.copy(otherItems = Some(amount)))
    )))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }
}
