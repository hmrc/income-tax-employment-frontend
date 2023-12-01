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

package services.employment

import models.User
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentService @Inject()(employmentSessionService: EmploymentSessionService,
                                  implicit val ec: ExecutionContext) {

  def updateEmployerRef(user: User,
                        taxYear: Int,
                        employmentId: String,
                        originalEmploymentUserData: EmploymentUserData,
                        payeRef: Option[String]): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment: EmploymentCYAModel = cya.copy(cya.employmentDetails.copy(employerRef = payeRef))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateStartDate(user: User,
                      taxYear: Int,
                      employmentId: String,
                      originalEmploymentUserData: EmploymentUserData,
                      startDate: LocalDate): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val leaveDate = cya.employmentDetails.cessationDate
    lazy val leaveDateLocalDate = LocalDate.parse(leaveDate.get)
    lazy val leaveDateIsEqualOrAfterStartDate = !leaveDateLocalDate.isBefore(startDate)

    val resetLeaveDateIfNowInvalid = if (leaveDate.isDefined && !leaveDateIsEqualOrAfterStartDate) {
      None
    } else {
      leaveDate
    }

    val updatedEmployment = cya.copy(cya.employmentDetails.copy(
      startDate = Some(startDate.toString),
      cessationDate = resetLeaveDateIfNowInvalid)
    )

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updatePayrollId(user: User,
                      taxYear: Int,
                      employmentId: String,
                      originalEmploymentUserData: EmploymentUserData,
                      payrollId: Option[String]): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment = cya.copy(cya.employmentDetails.copy(payrollId = payrollId))

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

  def updateEndDate(user: User,
                    taxYear: Int,
                    employmentId: String,
                    originalEmploymentUserData: EmploymentUserData,
                    endDate: LocalDate): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val updatedEmployment = cya.copy(cya.employmentDetails.copy(cessationDate = Some(endDate.toString)))

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

  def updateOffPayrollWorkingStatus(user: User,
                                taxYear: Int,
                                employmentId: String,
                                originalEmploymentUserData: EmploymentUserData,
                                offPayrollWorkingStatus: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment

    val updatedEmployment = cya.copy(cya.employmentDetails.copy(offPayrollWorkingStatus = Some(offPayrollWorkingStatus)))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

}
