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

package services.employment

import java.time.LocalDate

import javax.inject.Inject
import models.User
import models.employment.EmploymentDate
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService

import scala.concurrent.{ExecutionContext, Future}

class EmploymentService @Inject()(employmentSessionService: EmploymentSessionService,
                                  implicit val ec: ExecutionContext) {

  def updateEmployerRef(user: User,
                        taxYear: Int,
                        employmentId: String,
                        originalEmploymentUserData: EmploymentUserData,
                        payeRef: String): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment: EmploymentCYAModel = cya.copy(cya.employmentDetails.copy(employerRef = Some(payeRef)))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateStartDate(user: User,
                      taxYear: Int,
                      employmentId: String,
                      originalEmploymentUserData: EmploymentUserData,
                      startedDate: EmploymentDate): Future[Either[Unit, EmploymentUserData]] = {
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

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updatePayrollId(user: User,
                      taxYear: Int,
                      employmentId: String,
                      originalEmploymentUserData: EmploymentUserData,
                      payrollId: String): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment = cya.copy(cya.employmentDetails.copy(payrollId = Some(payrollId)))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateDidYouLeaveQuestion(user: User,
                                taxYear: Int,
                                employmentId: String,
                                originalEmploymentUserData: EmploymentUserData,
                                leftEmployer: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val cessationDateUpdated = {
      if (leftEmployer) cya.employmentDetails.cessationDate else None
    }
    val updatedEmployment = cya.copy(cya.employmentDetails.copy(didYouLeaveQuestion = Some(leftEmployer), cessationDate = cessationDateUpdated))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateCessationDate(user: User,
                          taxYear: Int,
                          employmentId: String,
                          originalEmploymentUserData: EmploymentUserData,
                          cessationDate: String): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment = cya.copy(cya.employmentDetails.copy(cessationDate = Some(cessationDate)))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateTaxablePayToDate(user: User,
                             taxYear: Int,
                             employmentId: String,
                             originalEmploymentUserData: EmploymentUserData,
                             amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment = cya.copy(employmentDetails = cya.employmentDetails.copy(taxablePayToDate = Some(amount)))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateTotalTaxToDate(user: User,
                           taxYear: Int,
                           employmentId: String,
                           originalEmploymentUserData: EmploymentUserData,
                           amount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val updatedEmployment = originalEmploymentUserData.employment.copy(employmentDetails =
      originalEmploymentUserData.employment.employmentDetails.copy(totalTaxToDate = Some(amount)))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }
}