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

import audit.{AmendEmploymentExpensesUpdateAudit, AuditEmploymentExpensesData, AuditNewEmploymentExpensesData, CreateNewEmploymentExpensesAudit}
import common.SessionValues
import config.{MockAuditService, MockEmploymentSessionService}
import controllers.expenses.CheckEmploymentExpensesController
import models.employment.AllEmploymentData
import models.expenses.createUpdate.CreateUpdateExpensesRequest
import play.api.http.HeaderNames.LOCATION
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.header
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.UnitTestWithApp
import views.html.expenses.{CheckEmploymentExpensesView, CheckEmploymentExpensesViewEOY}

import scala.concurrent.Future

class CheckEmploymentExpensesControllerSpec extends UnitTestWithApp with DefaultAwaitTimeout with MockEmploymentSessionService with MockAuditService {

  lazy val view: CheckEmploymentExpensesView = app.injector.instanceOf[CheckEmploymentExpensesView]
  lazy val viewEOY: CheckEmploymentExpensesViewEOY = app.injector.instanceOf[CheckEmploymentExpensesViewEOY]

  lazy val controller = new CheckEmploymentExpensesController(
    authorisedAction,
    view,
    viewEOY,
    createOrAmendExpensesService,
    mockEmploymentSessionService,
    mockAuditService,
    inYearAction,
    mockErrorHandler,
    testClock,
    mockAppConfig,
    mockMessagesControllerComponents,
    ec
  )

  val taxYear = 2022

  "calling show() as an individual" should {

    "return status code 303 with correct Location header" when {
      "there is no expenses data in the database" in new TestWithAuth {
        val responseF: Future[Result] = {
          mockFind(taxYear,Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller.show(taxYear)(fakeRequest)
        }

        status(responseF) shouldBe SEE_OTHER
        header(LOCATION, responseF) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return status code 200 with correct content" when {
      "there is expenses data in the database" in new TestWithAuth {

        val request: FakeRequest[AnyContentAsEmpty.type] =
          fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          )

        val responseF: Future[Result] = {
          mockFind(taxYear,Ok(view(taxYear, employmentsModel.hmrcExpenses.get.expenses.get, isInYear = true, isMultipleEmployments = true)))
          controller.show(taxYear)(request)
        }

        status(responseF) shouldBe OK
      }
    }

  }

  "calling show() as an agent" should {

    "return status code 303 with correct Location header" when {
      "there is no expenses data in the database" in new TestWithAuth(isAgent = true) {
        val responseF: Future[Result] = {
          mockFind(taxYear,Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller.show(taxYear)(fakeRequestWithMtditidAndNino)
        }

        status(responseF) shouldBe SEE_OTHER
        header(LOCATION, responseF) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return status code 200 with correct content" when {
      "there is expenses data in the database" in new TestWithAuth(isAgent = true) {
        val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithMtditidAndNino

        val responseF: Future[Result] = {
          mockFind(taxYear,Ok(view(taxYear, employmentsModel.hmrcExpenses.get.expenses.get, isInYear = true, isMultipleEmployments = true)))
          controller.show(taxYear)(request)
        }

        status(responseF) shouldBe OK
      }
    }

  }
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
      await(controller.performSubmitAudits(model, taxYear, prior)) shouldBe AuditResult.Success
    }

    "send the audit events from the model when it's a create and theres existing data" in {

      val model: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(
        Some(true),
        expenses)

      val prior: AllEmploymentData = employmentsModel.copy(
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
      await(controller.performSubmitAudits(model, taxYear, Some(prior))) shouldBe AuditResult.Success
    }

  }

  "send the audit events from the model when it's a amend and there is existing data" in {

    val model: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(Some(true),
      expenses)

    val priorCustomerEmploymentExpenses = employmentExpenses.copy(
      expenses = Some ( this.expenses.copy(
        jobExpenses = Some(0.0),
        flatRateJobExpenses = Some(0.0),
        professionalSubscriptions = Some(0.0),
        otherAndCapitalAllowances = Some(0.0)
      ) ))

    val prior: AllEmploymentData = employmentsModel.copy(
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
    await(controller.performSubmitAudits(model, taxYear, Some(prior))) shouldBe AuditResult.Success
  }

}
