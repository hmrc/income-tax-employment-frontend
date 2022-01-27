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

package services.studentLoans

import models.User
import models.employment.StudentLoansCYAModel
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService
import utils.Clock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StudentLoansService @Inject()(employmentSessionService: EmploymentSessionService,
                                    implicit val ec: ExecutionContext) {

  def updateUglQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, ugl: Boolean)
                           (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val newStudentLoans: Option[StudentLoansCYAModel] = originalEmploymentUserData.employment.studentLoans.
      map(studentLoan => if (ugl) {
        studentLoan.copy(uglDeduction = ugl)
      } else {
        studentLoan.copy(uglDeduction = ugl, uglDeductionAmount = None)
      })
    val updatedEmployment: EmploymentCYAModel = cya.copy(studentLoans = newStudentLoans)

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }
  def updatePglQuestion(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, pgl: Boolean)
                           (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val newStudentLoans: Option[StudentLoansCYAModel] = originalEmploymentUserData.employment.studentLoans.
      map(studentLoan => if (pgl) {
        studentLoan.copy(pglDeduction = pgl)
      } else {
        studentLoan.copy(pglDeduction = pgl, pglDeductionAmount = None)
      })
    val updatedEmployment: EmploymentCYAModel = cya.copy(studentLoans = newStudentLoans)

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updateUglDeductionAmount(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, uglAmount: BigDecimal)
                       (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val newStudentLoans: Option[StudentLoansCYAModel] = originalEmploymentUserData.employment.studentLoans.
      map(studentLoan => studentLoan.copy(uglDeductionAmount = Some(uglAmount)))
    val updatedEmployment: EmploymentCYAModel = cya.copy(studentLoans = newStudentLoans)

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }

  def updatePglDeductionAmount(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, pglAmount: BigDecimal)
                              (implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val newStudentLoans: Option[StudentLoansCYAModel] = originalEmploymentUserData.employment.studentLoans.
      map(studentLoan => studentLoan.copy(pglDeductionAmount = Some(pglAmount)))
    val updatedEmployment: EmploymentCYAModel = cya.copy(studentLoans = newStudentLoans)

    employmentSessionService.createOrUpdateEmploymentUserDataWith(
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      updatedEmployment
    )
  }
}
