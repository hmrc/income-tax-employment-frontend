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

import config.ErrorHandler
import connectors.CreateOrAmendExpensesConnector
import connectors.parsers.CreateOrAmendExpensesHttpParser.CreateOrAmendExpensesResponse
import controllers.employment.routes.EmploymentSummaryController
import javax.inject.Inject
import models.employment.{AllEmploymentData, EmploymentExpenses}
import models.expenses.{Expenses, ExpensesDataRemainsUnchanged}
import models.mongo.ExpensesUserData
import models.requests.{CreateUpdateExpensesRequest, CreateUpdateExpensesRequestError, NothingToUpdate}
import models.{AuthorisationRequest, User}
import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendExpensesService @Inject()(createOrAmendExpensesConnector: CreateOrAmendExpensesConnector,
                                             errorHandler: ErrorHandler,
                                             implicit val executionContext: ExecutionContext) extends Logging {

  def createOrUpdateExpensesResult(taxYear: Int, expensesRequest: CreateUpdateExpensesRequest)
                                  (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Either[Result, Result]] = {
    createOrUpdateExpenses(request.user, taxYear, expensesRequest).map {
      case Left(error) => Left(errorHandler.handleError(error.status))
      case Right(_) => Right(Redirect(EmploymentSummaryController.show(taxYear)))
    }
  }

  private def createOrUpdateExpenses(user: User, taxYear: Int, expensesRequest: CreateUpdateExpensesRequest)
                                    (implicit hc: HeaderCarrier): Future[CreateOrAmendExpensesResponse] = {
    createOrAmendExpensesConnector.createOrAmendExpenses(user.nino, taxYear,
      expensesRequest)(hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def createExpensesModelAndReturnResult(user: User, cya: ExpensesUserData, prior: Option[AllEmploymentData], taxYear: Int)
                                        (result: CreateUpdateExpensesRequest => Future[Result]): Future[Result] = {
    cyaAndPriorToCreateUpdateExpensesRequest(user, cya, prior) match {
      //TODO Route to: journey not finished page / show banner saying not finished / hide submit button when not complete?
      case Left(NothingToUpdate) => Future.successful(Redirect(EmploymentSummaryController.show(taxYear)))
      case Right(model) => result(model)
    }
  }

  def cyaAndPriorToCreateUpdateExpensesRequest(user: User,
                                               cya: ExpensesUserData,
                                               prior: Option[AllEmploymentData]): Either[CreateUpdateExpensesRequestError, CreateUpdateExpensesRequest] = {

    val hmrcExpenses: Option[EmploymentExpenses] = prior.flatMap(res => res.hmrcExpenses.filter(_.dateIgnored.isEmpty))
    val expensesData = formCreateUpdateExpenses(cya, prior)

    if (expensesData.dataHasNotChanged) {
      logger.info(s"[CreateOrAmendExpensesService][cyaAndPriorToCreateUpdateExpensesRequest] " +
        s"Data to be submitted matched the prior data exactly. Nothing to update. SessionId: ${user.sessionId}")
      Left(NothingToUpdate: CreateUpdateExpensesRequestError)
    } else {
      Right(CreateUpdateExpensesRequest(
        ignoreExpenses = if (hmrcExpenses.isDefined) Some(true) else None,
        expenses = expensesData.data))
    }
  }

  private def formCreateUpdateExpenses(cya: ExpensesUserData, prior: Option[AllEmploymentData]): ExpensesDataRemainsUnchanged[Expenses] = {
    lazy val newCreateUpdateExpenses = {
      Expenses(
        cya.expensesCya.expenses.businessTravelCosts,
        cya.expensesCya.expenses.jobExpenses,
        cya.expensesCya.expenses.flatRateJobExpenses,
        cya.expensesCya.expenses.professionalSubscriptions,
        cya.expensesCya.expenses.hotelAndMealExpenses,
        cya.expensesCya.expenses.otherAndCapitalAllowances,
        cya.expensesCya.expenses.vehicleExpenses,
        cya.expensesCya.expenses.mileageAllowanceRelief
      )
    }

    lazy val default = ExpensesDataRemainsUnchanged(newCreateUpdateExpenses, dataHasNotChanged = false)

    prior.fold[ExpensesDataRemainsUnchanged[Expenses]](default) {
      prior =>
        val priorExpensesData: Option[EmploymentExpenses] = prior.latestEOYExpenses.map(_.latestExpenses)
        priorExpensesData.fold[ExpensesDataRemainsUnchanged[Expenses]](default)(
          prior => ExpensesDataRemainsUnchanged(newCreateUpdateExpenses, prior.dataHasNotChanged(newCreateUpdateExpenses))
        )
    }
  }
}
