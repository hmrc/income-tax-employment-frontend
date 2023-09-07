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

package services.expenses

import audit.{AmendEmploymentExpensesUpdateAudit, AuditEmploymentExpensesData, AuditNewEmploymentExpensesData, CreateNewEmploymentExpensesAudit}
import models.employment._
import models.expenses.{DecodedAmendExpensesPayload, DecodedCreateNewExpensesPayload, Expenses}
import models.requests.CreateUpdateExpensesRequest
import support.builders.models.UserBuilder.aUser
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import support.builders.models.expenses.ExpensesBuilder.anExpenses
import support.mocks.{MockAuditService, MockEmploymentSessionService, MockNrsService}
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.ExecutionContext

class CheckEmploymentExpensesServiceSpec extends UnitTest
  with TaxYearProvider
  with MockAuditService
  with MockNrsService
  with MockEmploymentSessionService {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val underTest = new CheckEmploymentExpensesService(mockAuditService, mockNrsService)

  "calling performSubmitAudit" should {
    "send the audit events from the model when it's a create" in {
      val model: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(Some(true), anExpenses)
      val prior = None

      mockAuditSendEvent(CreateNewEmploymentExpensesAudit(
        taxYearEOY, aUser.affinityGroup.toLowerCase, aUser.nino, aUser.mtditid,
        AuditNewEmploymentExpensesData(anExpenses.jobExpenses, anExpenses.flatRateJobExpenses, anExpenses.professionalSubscriptions, anExpenses.otherAndCapitalAllowances)
      ).toAuditModel)
      await(underTest.performSubmitAudits(aUser, model, taxYearEOY, prior)) shouldBe AuditResult.Success
    }

    "send the audit events from the model when it's a create and theres existing data" in {
      val model: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(Some(true), anExpenses)

      val prior: AllEmploymentData = anAllEmploymentData.copy(
        hmrcExpenses = None,
        customerExpenses = None
      )

      mockAuditSendEvent(CreateNewEmploymentExpensesAudit(
        taxYearEOY, aUser.affinityGroup.toLowerCase, aUser.nino, aUser.mtditid, AuditNewEmploymentExpensesData(
          anExpenses.jobExpenses,
          anExpenses.flatRateJobExpenses,
          anExpenses.professionalSubscriptions,
          anExpenses.otherAndCapitalAllowances
        )
      ).toAuditModel)

      await(underTest.performSubmitAudits(aUser, model, taxYearEOY, Some(prior))) shouldBe AuditResult.Success
    }
  }

  "send the audit events from the model when it's a amend and there is existing data" in {
    val model: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(Some(true), anExpenses)
    val priorCustomerEmploymentExpenses = anEmploymentExpenses.copy(
      expenses = Some(anExpenses.copy(
        jobExpenses = Some(0.0),
        flatRateJobExpenses = Some(0.0),
        professionalSubscriptions = Some(0.0),
        otherAndCapitalAllowances = Some(0.0)
      )))

    val prior: AllEmploymentData = anAllEmploymentData.copy(
      hmrcExpenses = None,
      customerExpenses = Some(
        priorCustomerEmploymentExpenses
      )
    )

    mockAuditSendEvent(AmendEmploymentExpensesUpdateAudit(
      taxYearEOY, aUser.affinityGroup.toLowerCase, aUser.nino, aUser.mtditid,
      priorEmploymentExpensesData = AuditEmploymentExpensesData(
        priorCustomerEmploymentExpenses.expenses.get.jobExpenses,
        priorCustomerEmploymentExpenses.expenses.get.flatRateJobExpenses,
        priorCustomerEmploymentExpenses.expenses.get.professionalSubscriptions,
        priorCustomerEmploymentExpenses.expenses.get.otherAndCapitalAllowances
      ),
      employmentExpensesData = AuditEmploymentExpensesData(
        anExpenses.jobExpenses,
        anExpenses.flatRateJobExpenses,
        anExpenses.professionalSubscriptions,
        anExpenses.otherAndCapitalAllowances
      )
    ).toAuditModel)
    await(underTest.performSubmitAudits(aUser, model, taxYearEOY, Some(prior))) shouldBe AuditResult.Success
  }

  ".performSubmitNrsPayload" should {

    "send the event from the model when it's a create" in {

      val model: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(
        Some(true),
        Expenses(
          businessTravelCosts = Some(123),
          jobExpenses = Some(123),
          flatRateJobExpenses = Some(123),
          professionalSubscriptions = Some(123),
          hotelAndMealExpenses = Some(123),
          otherAndCapitalAllowances = Some(123),
          vehicleExpenses = Some(123),
          mileageAllowanceRelief = Some(123)
        )
      )


      verifySubmitEvent(DecodedCreateNewExpensesPayload(Expenses(
        businessTravelCosts = model.expenses.businessTravelCosts,
        jobExpenses = model.expenses.jobExpenses,
        flatRateJobExpenses = model.expenses.flatRateJobExpenses,
        professionalSubscriptions = model.expenses.professionalSubscriptions,
        hotelAndMealExpenses = model.expenses.hotelAndMealExpenses,
        otherAndCapitalAllowances = model.expenses.otherAndCapitalAllowances,
        vehicleExpenses = model.expenses.vehicleExpenses,
        mileageAllowanceRelief = model.expenses.mileageAllowanceRelief
      )))

      await(underTest.performSubmitNrsPayload(model, prior = None, aUser)) shouldBe Right(())
    }

    "send the event from the model when it's an amend" in {

      val model: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(
        None,
        Expenses(
          businessTravelCosts = Some(10),
          jobExpenses = Some(20),
          flatRateJobExpenses = Some(30),
          professionalSubscriptions = Some(40),
          hotelAndMealExpenses = Some(50),
          otherAndCapitalAllowances = Some(60),
          vehicleExpenses = Some(70),
          mileageAllowanceRelief = Some(80))
      )

      val priorCustomerEmploymentExpenses = anEmploymentExpenses.copy(
        expenses = Some(anExpenses.copy(
          businessTravelCosts = Some(123),
          jobExpenses = Some(123),
          flatRateJobExpenses = Some(123),
          professionalSubscriptions = Some(123),
          hotelAndMealExpenses = Some(123),
          otherAndCapitalAllowances = Some(123),
          vehicleExpenses = Some(123),
          mileageAllowanceRelief = Some(123))
        ))

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
            offPayrollWorker = Some(false),
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

      val priorExpenses: AllEmploymentData = AllEmploymentData(
        hmrcEmploymentData = Seq(employmentSource1),
        hmrcExpenses = Some(priorCustomerEmploymentExpenses),
        customerEmploymentData = Seq(),
        customerExpenses = None,
        None
      )

      verifySubmitEvent(
        DecodedAmendExpensesPayload(
          priorEmploymentExpensesData = Expenses(
            businessTravelCosts = priorCustomerEmploymentExpenses.expenses.flatMap(_.businessTravelCosts),
            jobExpenses = priorCustomerEmploymentExpenses.expenses.flatMap(_.jobExpenses),
            flatRateJobExpenses = priorCustomerEmploymentExpenses.expenses.flatMap(_.flatRateJobExpenses),
            professionalSubscriptions = priorCustomerEmploymentExpenses.expenses.flatMap(_.professionalSubscriptions),
            hotelAndMealExpenses = priorCustomerEmploymentExpenses.expenses.flatMap(_.hotelAndMealExpenses),
            otherAndCapitalAllowances = priorCustomerEmploymentExpenses.expenses.flatMap(_.otherAndCapitalAllowances),
            vehicleExpenses = priorCustomerEmploymentExpenses.expenses.flatMap(_.vehicleExpenses),
            mileageAllowanceRelief = priorCustomerEmploymentExpenses.expenses.flatMap(_.mileageAllowanceRelief)
          ),
          employmentExpensesData = Expenses(
            businessTravelCosts = model.expenses.businessTravelCosts,
            jobExpenses = model.expenses.jobExpenses,
            flatRateJobExpenses = model.expenses.flatRateJobExpenses,
            professionalSubscriptions = model.expenses.professionalSubscriptions,
            hotelAndMealExpenses = model.expenses.hotelAndMealExpenses,
            otherAndCapitalAllowances = model.expenses.otherAndCapitalAllowances,
            vehicleExpenses = model.expenses.vehicleExpenses,
            mileageAllowanceRelief = model.expenses.mileageAllowanceRelief
          )))


      await(underTest.performSubmitNrsPayload(model, prior = Some(priorExpenses), aUser)) shouldBe Right(())

    }
  }
}
