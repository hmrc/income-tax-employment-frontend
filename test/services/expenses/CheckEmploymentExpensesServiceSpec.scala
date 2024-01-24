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
import models.requests.CreateUpdateExpensesRequest
import support.builders.models.UserBuilder.aUser
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import support.builders.models.expenses.ExpensesBuilder.anExpenses
import support.mocks.{MockAuditService, MockEmploymentSessionService}
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.ExecutionContext

class CheckEmploymentExpensesServiceSpec extends UnitTest
  with TaxYearProvider
  with MockAuditService
  with MockEmploymentSessionService {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val underTest = new CheckEmploymentExpensesService(mockAuditService)

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
}
