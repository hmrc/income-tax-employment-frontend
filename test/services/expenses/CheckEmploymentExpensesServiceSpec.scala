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

package services.expenses

import audit.{AmendEmploymentExpensesUpdateAudit, AuditEmploymentExpensesData, AuditNewEmploymentExpensesData, CreateNewEmploymentExpensesAudit}
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import config.MockAuditService
import models.employment.AllEmploymentData
import models.requests.CreateUpdateExpensesRequest
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.UnitTest

class CheckEmploymentExpensesServiceSpec extends UnitTest with MockAuditService {

  private val taxYear = 2021

  private val underTest = new CheckEmploymentExpensesService(mockAuditService)

  "calling performSubmitAudit" should {
    "send the audit events from the model when it's a create" in {
      val model: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(
        Some(true),
        expenses)
      val prior = None

      verifyAuditEvent(CreateNewEmploymentExpensesAudit(
        taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, AuditNewEmploymentExpensesData(
          expenses.jobExpenses,
          expenses.flatRateJobExpenses,
          expenses.professionalSubscriptions,
          expenses.otherAndCapitalAllowances
        )
      ).toAuditModel)
      await(underTest.performSubmitAudits(model, taxYear, prior)) shouldBe AuditResult.Success
    }

    "send the audit events from the model when it's a create and theres existing data" in {
      val model: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(
        Some(true),
        expenses)

      val prior: AllEmploymentData = anAllEmploymentData.copy(
        hmrcExpenses = None,
        customerExpenses = None
      )

      verifyAuditEvent(CreateNewEmploymentExpensesAudit(
        taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, AuditNewEmploymentExpensesData(
          expenses.jobExpenses,
          expenses.flatRateJobExpenses,
          expenses.professionalSubscriptions,
          expenses.otherAndCapitalAllowances
        )
      ).toAuditModel)
      await(underTest.performSubmitAudits(model, taxYear, Some(prior))) shouldBe AuditResult.Success
    }
  }

  "send the audit events from the model when it's a amend and there is existing data" in {
    val model: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(Some(true),
      expenses)
    val priorCustomerEmploymentExpenses = employmentExpenses.copy(
      expenses = Some(this.expenses.copy(
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

    verifyAuditEvent(AmendEmploymentExpensesUpdateAudit(
      taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid,
      priorEmploymentExpensesData = AuditEmploymentExpensesData(
        priorCustomerEmploymentExpenses.expenses.get.jobExpenses,
        priorCustomerEmploymentExpenses.expenses.get.flatRateJobExpenses,
        priorCustomerEmploymentExpenses.expenses.get.professionalSubscriptions,
        priorCustomerEmploymentExpenses.expenses.get.otherAndCapitalAllowances
      ),
      employmentExpensesData = AuditEmploymentExpensesData
      (
        expenses.jobExpenses,
        expenses.flatRateJobExpenses,
        expenses.professionalSubscriptions,
        expenses.otherAndCapitalAllowances
      )
    ).toAuditModel)
    await(underTest.performSubmitAudits(model, taxYear, Some(prior))) shouldBe AuditResult.Success
  }
}
