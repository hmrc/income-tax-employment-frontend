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

import audit.{AmendStudentLoansDeductionsUpdateAudit, CreateNewStudentLoansDeductionsAudit, ViewStudentLoansDeductionsAudit}
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.studentLoans.{DecodedAmendStudentLoansPayload, DecodedCreateNewStudentLoansPayload}
import services.EmploymentSessionService
import support.builders.models.UserBuilder.aUser
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.mocks._
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.ExecutionContext

class StudentLoansCYAServiceSpec extends UnitTest
  with TaxYearProvider
  with MockAuditService
  with MockNrsService
  with MockEmploymentSessionService
  with MockErrorHandler {

  private implicit val ec: ExecutionContext = ExecutionContext.global
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val employerId = "1234567890"
  private lazy val session: EmploymentSessionService = mock[EmploymentSessionService]


  private lazy val hmrcSource: HmrcEmploymentSource = HmrcEmploymentSource(
    employerId, "HMRC", None, None, None, None, None, None, None, None
  )

  private lazy val customerSource: EmploymentSource = EmploymentSource(
    employerId, "Customer", None, None, None, None, None, None, None, None
  )

  private lazy val allEmploymentData: AllEmploymentData = AllEmploymentData(
    Seq(hmrcSource), None, Seq(customerSource), None
  )

  private lazy val studentLoans: StudentLoans = StudentLoans(
    uglDeductionAmount = Some(100),
    pglDeductionAmount = Some(100)
  )

  private val validModel: StudentLoansCYAModel = StudentLoansCYAModel(
    uglDeduction = true,
    uglDeductionAmount = Some(500.00),
    pglDeduction = true,
    pglDeductionAmount = Some(500.00)
  )

  private val underTest = new StudentLoansCYAService(session, new MockAppConfig().config(), mockErrorHandler, mockAuditService, mockNrsService, ec)

  ".extractEmploymentInformation" should {
    "return an employment source" when {
      "the data contains customer data and is flagged as customer held" in {
        val result = underTest.extractEmploymentInformation(allEmploymentData, employerId, isCustomerHeld = true)

        result shouldBe Some(customerSource)
      }

      "the data contains hmrc data and is flagged as hmrc held" in {
        val result = underTest.extractEmploymentInformation(allEmploymentData, employerId, isCustomerHeld = false)

        result shouldBe Some(hmrcSource.toEmploymentSource)
      }

    }

    "return a none" when {
      "the data does not contain customer data and is flagged as customer held" in {
        val result = underTest.extractEmploymentInformation(allEmploymentData.copy(customerEmploymentData = Seq()), employerId, isCustomerHeld = true)

        result shouldBe None
      }

      "the data does not contain hmrc data and is flagged as hmrc held" in {
        val result = underTest.extractEmploymentInformation(allEmploymentData.copy(hmrcEmploymentData = Seq()), employerId, isCustomerHeld = false)

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
        userType = aUser.affinityGroup.toLowerCase,
        nino = aUser.nino,
        mtditid = aUser.mtditid,
        deductions = Deductions(
          studentLoans = Some(studentLoans))
      )

      mockAuditSendEvent(createNewStudentLoansDeductionsAudit.toAuditModel)

      val customerEmploymentData = anEmploymentSource.copy(employmentData = None)
      val employmentDataWithoutDeductions = allEmploymentData.copy(customerEmploymentData = Seq(customerEmploymentData))
      val result = underTest.performSubmitAudits(aUser, model, employmentId = "001", taxYear = taxYearEOY, Some(employmentDataWithoutDeductions))

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
        cessationDate = Some(s"${taxYearEOY - 1}-03-11"),
        dateIgnored = None,
        submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"),
        hmrcEmploymentFinancialData = Some(
          EmploymentFinancialData(
            employmentData = Some(EmploymentData(
              submittedOn = s"${taxYearEOY - 1}-02-12",
              employmentSequenceNumber = Some("123456789999"),
              companyDirector = Some(true),
              closeCompany = Some(false),
              directorshipCeasedDate = Some(s"${taxYearEOY - 1}-02-12"),
              disguisedRemuneration = Some(false),
              pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some(s"${taxYearEOY - 1}-04-23"), Some(32), Some(2))),
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
        userType = aUser.affinityGroup.toLowerCase,
        nino = aUser.nino,
        mtditid = aUser.mtditid,
        priorStudentLoanDeductionsData = employmentSource1.hmrcEmploymentFinancialData.get.employmentData.flatMap(_.deductions).get,
        studentLoanDeductionsData = Deductions(
          studentLoans = Some(StudentLoans(
            uglDeductionAmount = Some(200.00),
            pglDeductionAmount = Some(200.00)
          ))
        )
      ).toAuditModel
      )

      val result = underTest.performSubmitAudits(aUser, model, employmentId = "001", taxYear = taxYearEOY, Some(prior))

      await(result) shouldBe AuditResult.Success
    }
  }

  ".sendViewStudentLoansDeductionsAudit" should {
    "send the audit event" in {
      mockAuditSendEvent(ViewStudentLoansDeductionsAudit(
        taxYearEOY, aUser.affinityGroup.toLowerCase, aUser.nino, aUser.mtditid, validModel.asDeductions
      ).toAuditModel)

      await(underTest.sendViewStudentLoansDeductionsAudit(aUser, taxYearEOY, validModel.asDeductions)) shouldBe AuditResult.Success
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

      await(underTest.performSubmitNrsPayload(aUser, model, "001", prior = None)) shouldBe Right()

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
        cessationDate = Some(s"${taxYearEOY - 1}-03-11"),
        dateIgnored = None,
        submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"),
        hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
          employmentData = Some(EmploymentData(
            submittedOn = s"${taxYearEOY - 1}-02-12",
            employmentSequenceNumber = Some("123456789999"),
            companyDirector = Some(true),
            closeCompany = Some(false),
            directorshipCeasedDate = Some(s"${taxYearEOY - 1}-02-12"),
            disguisedRemuneration = Some(false),
            pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some(s"${taxYearEOY - 1}-04-23"), Some(32), Some(2))),
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

      await(underTest.performSubmitNrsPayload(aUser, model, "001", Some(priorData))) shouldBe Right()
    }
  }
}
