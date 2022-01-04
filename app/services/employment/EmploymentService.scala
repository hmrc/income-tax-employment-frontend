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

package services.employment

import models.User
import models.employment.EmploymentDate
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService
import utils.Clock

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentService @Inject()(employmentSessionService: EmploymentSessionService,
                                  implicit val ec: ExecutionContext) {

  def updateEmployerRef(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, payeRef: String)
                       (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment: EmploymentCYAModel = cya.copy(cya.employmentDetails.copy(employerRef = Some(payeRef)))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateStartDate(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, startedDate: EmploymentDate)
                     (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val leaveDate = cya.employmentDetails.cessationDate
    lazy val leaveDateLocalDate = LocalDate.parse(leaveDate.get)
    lazy val leaveDateIsEqualOrAfterStartDate = !leaveDateLocalDate.isBefore(startedDate.toLocalDate)

    val resetLeaveDateIfNowInvalid = if (leaveDate.isDefined && !leaveDateIsEqualOrAfterStartDate) {
      None
    } else {
      leaveDate
    }

    val updatedEmployment = cya.copy(cya.employmentDetails.copy(
      startDate = Some(startedDate.toLocalDate.toString),
      cessationDate = resetLeaveDateIfNowInvalid)
    )

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updatePayrollId(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, payrollId: String)
                     (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment = cya.copy(cya.employmentDetails.copy(payrollId = Some(payrollId)))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateCessationDateQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, questionValue: Boolean)
                                 (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val cessationDateUpdated = {
      if (questionValue) None else cya.employmentDetails.cessationDate
    }
    val updatedEmployment = cya.copy(cya.employmentDetails.copy(cessationDateQuestion = Some(questionValue), cessationDate = cessationDateUpdated))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateCessationDate(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, cessationDate: String)
                         (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment = cya.copy(cya.employmentDetails.copy(cessationDate = Some(cessationDate)))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateTaxablePayToDate(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                            (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment = cya.copy(employmentDetails = cya.employmentDetails.copy(taxablePayToDate = Some(amount)))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateTotalTaxToDate(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                          (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val updatedEmployment = originalEmploymentUserData.employment.copy(employmentDetails =
      originalEmploymentUserData.employment.employmentDetails.copy(totalTaxToDate = Some(amount)))

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }
}