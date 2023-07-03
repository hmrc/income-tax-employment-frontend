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

package services.studentLoans

import models.User
import models.employment.StudentLoansCYAModel
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import services.EmploymentSessionService

import javax.inject.Inject
import scala.concurrent.Future

class StudentLoansService @Inject()(employmentSessionService: EmploymentSessionService) {

  def updateUglQuestion(user: User,
                        taxYear: Int,
                        employmentId: String,
                        originalEmploymentUserData: EmploymentUserData,
                        ugl: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val newStudentLoans: Option[StudentLoansCYAModel] = originalEmploymentUserData.employment.studentLoans.
      map(studentLoan => if (ugl) {
        studentLoan.copy(uglDeduction = ugl)
      } else {
        studentLoan.copy(uglDeduction = ugl, uglDeductionAmount = None)
      })
    val updatedEmployment: EmploymentCYAModel = cya.copy(studentLoans = newStudentLoans)

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updatePglQuestion(user: User,
                        taxYear: Int,
                        employmentId: String,
                        originalEmploymentUserData: EmploymentUserData,
                        pgl: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val newStudentLoans: Option[StudentLoansCYAModel] = originalEmploymentUserData.employment.studentLoans.
      map(studentLoan => if (pgl) {
        studentLoan.copy(pglDeduction = pgl)
      } else {
        studentLoan.copy(pglDeduction = pgl, pglDeductionAmount = None)
      })
    val updatedEmployment: EmploymentCYAModel = cya.copy(studentLoans = newStudentLoans)

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updateUglDeductionAmount(user: User,
                               taxYear: Int,
                               employmentId: String,
                               originalEmploymentUserData: EmploymentUserData,
                               uglAmount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val newStudentLoans: Option[StudentLoansCYAModel] = originalEmploymentUserData.employment.studentLoans.
      map(studentLoan => studentLoan.copy(uglDeductionAmount = Some(uglAmount)))
    val updatedEmployment: EmploymentCYAModel = cya.copy(studentLoans = newStudentLoans)

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }

  def updatePglDeductionAmount(user: User,
                               taxYear: Int,
                               employmentId: String,
                               originalEmploymentUserData: EmploymentUserData,
                               pglAmount: BigDecimal): Future[Either[Unit, EmploymentUserData]] = {
    val cya = originalEmploymentUserData.employment
    val newStudentLoans: Option[StudentLoansCYAModel] = originalEmploymentUserData.employment.studentLoans.
      map(studentLoan => studentLoan.copy(pglDeductionAmount = Some(pglAmount)))
    val updatedEmployment: EmploymentCYAModel = cya.copy(studentLoans = newStudentLoans)

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }
}
