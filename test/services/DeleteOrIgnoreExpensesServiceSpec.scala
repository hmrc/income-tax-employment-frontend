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

import config.{AppConfig, ErrorHandler, MockDeleteOrIgnoreExpensesConnector, MockIncomeSourceConnector}
import controllers.employment.routes.EmploymentSummaryController
import models.employment._
import models.expenses.Expenses
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.i18n.MessagesApi
import play.api.mvc.Results.{Ok, Redirect}
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

class DeleteOrIgnoreExpensesServiceSpec extends UnitTest
  with MockDeleteOrIgnoreExpensesConnector
  with MockIncomeSourceConnector {

  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  val service: DeleteOrIgnoreExpensesService = new DeleteOrIgnoreExpensesService(mockDeleteOrIgnoreExpensesConnector, mockIncomeSourceConnector, errorHandler, mockExecutionContext)

  val taxYear = 2022

  private val hmrcExpensesWithoutDateIgnored =
    EmploymentExpenses(
      None,
      None,
      Some(8),
      Some(Expenses(Some(1), Some(1), Some(1), Some(1), Some(1), Some(1), Some(1), Some(1)))
    )

  private val hmrcExpensesWithDateIgnored =
    EmploymentExpenses(
      None,
      Some("2020-04-04T01:01:01Z"),
      Some(8),
      Some(Expenses(Some(1), Some(1), Some(1), Some(1), Some(1), Some(1), Some(1), Some(1)))
    )

  private val customerExpenses =
    EmploymentExpenses(
      None,
      None,
      Some(40.00),
      Some(Expenses(Some(5), Some(5), Some(5), Some(5), Some(5), Some(5), Some(5), Some(5)))
    )

  def data(hmrcExpenses: Option[EmploymentExpenses], customerExpenses: Option[EmploymentExpenses]): AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(),
    hmrcExpenses = hmrcExpenses,
    customerEmploymentData = Seq(),
    customerExpenses = customerExpenses
  )

  ".deleteOrIgnoreExpenses" should {
    "return a successful result" when {
      "there is both hmrc expenses and customer expenses" which {
        "toRemove is equal to 'ALL'" in {
          mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
          mockDeleteOrIgnoreExpensesSuccess(nino, taxYear, "ALL")

          val response = service.deleteOrIgnoreExpenses(data(Some(hmrcExpensesWithoutDateIgnored), Some(customerExpenses)), taxYear)(Ok)

          await(response) shouldBe Ok
        }
      }

      "there is both hmrc expenses and customer expenses but hmrc data has dateIgnored" which {
        "toRemove is equal to 'CUSTOMER'" in {
          mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
          mockDeleteOrIgnoreExpensesSuccess(nino, taxYear, "CUSTOMER")

          val response = service.deleteOrIgnoreExpenses(data(Some(hmrcExpensesWithDateIgnored), Some(customerExpenses)), taxYear)(Ok)

          await(response) shouldBe Ok
        }
      }

      "there is hmrc data and no customer data" which {
        "toRemove is equal to 'HMRC-HELD'" in {
          mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
          mockDeleteOrIgnoreExpensesSuccess(nino, taxYear, "HMRC-HELD")

          val response = service.deleteOrIgnoreExpenses(data(Some(hmrcExpensesWithoutDateIgnored), None), taxYear)(Ok)

          await(response) shouldBe Ok
        }
      }

      "there is customer data and no hmrc data" which {
        "toRemove is equal to 'CUSTOMER'" in {
          mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
          mockDeleteOrIgnoreExpensesSuccess(nino, taxYear, "CUSTOMER")

          val response = service.deleteOrIgnoreExpenses(data(None, Some(customerExpenses)), taxYear)(Ok)

          await(response) shouldBe Ok
        }
      }
    }

    "returns an unsuccessful result" when {
      "there is no hmrc or customer data" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
        val response = service.deleteOrIgnoreExpenses(data(None, None), taxYear)(Ok)

        await(response) shouldBe Redirect(EmploymentSummaryController.show(taxYear).url)
      }

      "the connector throws a Left" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
        mockDeleteOrIgnoreExpensesError(nino, taxYear, "ALL")

        val response = service.deleteOrIgnoreExpenses(data(Some(hmrcExpensesWithoutDateIgnored), Some(customerExpenses)), taxYear)(Ok)

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }

      "incomeSourceConnector returns error" in {
        mockRefreshIncomeSourceResponseError(taxYear, nino, "employment")
        mockDeleteOrIgnoreExpensesSuccess(nino, taxYear, "CUSTOMER")

        val response = service.deleteOrIgnoreExpenses(data(None, Some(customerExpenses)), taxYear)(Ok)

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
