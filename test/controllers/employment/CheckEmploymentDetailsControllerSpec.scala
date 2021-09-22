/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.employment

import audit.{AmendEmploymentDetailsUpdateAudit, AuditEmploymentData, AuditNewEmploymentData, CreateNewEmploymentDetailsAudit, PriorEmploymentAuditInfo}
import common.SessionValues
import config.{MockAuditService, MockEmploymentSessionService}
import models.employment.{AllEmploymentData, Deductions, EmploymentData, EmploymentDetailsViewModel, EmploymentSource, Pay, StudentLoans}
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import play.api.http.Status._
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.UnitTestWithApp
import views.html.employment.CheckEmploymentDetailsView

import scala.concurrent.Future

class CheckEmploymentDetailsControllerSpec extends UnitTestWithApp with MockEmploymentSessionService with MockAuditService{

  lazy val view = app.injector.instanceOf[CheckEmploymentDetailsView]
  lazy val controller = new CheckEmploymentDetailsController()(
    mockMessagesControllerComponents,
    authorisedAction,
    view,
    inYearAction,
    mockAppConfig,
    mockIncomeTaxUserDataService,
    mockAuditService,
    ec,
    mockErrorHandler,
    testClock
  )
  val taxYear = mockAppConfig.defaultTaxYear
  val employmentId = "223/AB12399"


  ".show" should {

    "return a result when GetEmploymentDataModel is in Session" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = {
          mockFind(taxYear, Ok(view(
            EmploymentDetailsViewModel(
              employerName = "Dave",
              employerRef = Some("reference"),
              payrollId = Some("12345678"),
              employmentId = "id",
              startDate = Some("2020-02-12"),
              cessationDateQuestion = Some(true),
              cessationDate = Some("2020-02-12"),
              taxablePayToDate = Some(34234.15),
              totalTaxToDate = Some(6782.92),
              payrollId = None,
              isUsingCustomerData = false
            ), taxYear, isInYear = true
          )))
          controller.show(taxYear, employmentId = employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }

    "redirect the User to the Overview page no data in session" which {

      s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
        val result: Future[Result] = {
          mockFind(taxYear,Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller.show(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }
  }

  //scalastyle:off
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

      verifyAuditEvent(CreateNewEmploymentDetailsAudit(
        2021,user.affinityGroup.toLowerCase,user.nino,user.mtditid,AuditNewEmploymentData(
          Some("name"),Some("employerRef"),Some("2000-10-10"), None, Some(4354), Some(564), None
        ),Seq()
      ).toAuditModel)

      await(controller.performSubmitAudits(model, "001", 2021, prior)) shouldBe AuditResult.Success
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
      val employmentSource1 = EmploymentSource(
        employmentId = "002",
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
      mockGetLatestEmploymentDataEOY(prior,false)
      mockEmploymentSourceToUseNone(prior,"001",false)

      verifyAuditEvent(CreateNewEmploymentDetailsAudit(
        2021,user.affinityGroup.toLowerCase,user.nino,user.mtditid,AuditNewEmploymentData(
          Some("name"),Some("employerRef"),Some("2000-10-10"), None, Some(4354), Some(564), None
        ),Seq(PriorEmploymentAuditInfo("Mishima Zaibatsu",Some("223/AB12399")))
      ).toAuditModel)

      await(controller.performSubmitAudits(model, "001", 2021, Some(prior))) shouldBe AuditResult.Success
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

      mockEmploymentSourceToUseHMRC(prior,"001",false)

      verifyAuditEvent(AmendEmploymentDetailsUpdateAudit(
        2021,user.affinityGroup.toLowerCase,user.nino,user.mtditid,AuditEmploymentData(
          employerName = employmentSource1.employerName,
          employerRef = employmentSource1.employerRef,
          employmentId = employmentSource1.employmentId,
          startDate = employmentSource1.startDate,
          cessationDate = employmentSource1.cessationDate,
          taxablePayToDate = employmentSource1.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)),
          totalTaxToDate = employmentSource1.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)),
          payrollId = employmentSource1.payrollId
        ),AuditEmploymentData(
          "name",Some("employerRef"),"001",Some("2000-10-10"), Some("2020-03-11"), Some(4354), Some(564), Some("123456789999")
        )
      ).toAuditModel)

      await(controller.performSubmitAudits(model, "001", 2021, Some(prior))) shouldBe AuditResult.Success
    }
  }

}
