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
import controllers.employment.routes.EmploymentSummaryController
import models.employment._
import models.expenses.{Expenses, ExpensesViewModel}
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import models.requests
import models.requests.{CreateUpdateExpensesRequest, CreateUpdateExpensesRequestError, NothingToUpdate}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import scala.concurrent.Future

class CreateOrAmendExpensesServiceSpec extends UnitTest with MockCreateOrAmendExpensesConnector {

  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)
  val service: CreateOrAmendExpensesService = new CreateOrAmendExpensesService(mockCreateOrAmendExpensesConnector, errorHandler, mockExecutionContext)
  val taxYear = 2022
  val newAmount = BigDecimal("950.11")

  val expensesViewModel: ExpensesViewModel = ExpensesViewModel(jobExpensesQuestion = Some(true), jobExpenses = Some(100.11),
    flatRateJobExpensesQuestion = Some(true), flatRateJobExpenses = Some(200.22), professionalSubscriptionsQuestion = Some(true),
    professionalSubscriptions = Some(300.33), otherAndCapitalAllowancesQuestion = Some(true), otherAndCapitalAllowances = Some(400.44),
    businessTravelCosts = Some(500.55), hotelAndMealExpenses = Some(600.66), vehicleExpenses = Some(700.77), mileageAllowanceRelief = Some(800.88),
    isUsingCustomerData = false)

  val expenseCyaModel: ExpensesCYAModel = ExpensesCYAModel(expensesViewModel)
  private val expensesCyaData: ExpensesCYAModel = expenseCyaModel


  private val customerExpenses =
    EmploymentExpenses(
      None,
      None,
      Some(40),
      Some(Expenses(jobExpenses = Some(100.11), flatRateJobExpenses = Some(200.22),
        professionalSubscriptions = Some(300.33), otherAndCapitalAllowances = Some(400.44),
        businessTravelCosts = Some(500.55), hotelAndMealExpenses = Some(600.66), vehicleExpenses = Some(700.77), mileageAllowanceRelief = Some(800.88)))
    )

  private val expensesCyaDataWithNoExpenses: ExpensesCYAModel = ExpensesCYAModel(ExpensesViewModel(jobExpensesQuestion = Some(false), jobExpenses = None,
    flatRateJobExpensesQuestion = Some(false), flatRateJobExpenses = None, professionalSubscriptionsQuestion = Some(false),
    professionalSubscriptions = None, otherAndCapitalAllowancesQuestion = Some(false), otherAndCapitalAllowances = None,
    businessTravelCosts = None, hotelAndMealExpenses = None, vehicleExpenses = None, mileageAllowanceRelief = None,
    isUsingCustomerData = false))

  val fullCreateUpdateExpensesRequestWithIgnoreExpenses: CreateUpdateExpensesRequest = requests.CreateUpdateExpensesRequest(None, expensesCyaData.expenses.toExpenses)

  val AllEmpDataNoCustomerAndUnchangedHmrcExpenses: AllEmploymentData =
    AllEmploymentData(
      Seq.empty,
      Some(EmploymentExpenses(None, None, None, Some(expensesCyaData.expenses.toExpenses))),
      Seq(),
      None
    )

  val AllEmpDataHmrcExpensesCustomerAndUnchangedCustomerExpenses: AllEmploymentData =
    AllEmploymentData(
      Seq.empty,
      Some(EmploymentExpenses(None, None, None, Some(expensesCyaData.expenses.toExpenses))),
      Seq(),
      Some(customerExpenses)
    )

  val AllEmpDataNoHmrcExpensesAndUnchangedCustomerExpenses: AllEmploymentData =
    AllEmploymentData(
      Seq.empty,
      Some(EmploymentExpenses(None, None, None, Some(expensesCyaData.expenses.toExpenses))),
      Seq(),
      Some(customerExpenses)
    )

  private def expensesUserData(expensesCyaModel: ExpensesCYAModel = expensesCyaData) =
    ExpensesUserData(
      sessionId = sessionId,
      mtdItId = mtditid,
      nino = nino,
      taxYear = taxYear,
      isPriorSubmission = true,
      hasPriorExpenses = true,
      expensesCya = expensesCyaModel
    )

  val expensesModel: Expenses = expensesCyaData.expenses.toExpenses
  val updatedExpensesModel: Expenses = expensesCyaData.expenses.copy(flatRateJobExpenses = Some(BigDecimal("1000.01"))).toExpenses

  "createModelAndReturnResult" should {

    "return to employment summary when there is nothing changed in relation to the hmrc expenses and customer expenses" in {
      lazy val response = service.createExpensesModelAndReturnResult(
        expensesUserData(), Some(
          AllEmpDataHmrcExpensesCustomerAndUnchangedCustomerExpenses
        ), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe EmploymentSummaryController.show(taxYear).url
    }

    "return to employment summary when there is nothing changed in relation to the customer expenses when no hmrc expenses" in {
      lazy val response = service.createExpensesModelAndReturnResult(
        expensesUserData(), Some(
          AllEmpDataNoHmrcExpensesAndUnchangedCustomerExpenses
        ), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe EmploymentSummaryController.show(taxYear).url
    }

    "return to employment summary when there is nothing changed in relation to the hmrc expenses when no customer expenses" in {
      lazy val response = service.createExpensesModelAndReturnResult(
        expensesUserData(), Some(
          AllEmpDataNoCustomerAndUnchangedHmrcExpenses
        ), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe EmploymentSummaryController.show(taxYear).url
    }

    "redirect after successfully posting changed expensesUserData when there are no prior customer expenses or hmrc expenses" in {
      lazy val response = service.createExpensesModelAndReturnResult(
        expensesUserData(), Some(
          AllEmploymentData(
            Seq.empty,
            None,
            Seq(),
            None
          )
        ), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "redirect after successfully posting expensesUserData with no expenses when there are no prior customer expenses or hmrc expenses" in {
      lazy val response = service.createExpensesModelAndReturnResult(
        expensesUserData().copy(expensesCya = expensesCyaDataWithNoExpenses), Some(
          AllEmploymentData(
            Seq.empty,
            None,
            Seq(),
            None
          )
        ), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "redirect after successfully posting a minimal change to the expensesUserData data" when {
      "there is no prior customer expenses but hmrc expenses exists with a date ignored" in {

        lazy val response = service.createExpensesModelAndReturnResult(
          expensesUserData(expensesCyaData.copy(expensesViewModel.copy(flatRateJobExpenses = Some(newAmount)))), Some(
            AllEmploymentData(
              Seq.empty,
              Some(EmploymentExpenses(None, dateIgnored = Some("2021-01-01"), None, Some(expensesCyaData.expenses.toExpenses))),
              Seq(),
              None
            )
          ), taxYear
        )(_ => Future.successful(Redirect("303")))

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "303"
      }
    }

    "redirect after successfully posting a minimal change to the expensesUserData data" when {
      "there is no prior customer expenses  but prior hmrc expenses exists with date ignored is empty" in {

        lazy val response = service.createExpensesModelAndReturnResult(
          expensesUserData(expensesCyaData.copy(expensesViewModel.copy(flatRateJobExpenses = Some(newAmount)))), Some(
            AllEmploymentData(
              Seq.empty,
              Some(EmploymentExpenses(None, None, None, Some(expensesCyaData.expenses.toExpenses))),
              Seq(),
              None
            )
          ), taxYear
        )(_ => Future.successful(Redirect("303")))

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "303"
      }
    }

    "redirect after successfully posting a minimal change to the expensesUserData data" when {
      "there are hmrc expenses with date ignored but no customer expenses" in {

        val newAmount = BigDecimal("950.11")
        lazy val response = service.createExpensesModelAndReturnResult(
          expensesUserData(expensesCyaData.copy(expensesViewModel.copy(flatRateJobExpenses = Some(newAmount)))), Some(
            AllEmploymentData(
              Seq.empty,
              Some(EmploymentExpenses(None, dateIgnored = Some("2021-01-01"), None, Some(expensesCyaData.expenses.toExpenses))),
              Seq(),
              None
            )
          ), taxYear
        )(_ => Future.successful(Redirect("303")))

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "303"
      }
    }
  }

  "createOrUpdateExpensesResult" should {
    "return a successful result using the request model to make the api call and return the correct redirect" when {
      "hmrc expenses data has ignoreExpenses(true)" in {

        mockCreateOrAmendExpensesSuccess(nino, taxYear, requests.CreateUpdateExpensesRequest(Some(true), expensesModel))

        val response: Future[Either[Result, Result]] = service.createOrUpdateExpensesResult(
          taxYear, fullCreateUpdateExpensesRequestWithIgnoreExpenses.copy(ignoreExpenses = Some(true)))
        status(response.map(_.right.get)) shouldBe SEE_OTHER
        redirectUrl(response.map(_.right.get)) shouldBe EmploymentSummaryController.show(taxYear).url
      }
    }

    "return a successful result using the request model to make the api call and return the correct redirect" when {
      "hmrc expenses data has dateIgnored" in {

        mockCreateOrAmendExpensesSuccess(nino, taxYear, fullCreateUpdateExpensesRequestWithIgnoreExpenses)

        val response: Future[Either[Result, Result]] = service.createOrUpdateExpensesResult(
          taxYear, fullCreateUpdateExpensesRequestWithIgnoreExpenses)
        status(response.map(_.right.get)) shouldBe SEE_OTHER
        redirectUrl(response.map(_.right.get)) shouldBe EmploymentSummaryController.show(taxYear).url
      }
    }

    "return a successful result using the request model to make the api call and return the correct redirect" when {
      "the updated CYA expenses contains no expenses data" in {

        mockCreateOrAmendExpensesSuccess(nino, taxYear, fullCreateUpdateExpensesRequestWithIgnoreExpenses.copy(expenses = Expenses()))

        val response: Future[Either[Result, Result]] = service.createOrUpdateExpensesResult(
          taxYear, fullCreateUpdateExpensesRequestWithIgnoreExpenses.copy(expenses = Expenses()))
        status(response.map(_.right.get)) shouldBe SEE_OTHER
        redirectUrl(response.map(_.right.get)) shouldBe EmploymentSummaryController.show(taxYear).url
      }
    }

    "use the request model to make the api call and handle an error when the connector throws a Left" in {
      mockCreateOrAmendExpensesError(nino, taxYear, requests.CreateUpdateExpensesRequest(None, expensesModel))
      lazy val response: Future[Either[Result, Result]] = service.createOrUpdateExpensesResult(taxYear, fullCreateUpdateExpensesRequestWithIgnoreExpenses)
      status(response.map(_.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "cyaAndPriorToCreateUpdateExpensesRequest" should {
    "return a Left(NothingToUpdate) if there are no customer expenses and cya data is unchanged in relation to hmrc expenses" in {
      val result: Either[CreateUpdateExpensesRequestError, CreateUpdateExpensesRequest] = service.cyaAndPriorToCreateUpdateExpensesRequest(
        expensesUserData(), Some(
          AllEmpDataNoCustomerAndUnchangedHmrcExpenses
        )
      )
      result shouldBe Left(NothingToUpdate)
    }

    "return a Left(NothingToUpdate) if cya data is unchanged in relation to customer expenses when hmrc expenses also present" in {
      val result: Either[CreateUpdateExpensesRequestError, CreateUpdateExpensesRequest] = service.cyaAndPriorToCreateUpdateExpensesRequest(
        expensesUserData(), Some(
          AllEmpDataHmrcExpensesCustomerAndUnchangedCustomerExpenses
        )
      )
      result shouldBe Left(NothingToUpdate)
    }

    "return a Left(NothingToUpdate) if cya data is unchanged in relation to customer expenses when no hmrc expenses" in {
      val result: Either[CreateUpdateExpensesRequestError, CreateUpdateExpensesRequest] = service.cyaAndPriorToCreateUpdateExpensesRequest(
        expensesUserData(), Some(
          AllEmpDataNoHmrcExpensesAndUnchangedCustomerExpenses
        )
      )
      result shouldBe Left(NothingToUpdate)
    }

    "return a Right when no customer expenses but there are cya data changes in relation to hmrc expenses" in {
      val result: Either[CreateUpdateExpensesRequestError, CreateUpdateExpensesRequest] = service.cyaAndPriorToCreateUpdateExpensesRequest(
        expensesUserData(expensesCyaData.copy(expensesViewModel.copy(flatRateJobExpenses = Some(newAmount)))), Some(
          AllEmploymentData(
            Seq.empty,
            Some(EmploymentExpenses(None, None, None, Some(expensesCyaData.expenses.toExpenses))),
            Seq(),
            None
          )
        )
      )
      result.isRight shouldBe true
    }

    "return a Right when no hmrc expenses but there are changes in relation to customer expenses" in {
      val result: Either[CreateUpdateExpensesRequestError, CreateUpdateExpensesRequest] = service.cyaAndPriorToCreateUpdateExpensesRequest(
        expensesUserData(expensesCyaData.copy(expensesViewModel.copy(flatRateJobExpenses = Some(newAmount)))), Some(
          AllEmploymentData(
            Seq.empty,
            None,
            Seq(),
            Some(EmploymentExpenses(None, None, None, Some(expensesCyaData.expenses.toExpenses))),
          )
        )
      )
      result.isRight shouldBe true
    }

    "return a Right when there are prior hmrc expenses or customer expenses but updated cya data" in {
      val result: Either[CreateUpdateExpensesRequestError, CreateUpdateExpensesRequest] = service.cyaAndPriorToCreateUpdateExpensesRequest(
        expensesUserData(expensesCyaData.copy(expensesViewModel.copy(flatRateJobExpenses = Some(newAmount)))), Some(
          AllEmploymentData(
            Seq.empty,
            None,
            Seq(),
            None,
          )
        )
      )
      result.isRight shouldBe true
    }

  }

}
