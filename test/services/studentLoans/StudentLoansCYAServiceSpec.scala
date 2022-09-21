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

import audit.{AmendStudentLoansDeductionsUpdateAudit, CreateNewStudentLoansDeductionsAudit, ViewStudentLoansDeductionsAudit}
import config.AppConfig
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.studentLoans.{DecodedAmendStudentLoansPayload, DecodedCreateNewStudentLoansPayload}
import services.EmploymentSessionService
import support.ServiceUnitTest
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.mocks._
import uk.gov.hmrc.play.audit.http.connector.AuditResult

class StudentLoansCYAServiceSpec extends ServiceUnitTest with MockAuditService with MockNrsService with MockEmploymentSessionService
  with MockErrorHandler {

  lazy val session: EmploymentSessionService = mock[EmploymentSessionService]
  val mockAppConfig: AppConfig = new MockAppConfig().config()
  lazy val service = new StudentLoansCYAService(session, mockAppConfig, mockErrorHandler, mockAuditService, mockNrsService, ec)

  lazy val employerId = "1234567890"

  lazy val hmrcSource: HmrcEmploymentSource = HmrcEmploymentSource(
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

  val validModel: StudentLoansCYAModel = StudentLoansCYAModel(
    uglDeduction = true,
    uglDeductionAmount = Some(500.00),
    pglDeduction = true,
    pglDeductionAmount = Some(500.00)
  )

  ".extractEmploymentInformation" should {

    "return an employment source" when {

      "the data contains customer data and is flagged as customer held" in {
        val result = service.extractEmploymentInformation(allEmploymentData, employerId, isCustomerHeld = true)

        result shouldBe Some(customerSource)
      }

      "the data contains hmrc data and is flagged as hmrc held" in {
        val result = service.extractEmploymentInformation(allEmploymentData, employerId, isCustomerHeld = false)

        result shouldBe Some(hmrcSource.toEmploymentSource)
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
        taxYear = taxYearEOY,
        userType = authorisationRequest.user.affinityGroup.toLowerCase,
        nino = authorisationRequest.user.nino,
        mtditid = authorisationRequest.user.mtditid,
        deductions = Deductions(
          studentLoans = Some(studentLoans))
      )

      mockAuditSendEvent(createNewStudentLoansDeductionsAudit.toAuditModel)

      val customerEmploymentData = anEmploymentSource.copy(employmentData = None)
      val employmentDataWithoutDeductions = allEmploymentData.copy(customerEmploymentData = Seq(customerEmploymentData))

      val result = service.performSubmitAudits(authorisationRequest.user, model, employmentId = "001", taxYear = taxYearEOY, Some(employmentDataWithoutDeductions))

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

      val employmentSource1 = HmrcEmploymentSource(
        employmentId = "001",
        employerName = "Mishima Zaibatsu",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some(s"${taxYearEOY-1}-03-11"),
        dateIgnored = None,
        submittedOn = Some(s"${taxYearEOY-1}-01-04T05:01:01Z"),
        hmrcEmploymentFinancialData = Some(
          EmploymentFinancialData(
            employmentData = Some(EmploymentData(
              submittedOn = s"${taxYearEOY-1}-02-12",
              employmentSequenceNumber = Some("123456789999"),
              companyDirector = Some(true),
              closeCompany = Some(false),
              directorshipCeasedDate = Some(s"${taxYearEOY-1}-02-12"),
              disguisedRemuneration = Some(false),
              pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some(s"${taxYearEOY-1}-04-23"), Some(32), Some(2))),
              Some(Deductions(
                studentLoans = Some(StudentLoans(
                  uglDeductionAmount = Some(100.00),
                  pglDeductionAmount = Some(100.00)
                ))
              ))
            )),
            None
          )
        ),
        None
      )

      val prior: AllEmploymentData = AllEmploymentData(
        hmrcEmploymentData = Seq(employmentSource1),
        hmrcExpenses = None,
        customerEmploymentData = Seq(),
        customerExpenses = None
      )

      mockAuditSendEvent(AmendStudentLoansDeductionsUpdateAudit(
        taxYear = taxYearEOY,
        userType = authorisationRequest.user.affinityGroup.toLowerCase,
        nino = authorisationRequest.user.nino,
        mtditid = authorisationRequest.user.mtditid,
        priorStudentLoanDeductionsData = employmentSource1.hmrcEmploymentFinancialData.get.employmentData.flatMap(_.deductions).get,
        studentLoanDeductionsData = Deductions(
          studentLoans = Some(StudentLoans(
            uglDeductionAmount = Some(200.00),
            pglDeductionAmount = Some(200.00)
          ))
        )
      ).toAuditModel
      )

      val result = service.performSubmitAudits(authorisationRequest.user, model, employmentId = "001", taxYear = taxYearEOY, Some(prior))

      await(result) shouldBe AuditResult.Success
    }
  }

  ".sendViewStudentLoansDeductionsAudit" should {
    "send the audit event" in {
      mockAuditSendEvent(ViewStudentLoansDeductionsAudit(
        taxYearEOY, authorisationRequest.user.affinityGroup.toLowerCase, authorisationRequest.user.nino, authorisationRequest.user.mtditid, validModel.asDeductions
      ).toAuditModel)

      await(service.sendViewStudentLoansDeductionsAudit(authorisationRequest.user, taxYearEOY, validModel.asDeductions)) shouldBe AuditResult.Success
    }
  }

  "performSubmitNrsPayload" should {
    "send the event from the model when it's a create" in {
      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        Some("id"),
        Some(
          CreateUpdateEmployment(
            Some("employerRef"),
            "name",
            "2000-10-10"
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
            )
          )
        ),
        Some("001")
      )

      verifySubmitEvent(DecodedCreateNewStudentLoansPayload(Some("name"), Some("employerRef"),
        Deductions(Some(StudentLoans(
          Some(100),
          Some(100)
        )))))

      await(service.performSubmitNrsPayload(authorisationRequest.user, model, "001", prior = None)) shouldBe Right()

    }

    "send the event from the model when it's an amend" in {

      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        Some("id"),
        Some(
          CreateUpdateEmployment(
            Some("employerRef"),
            "name",
            "2000-10-10"
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
                  Some(750),
                  Some(1000)
                ))
              )
            )
          )
        ),
        Some("001")
      )

      val employmentSource1 = HmrcEmploymentSource(
        employmentId = "001",
        employerName = "Mishima Zaibatsu",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some(s"${taxYearEOY-1}-03-11"),
        dateIgnored = None,
        submittedOn = Some(s"${taxYearEOY-1}-01-04T05:01:01Z"),
        hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
          employmentData = Some(EmploymentData(
            submittedOn = s"${taxYearEOY-1}-02-12",
            employmentSequenceNumber = Some("123456789999"),
            companyDirector = Some(true),
            closeCompany = Some(false),
            directorshipCeasedDate = Some(s"${taxYearEOY-1}-02-12"),
            disguisedRemuneration = Some(false),
            pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some(s"${taxYearEOY-1}-04-23"), Some(32), Some(2))),
            Some(Deductions(
              studentLoans = Some(StudentLoans(
                uglDeductionAmount = Some(100.00),
                pglDeductionAmount = Some(100.00)
              ))
            ))
          )),
          None
        )),
        None
      )

      val priorData: AllEmploymentData = AllEmploymentData(
        hmrcEmploymentData = Seq(employmentSource1),
        hmrcExpenses = None,
        customerEmploymentData = Seq(),
        customerExpenses = None
      )

      verifySubmitEvent(DecodedAmendStudentLoansPayload(
        priorEmploymentStudentLoansData = Deductions(Some(StudentLoans(
          Some(100),
          Some(100)
        ))
        ),
        employmentStudentLoansData = Deductions(Some(StudentLoans(
          Some(750),
          Some(1000)
        ))
        )
      ))

      await(service.performSubmitNrsPayload(authorisationRequest.user, model, "001", Some(priorData))) shouldBe Right()
    }
  }
}