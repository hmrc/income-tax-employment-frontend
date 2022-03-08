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

package support.builders.models.employment

import DeductionsBuilder.aDeductions
import PayBuilder.aPay
import models.employment.{Deductions, EmploymentData, StudentLoans}
import support.builders.models.employment.StudentLoansBuilder.aStudentLoans

object EmploymentDataBuilder {

  val anEmploymentData: EmploymentData = EmploymentData(
    submittedOn = "2020-02-12",
    employmentSequenceNumber = Some("123456789999"),
    companyDirector = Some(true),
    closeCompany = Some(false),
    directorshipCeasedDate = Some("2020-02-12"),
    occPen = Some(false),
    disguisedRemuneration = Some(false),
    pay = Some(aPay),
    deductions = Some(aDeductions)
  )

  val aLatestCustomerSubmittedEmploymentData: EmploymentData = EmploymentData(
    submittedOn = "2021-02-12",
    employmentSequenceNumber = Some("123456789999"),
    companyDirector = Some(true),
    closeCompany = Some(false),
    directorshipCeasedDate = Some("2020-02-12"),
    occPen = Some(false),
    disguisedRemuneration = Some(false),
    pay = Some(aPay.copy(
      taxablePayToDate = aPay.taxablePayToDate.map(_ + 1000),
      totalTaxToDate = aPay.totalTaxToDate.map(_ + 1000)
    )),
    deductions = Some(Deductions(
      Some(StudentLoans(
        uglDeductionAmount = aStudentLoans.uglDeductionAmount.map(_ + 1000),
        pglDeductionAmount = aStudentLoans.pglDeductionAmount.map(_ + 1000)
      ))
    ))
  )
}
