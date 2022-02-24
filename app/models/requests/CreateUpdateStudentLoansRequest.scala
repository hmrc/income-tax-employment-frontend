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

package models.requests

import audit.{AmendStudentLoansDeductionsUpdateAudit, CreateNewStudentLoansDeductionsAudit}
import models.User
import models.employment.{Deductions, EmploymentSource, StudentLoans}

case class CreateUpdateStudentLoansRequest(deductions: Deductions) {

  def toCreateAuditModel(user: User, taxYear: Int): CreateNewStudentLoansDeductionsAudit = {
    CreateNewStudentLoansDeductionsAudit(
      taxYear = taxYear,
      userType = user.affinityGroup.toLowerCase,
      nino = user.nino,
      mtditid = user.mtditid,
      deductions = Deductions(
        studentLoans = Some(StudentLoans(
          uglDeductionAmount = deductions.studentLoans.flatMap(_.uglDeductionAmount),
          pglDeductionAmount = deductions.studentLoans.flatMap(_.pglDeductionAmount)
        )
        )))
  }

  def toAmendAuditModel(user: User, taxYear: Int, priorData: EmploymentSource): AmendStudentLoansDeductionsUpdateAudit = {


    AmendStudentLoansDeductionsUpdateAudit(
      taxYear = taxYear,
      userType = user.affinityGroup.toLowerCase,
      nino = user.nino,
      mtditid = user.mtditid,
      priorStudentLoanDeductionsData = Deductions(
        studentLoans = Some(StudentLoans(
          uglDeductionAmount = priorData.employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.uglDeductionAmount))),
          pglDeductionAmount = priorData.employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.pglDeductionAmount)))
        ))),
      studentLoanDeductionsData = Deductions(
        studentLoans = Some(StudentLoans(
          uglDeductionAmount = deductions.studentLoans.flatMap(_.uglDeductionAmount),
          pglDeductionAmount = deductions.studentLoans.flatMap(_.pglDeductionAmount)
        ))
      ))
  }
}
