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

import config.{AppConfig, ErrorHandler, MockCreateOrAmendExpensesConnector}
import models.employment._
import models.expenses.CreateExpensesRequestModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Ok
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

class CreateOrAmendExpensesServiceSpec extends UnitTest with MockCreateOrAmendExpensesConnector {

  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  val service: CreateOrAmendExpensesService = new CreateOrAmendExpensesService(mockCreateOrAmendExpensesConnector, errorHandler, mockExecutionContext)

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
      Some(expenses.copy(businessTravelCosts = None))
    )

  private val customerExpenses =
    EmploymentExpenses(
      None,
      None,
      Some(40),
      Some(expenses)
    )

  private def priorData(hmrcExpenses: Option[EmploymentExpenses], customerExpenses: Option[EmploymentExpenses]): AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(),
    hmrcExpenses = hmrcExpenses,
    customerEmploymentData = Seq(),
    customerExpenses = customerExpenses
  )

  private val expensesCyaData = ExpensesCYAModel(Expenses(Some(1), Some(2), Some(2), Some(2), Some(2), Some(2), Some(2), Some(2)), true)

  private val expensesUserData =
    ExpensesUserData(
      sessionId = sessionId,
      mtdItId = mtditid,
      nino = nino,
      taxYear = taxYear,
      isPriorSubmission = true,
      expensesCya = expensesCyaData
    )

  ".createOrAmendExpenses" should {

    "return a successful result" when {

      "there is both hmrc expenses and customer expenses" which {
        "expense request model contains ignoreExpenses(true) and expensesCyaData" in {
          mockCreateOrAmendExpensesSuccess(nino, taxYear, CreateExpensesRequestModel(Some(true), expensesCyaData.expenses))

          val response = service.createOrAmendExpense(expensesUserData, priorData(Some(hmrcExpensesWithoutDateIgnored), Some(customerExpenses)), taxYear)(Ok)

          await(response) shouldBe Ok
        }
      }

      "there is both hmrc expenses and customer expenses but hmrc data has dateIgnored" which {

        "expense request model only has expensesCyaData" in {
          mockCreateOrAmendExpensesSuccess(nino, taxYear, CreateExpensesRequestModel(None, expensesCyaData.expenses))

          val response = service.createOrAmendExpense(expensesUserData, priorData(Some(hmrcExpensesWithDateIgnored), Some(customerExpenses)), taxYear)(Ok)

          await(response) shouldBe Ok
        }
      }

      "there is hmrc data and no customer data" which {

        "expense request model contains ignoreExpenses(true) and expensesCyaData" in {
          mockCreateOrAmendExpensesSuccess(nino, taxYear, CreateExpensesRequestModel(Some(true), expensesCyaData.expenses))

          val response = service.createOrAmendExpense(expensesUserData, priorData(Some(hmrcExpensesWithoutDateIgnored), None), taxYear)(Ok)

          await(response) shouldBe Ok
        }
      }

      "there is customer data and no hmrc data" which {

        "expense request model only has expensesCyaData" in {
          mockCreateOrAmendExpensesSuccess(nino, taxYear, CreateExpensesRequestModel(None, expensesCyaData.expenses))

          val response = service.createOrAmendExpense(expensesUserData, priorData(None, Some(customerExpenses)), taxYear)(Ok)

          await(response) shouldBe Ok
        }
      }

      "there is no hmrc and no customer data but there is expensesCyaData" which {

        "expense request model only has expensesCyaData" in {
          mockCreateOrAmendExpensesSuccess(nino, taxYear, CreateExpensesRequestModel(None, expensesCyaData.expenses))

          val response = service.createOrAmendExpense(expensesUserData, priorData(None, None), taxYear)(Ok)

          await(response) shouldBe Ok
        }

      }

    }

    "returns an unsuccessful result" when {

      "the connector throws a Left" in {
        mockCreateOrAmendExpensesError(nino, taxYear, CreateExpensesRequestModel(Some(true), expensesCyaData.expenses))

        val response = service.createOrAmendExpense(expensesUserData, priorData(Some(hmrcExpensesWithoutDateIgnored), Some(customerExpenses)), taxYear)(Ok)

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }
    }

  }

}
