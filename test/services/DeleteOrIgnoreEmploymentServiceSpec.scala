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

package services

import config._
import controllers.employment.routes.EmploymentSummaryController
import models.employment._
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.i18n.MessagesApi
import play.api.mvc.Results.{Ok, Redirect}
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

class DeleteOrIgnoreEmploymentServiceSpec extends UnitTest
  with MockDeleteOrIgnoreEmploymentConnector
  with MockIncomeSourceConnector
  with MockEmploymentUserDataRepository {

  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  val service: DeleteOrIgnoreEmploymentService = new DeleteOrIgnoreEmploymentService(
    mockDeleteOrIgnoreEmploymentConnector,
    mockIncomeSourceConnector,
    errorHandler,
    mockExecutionContext
  )

  val taxYear = 2022
  val employmentId: String = "001"
  val differentEmploymentId: String = "003"

  val empSource: EmploymentSource = EmploymentSource(
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

  val data: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(empSource), hmrcExpenses = None, customerEmploymentData = Seq(empSource.copy(employmentId = "002")), None
  )

  ".deleteOrIgnoreEmployment" should {
    "return a successful result" when {
      "there is hmrc data and no customer data" which {
        "toRemove is equal to 'HMRC-HELD'" in {
          mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
          mockDeleteOrIgnoreEmploymentRight(nino, taxYear, employmentId, "HMRC-HELD")

          val response = service.deleteOrIgnoreEmployment(user, data.copy(customerEmploymentData = Seq()), taxYear, employmentId)(Ok)

          await(response) shouldBe Ok
        }
      }

      "there is customer data and no hmrc data" which {
        "toRemove is equal to 'CUSTOMER'" in {
          mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
          mockDeleteOrIgnoreEmploymentRight(nino, taxYear, "002", "CUSTOMER")

          val response = service.deleteOrIgnoreEmployment(user, data.copy(hmrcEmploymentData = Seq()), taxYear, "002")(Ok)

          await(response) shouldBe Ok
        }
      }
    }

    "returns an unsuccessful result" when {
      "there is no hmrc or customer data" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
        mockDeleteOrIgnoreEmploymentRight(nino, taxYear, "002", "CUSTOMER")

        val response = service.deleteOrIgnoreEmployment(user, data.copy(hmrcEmploymentData = Seq(), customerEmploymentData = Seq()), taxYear, "002")(Ok)

        await(response) shouldBe Redirect(EmploymentSummaryController.show(taxYear).url)
      }

      "there is no employment data for that employment id" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
        val response = service.deleteOrIgnoreEmployment(user, data, taxYear, differentEmploymentId)(Ok)

        await(response) shouldBe Redirect(EmploymentSummaryController.show(taxYear).url)
      }

      "the connector throws a Left" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
        mockDeleteOrIgnoreEmploymentLeft(nino, taxYear, "002", "CUSTOMER")

        val response = service.deleteOrIgnoreEmployment(user, data, taxYear, "002")(Ok)

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }

      "incomeSourceConnector returns error" in {
        mockRefreshIncomeSourceResponseError(taxYear, nino, "employment")
        mockDeleteOrIgnoreEmploymentRight(nino, taxYear, "002", "CUSTOMER")

        val response = service.deleteOrIgnoreEmployment(user, data.copy(hmrcEmploymentData = Seq()), taxYear, "002")(Ok)

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
