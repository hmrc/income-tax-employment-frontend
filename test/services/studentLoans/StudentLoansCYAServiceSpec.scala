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

import audit.{AmendStudentLoansDeductionsUpdateAudit, CreateNewStudentLoansDeductionsAudit}
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.employment._
import services.EmploymentSessionService
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.mocks.{MockAuditService, MockEmploymentSessionService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.UnitTest

class StudentLoansCYAServiceSpec extends UnitTest with MockAuditService with MockEmploymentSessionService {

  lazy val session: EmploymentSessionService = mock[EmploymentSessionService]

  lazy val service = new StudentLoansCYAService(session, mockAppConfig, mockErrorHandler, mockAuditService, mockExecutionContext)

  lazy val employerId = "1234567890"

  lazy val hmrcSource: EmploymentSource = EmploymentSource(
    employerId, "HMRC", None, None, None, None, None, None, None, None
  )

  lazy val customerSource: EmploymentSource = EmploymentSource(
    employerId, "Customer", None, None, None, None, None, None, None, None
  )

  lazy val allEmploymentData: AllEmploymentData = AllEmploymentData(
    Seq(hmrcSource), None, Seq(customerSource), None
  )

  lazy val studentLoans: StudentLoans = StudentLoans(
    uglDeductionAmount = Some(100),
    pglDeductionAmount = Some(100)
  )

  ".extractEmploymentInformation" should {

    "return an employment source" when {

      "the data contains customer data and is flagged as customer held" in {
        val result = service.extractEmploymentInformation(allEmploymentData, employerId, isCustomerHeld = true)

        result shouldBe Some(customerSource)
      }

      "the data contains hmrc data and is flagged as hmrc held" in {
        val result = service.extractEmploymentInformation(allEmploymentData, employerId, isCustomerHeld = false)

        result shouldBe Some(hmrcSource)
      }

    }

    "return a none" when {

      "the data does not contain customer data and is flagged as customer held" in {
        val result = service.extractEmploymentInformation(allEmploymentData.copy(customerEmploymentData = Seq()), employerId, isCustomerHeld = true)

        result shouldBe None
      }

      "the data does not contain hmrc data and is flagged as hmrc held" in {
        val result = service.extractEmploymentInformation(allEmploymentData.copy(hmrcEmploymentData = Seq()), employerId, isCustomerHeld = false)

        result shouldBe None
      }

    }

  }

  ".performSubmitAudits" should {
    "send the event from the model when it's a create" in {

      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        None,
        Some(
          CreateUpdateEmployment(
            anEmploymentSource.employerRef,
            anEmploymentSource.employerName,
            anEmploymentSource.startDate.get
          )
        ),
        Some(
          CreateUpdateEmploymentData(
            pay = CreateUpdatePay(
              4354,
              564
            ),
            deductions = Some(
              Deductions(
                Some(StudentLoans(
                  Some(100),
                  Some(100)
                ))
              )
            ),
            None
          )
        ),
        Some("001")
      )


      val createNewStudentLoansDeductionsAudit = CreateNewStudentLoansDeductionsAudit(
        taxYear = 2021,
        userType = authorisationRequest.user.affinityGroup.toLowerCase,
        nino = authorisationRequest.user.nino,
        mtditid = authorisationRequest.user.mtditid,
        deductions = Deductions(
          studentLoans = Some(studentLoans))
      )

      mockAuditSendEvent(createNewStudentLoansDeductionsAudit.toAuditModel)

      val customerEmploymentData = anEmploymentSource.copy(employmentData = None)
      val employmentDataWithoutDeductions = allEmploymentData.copy(customerEmploymentData = Seq(customerEmploymentData))

      val result = service.performSubmitAudits(authorisationRequest.user, model, employmentId = "001", taxYear = 2021, Some(employmentDataWithoutDeductions))

      await(result) shouldBe AuditResult.Success

    }

    "send the audit events from the model when it's an amend" in {

      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        None,
        Some(
          CreateUpdateEmployment(
            anEmploymentSource.employerRef,
            anEmploymentSource.employerName,
            anEmploymentSource.startDate.get
          )
        ),
        Some(
          CreateUpdateEmploymentData(
            pay = CreateUpdatePay(
              4354,
              564
            ),
            deductions = Some(
              Deductions(
                Some(StudentLoans(
                  Some(200),
                  Some(200)
                ))
              )
            ),
            None
          )
        ),
        Some("001")
      )

      val employmentSource1 = EmploymentSource(
        employmentId = "001",
        employerName = "Mishima Zaibatsu",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some("2020-03-11"),
        dateIgnored = None,
        submittedOn = Some("2020-01-04T05:01:01Z"),
        employmentData = Some(EmploymentData(
          submittedOn = "2020-02-12",
          employmentSequenceNumber = Some("123456789999"),
          companyDirector = Some(true),
          closeCompany = Some(false),
          directorshipCeasedDate = Some("2020-02-12"),
          occPen = Some(false),
          disguisedRemuneration = Some(false),
          pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
          Some(Deductions(
            studentLoans = Some(StudentLoans(
              uglDeductionAmount = Some(100.00),
              pglDeductionAmount = Some(100.00)
            ))
          ))
        )),
        None
      )

      val prior: AllEmploymentData = AllEmploymentData(
        hmrcEmploymentData = Seq(employmentSource1),
        hmrcExpenses = None,
        customerEmploymentData = Seq(),
        customerExpenses = None
      )

      mockAuditSendEvent(AmendStudentLoansDeductionsUpdateAudit(
        taxYear = 2021,
        userType = authorisationRequest.user.affinityGroup.toLowerCase,
        nino = authorisationRequest.user.nino,
        mtditid = authorisationRequest.user.mtditid,
        priorStudentLoanDeductionsData = employmentSource1.employmentData.flatMap(_.deductions).get,
        studentLoanDeductionsData = Deductions(
          studentLoans = Some(StudentLoans(
            uglDeductionAmount = Some(200.00),
            pglDeductionAmount = Some(200.00)
          ))
        )
      ).toAuditModel
      )

      val result = service.performSubmitAudits(authorisationRequest.user, model, employmentId = "001", taxYear = 2021, Some(prior))

      await(result) shouldBe AuditResult.Success
    }
  }

}
