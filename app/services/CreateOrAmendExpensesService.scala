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

import config.ErrorHandler
import connectors.CreateOrAmendExpensesConnector
import models.User
import models.employment.{AllEmploymentData, EmploymentExpenses, Expenses}
import models.expenses.CreateExpensesRequestModel
import models.mongo.ExpensesUserData
import play.api.Logging
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class CreateOrAmendExpensesService @Inject()(createOrAmendExpensesConnector: CreateOrAmendExpensesConnector,
                                             errorHandler: ErrorHandler,
                                             implicit val executionContext: ExecutionContext) extends Logging {

  def createOrAmendExpense(expensesUserData: ExpensesUserData, priorData: AllEmploymentData, taxYear: Int)(result: Result)
                            (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {

    val hmrcExpenses: Option[EmploymentExpenses] = priorData.hmrcExpenses.filter(_.dateIgnored.isEmpty)

    hmrcExpenses match {
      case Some(_) => handleConnectorCall(taxYear, Some(true), expensesUserData.expensesCya.expenses.toExpenses)(result)
      case None => handleConnectorCall(taxYear, None, expensesUserData.expensesCya.expenses.toExpenses)(result)
    }
  }

  private def handleConnectorCall(taxYear: Int, ignoreExpenses: Option[Boolean], expenses: Expenses)(result: Result)
                                 (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {

    val hcWithMtditid = hc.withExtraHeaders("mtditid" -> user.mtditid)

    createOrAmendExpensesConnector.createOrAmendExpenses(user.nino, taxYear, CreateExpensesRequestModel(ignoreExpenses, expenses))(hcWithMtditid).map {
      case Left(error) => errorHandler.handleError(error.status)(user)
      case Right(_) => result
    }
  }

}
