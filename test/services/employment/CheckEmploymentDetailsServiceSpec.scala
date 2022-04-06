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

import audit._
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import support.mocks.{MockAuditService, MockEmploymentSessionService, MockNrsService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.UnitTest

class CheckEmploymentDetailsServiceSpec extends UnitTest
  with MockEmploymentSessionService
  with MockNrsService
  with MockAuditService {

  private val underTest = new CheckEmploymentDetailsService(mockNrsService, mockAuditService)

  "performSubmitAudits" should {
    "send the audit events from the model when it's a create" in {

      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        None,
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
      val prior = None

      mockAuditSendEvent(CreateNewEmploymentDetailsAudit(
        taxYearEOY, authorisationRequest.user.affinityGroup.toLowerCase, authorisationRequest.user.nino, authorisationRequest.user.mtditid, AuditNewEmploymentData(
          Some("name"), Some("employerRef"), Some("2000-10-10"), None, Some(4354), Some(564), None
        ), Seq()
      ).toAuditModel)

      await(underTest.performSubmitAudits(authorisationRequest.user, model, "001", taxYearEOY, prior)) shouldBe AuditResult.Success
    }
    "send the audit events from the model when it's a create and theres existing data" in {

      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        None,
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
      val employmentSource1 = HmrcEmploymentSource(
        employmentId = "002",
        employerName = "Mishima Zaibatsu",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some("2020-03-11"),
        dateIgnored = None,
        submittedOn = Some("2020-01-04T05:01:01Z"),
        hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
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
        )), None
      )

      val prior: AllEmploymentData = AllEmploymentData(
        hmrcEmploymentData = Seq(employmentSource1),
        hmrcExpenses = None,
        customerEmploymentData = Seq(),
        customerExpenses = None
      )

      mockAuditSendEvent(CreateNewEmploymentDetailsAudit(
        taxYearEOY, authorisationRequest.user.affinityGroup.toLowerCase, authorisationRequest.user.nino, authorisationRequest.user.mtditid, AuditNewEmploymentData(
          Some("name"), Some("employerRef"), Some("2000-10-10"), None, Some(4354), Some(564), None
        ), Seq(PriorEmploymentAuditInfo("Mishima Zaibatsu", Some("223/AB12399")))
      ).toAuditModel)

      await(underTest.performSubmitAudits(authorisationRequest.user, model, "001", taxYearEOY, Some(prior))) shouldBe AuditResult.Success
    }
    "send the audit events from the model when it's an amend" in {

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

      val employmentSource1 = HmrcEmploymentSource(
        employmentId = "001",
        employerName = "Mishima Zaibatsu",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some("2020-03-11"),
        dateIgnored = None,
        submittedOn = Some("2020-01-04T05:01:01Z"),
        hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
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
        )),
        None
      )

      val prior: AllEmploymentData = AllEmploymentData(
        hmrcEmploymentData = Seq(employmentSource1),
        hmrcExpenses = None,
        customerEmploymentData = Seq(),
        customerExpenses = None
      )

      mockAuditSendEvent(AmendEmploymentDetailsUpdateAudit(
        taxYearEOY, authorisationRequest.user.affinityGroup.toLowerCase, authorisationRequest.user.nino, authorisationRequest.user.mtditid, AuditEmploymentData(
          employerName = employmentSource1.employerName,
          employerRef = employmentSource1.employerRef,
          employmentId = employmentSource1.employmentId,
          startDate = employmentSource1.startDate,
          cessationDate = employmentSource1.cessationDate,
          taxablePayToDate = employmentSource1.toEmploymentSource.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)),
          totalTaxToDate = employmentSource1.toEmploymentSource.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)),
          payrollId = employmentSource1.payrollId
        ), AuditEmploymentData(
          "name", Some("employerRef"), "001", Some("2000-10-10"), Some("2020-03-11"), Some(4354), Some(564), Some("123456789999")
        )
      ).toAuditModel)

      await(underTest.performSubmitAudits(authorisationRequest.user, model, "001", taxYearEOY, Some(prior))) shouldBe AuditResult.Success
    }
  }

  "performSubmitNrsPayload" should {
    "send the event from the model when it's a create" in {

      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        None,
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
      val prior = None

      verifySubmitEvent(DecodedCreateNewEmploymentDetailsPayload(DecodedNewEmploymentData(
        Some("name"), Some("employerRef"), Some("2000-10-10"), None, Some(4354), Some(564), None
      ), Seq()
      ))

      await(underTest.performSubmitNrsPayload(authorisationRequest.user, model, "001", prior)) shouldBe Right()
    }
    "send the events from the model when it's a create and theres existing data" in {

      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        None,
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
      val employmentSource1 = HmrcEmploymentSource(
        employmentId = "002",
        employerName = "Mishima Zaibatsu",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some("2020-03-11"),
        dateIgnored = None,
        submittedOn = Some("2020-01-04T05:01:01Z"),
        hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
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
        )),
        None
      )

      val prior: AllEmploymentData = AllEmploymentData(
        hmrcEmploymentData = Seq(employmentSource1),
        hmrcExpenses = None,
        customerEmploymentData = Seq(),
        customerExpenses = None
      )

      verifySubmitEvent(DecodedCreateNewEmploymentDetailsPayload(DecodedNewEmploymentData(
        Some("name"), Some("employerRef"), Some("2000-10-10"), None, Some(4354), Some(564), None
      ), Seq(DecodedPriorEmploymentInfo("Mishima Zaibatsu", Some("223/AB12399")))
      ))

      await(underTest.performSubmitNrsPayload(authorisationRequest.user, model, "001", Some(prior))) shouldBe Right()
    }
    "send the events from the model when it's an amend" in {

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

      val employmentSource1 = HmrcEmploymentSource(
        employmentId = "001",
        employerName = "Mishima Zaibatsu",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some("2020-03-11"),
        dateIgnored = None,
        submittedOn = Some("2020-01-04T05:01:01Z"),
        hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
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
        )),
        None
      )

      val prior: AllEmploymentData = AllEmploymentData(
        hmrcEmploymentData = Seq(employmentSource1),
        hmrcExpenses = None,
        customerEmploymentData = Seq(),
        customerExpenses = None
      )

      verifySubmitEvent(DecodedAmendEmploymentDetailsPayload(DecodedEmploymentData(
        employerName = employmentSource1.employerName,
        employerRef = employmentSource1.employerRef,
        employmentId = employmentSource1.employmentId,
        startDate = employmentSource1.startDate,
        cessationDate = employmentSource1.cessationDate,
        taxablePayToDate = employmentSource1.toEmploymentSource.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)),
        totalTaxToDate = employmentSource1.toEmploymentSource.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)),
        payrollId = employmentSource1.payrollId
      ), DecodedEmploymentData(
        "name", Some("employerRef"), "001", Some("2000-10-10"), Some("2020-03-11"), Some(4354), Some(564), Some("123456789999")
      )
      ))

      await(underTest.performSubmitNrsPayload(authorisationRequest.user, model, "001", Some(prior))) shouldBe Right()
    }
  }
}
