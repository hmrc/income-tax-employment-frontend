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

package services

import audit.DeleteEmploymentAudit
import config._
import controllers.employment.routes.EmploymentSummaryController
import models.employment._
import models.expenses.Expenses
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.i18n.MessagesApi
import play.api.mvc.Results.{Ok, Redirect}
import services.employment.RemoveEmploymentService
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

class RemoveEmploymentServiceSpec extends UnitTest
  with MockDeleteOrIgnoreEmploymentConnector
  with MockIncomeSourceConnector
  with MockAuditService
  with MockEmploymentUserDataRepository
  with MockNrsService {

  private val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  private val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  private val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  private val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  private val service: RemoveEmploymentService = new RemoveEmploymentService(
    mockDeleteOrIgnoreEmploymentConnector,
    mockIncomeSourceConnector,
    mockAuditService,
    errorHandler,
    mockNrsService,
    mockExecutionContext
  )

  private val taxYear = 2022
  private val employmentId: String = "001"
  private val differentEmploymentId: String = "003"

  private val empSource: EmploymentSource = EmploymentSource(
    employmentId = "001",
    employerName = "maggie",
    employerRef = None,
    payrollId = None,
    startDate = None,
    cessationDate = None,
    dateIgnored = None,
    submittedOn = None,
    employmentData = None,
    employmentBenefits = None
  )

  private val data: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(empSource),
    hmrcExpenses = None,
    customerEmploymentData = Seq(empSource.copy(employmentId = "002")),
    customerExpenses = None
  )

  private val customerExpenses =
    EmploymentExpenses(
      None,
      None,
      Some(40.00),
      Some(Expenses(Some(5), Some(5), Some(5), Some(5), Some(5), Some(5), Some(5), Some(5)))
    )

  private val dataWithExpenses: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(empSource),
    hmrcExpenses = None,
    customerEmploymentData = Seq(empSource.copy(employmentId = "001")),
    customerExpenses = Some(customerExpenses)
  )

  ".deleteOrIgnoreEmployment" should {
    "return a successful result" when {
      "there is hmrc data and no customer data" which {
        "toRemove is equal to 'HMRC-HELD'" in {
          val allEmploymentData = data.copy(customerEmploymentData = Seq())
          val hmrcDataSource = data.hmrcEmploymentData.find(_.employmentId.equals(employmentId)).get
          val employmentDetailsViewModel: EmploymentDetailsViewModel = hmrcDataSource.toEmploymentDetailsViewModel(isUsingCustomerData = false)
          val deleteEmploymentAudit = DeleteEmploymentAudit(taxYear, "individual", nino, mtditid, employmentDetailsViewModel, None, None)

          mockAuditSendEvent(deleteEmploymentAudit.toAuditModel)
          verifySubmitEvent(DecodedDeleteEmploymentPayload(
            employmentData = employmentDetailsViewModel,
            benefits = None,
            expenses = None
          ).toNrsPayloadModel)

          mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
          mockDeleteOrIgnoreEmploymentRight(nino, taxYear, employmentId, "HMRC-HELD")

          await(service.deleteOrIgnoreEmployment(allEmploymentData, taxYear, employmentId)(Ok)) shouldBe Ok
        }
      }

      "there is customer data and no hmrc data" which {
        "toRemove is equal to 'CUSTOMER'" in {
          val allEmploymentData = data.copy(hmrcEmploymentData = Seq())
          val customerDataSource = data.customerEmploymentData.find(_.employmentId.equals("002")).get
          val employmentDetailsViewModel: EmploymentDetailsViewModel = customerDataSource.toEmploymentDetailsViewModel(isUsingCustomerData = true)
          val deleteEmploymentAudit = DeleteEmploymentAudit(taxYear, "individual", nino, mtditid, employmentDetailsViewModel, None, None)

          mockAuditSendEvent(deleteEmploymentAudit.toAuditModel)
          verifySubmitEvent(DecodedDeleteEmploymentPayload(
            employmentData = employmentDetailsViewModel,
            benefits = None,
            expenses = None
          ).toNrsPayloadModel)
          mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
          mockDeleteOrIgnoreEmploymentRight(nino, taxYear, "002", "CUSTOMER")

          await(service.deleteOrIgnoreEmployment(allEmploymentData, taxYear, employmentId = "002")(Ok)) shouldBe Ok
        }
      }
    }

    "returns an unsuccessful result" when {
      "there is no hmrc or customer data" in {
        val allEmploymentData = data.copy(hmrcEmploymentData = Seq(), customerEmploymentData = Seq())

        mockRefreshIncomeSourceResponseSuccess(taxYear, nino, incomeSource = "employment")
        mockDeleteOrIgnoreEmploymentRight(nino, taxYear, employmentId = "002", toRemove = "CUSTOMER")

        await(service.deleteOrIgnoreEmployment(allEmploymentData, taxYear, employmentId = "002")(Ok)) shouldBe Redirect(EmploymentSummaryController.show(taxYear).url)
      }

      "there is no employment data for that employment id" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")

        await(service.deleteOrIgnoreEmployment(data, taxYear, differentEmploymentId)(Ok)) shouldBe Redirect(EmploymentSummaryController.show(taxYear).url)
      }

      "the connector throws a Left" in {
        val customerDataSource = data.customerEmploymentData.find(_.employmentId.equals("002")).get
        val employmentDetailsViewModel: EmploymentDetailsViewModel = customerDataSource.toEmploymentDetailsViewModel(isUsingCustomerData = true)
        val deleteEmploymentAudit = DeleteEmploymentAudit(taxYear, "individual", nino, mtditid, employmentDetailsViewModel, None, None)

        mockAuditSendEvent(deleteEmploymentAudit.toAuditModel)
        verifySubmitEvent(DecodedDeleteEmploymentPayload(
          employmentData = employmentDetailsViewModel,
          benefits = None,
          expenses = None
        ).toNrsPayloadModel)
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
        mockDeleteOrIgnoreEmploymentLeft(nino, taxYear, "002", "CUSTOMER")

        status(service.deleteOrIgnoreEmployment(data, taxYear, "002")(Ok)) shouldBe INTERNAL_SERVER_ERROR
      }

      "incomeSourceConnector returns error" in {
        val allEmploymentData = data.copy(hmrcEmploymentData = Seq())
        val customerDataSource = data.customerEmploymentData.find(_.employmentId.equals("002")).get
        val employmentDetailsViewModel: EmploymentDetailsViewModel = customerDataSource.toEmploymentDetailsViewModel(isUsingCustomerData = true)
        val deleteEmploymentAudit = DeleteEmploymentAudit(taxYear, "individual", nino, mtditid, employmentDetailsViewModel, None, None)

        mockAuditSendEvent(deleteEmploymentAudit.toAuditModel)
        verifySubmitEvent(DecodedDeleteEmploymentPayload(
          employmentData = employmentDetailsViewModel,
          benefits = None,
          expenses = None
        ).toNrsPayloadModel)
        mockRefreshIncomeSourceResponseError(taxYear, nino, "employment")
        mockDeleteOrIgnoreEmploymentRight(nino, taxYear, "002", "CUSTOMER")

        status(service.deleteOrIgnoreEmployment(allEmploymentData, taxYear, "002")(Ok)) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "calling .performSubmitNrsPayload" should {
    "send the event from the model" in {

      val customerDataSource = dataWithExpenses.customerEmploymentData.find(_.employmentId.equals("001")).get

      verifySubmitEvent(DecodedDeleteEmploymentPayload(
        employmentData = customerDataSource.toEmploymentDetailsViewModel(isUsingCustomerData = true),
        benefits = None,
        expenses = customerExpenses.expenses
        ).toNrsPayloadModel
      )

      await(service.performSubmitNrsPayload(dataWithExpenses, empSource, isUsingCustomerData = true)) shouldBe Right()

    }
  }
}
