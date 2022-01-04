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

import common.EmploymentToRemove._
import config.ErrorHandler
import connectors.{DeleteOrIgnoreExpensesConnector, IncomeSourceConnector}
import controllers.employment.routes.EmploymentSummaryController
import models.User
import models.employment.AllEmploymentData
import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class DeleteOrIgnoreExpensesService @Inject()(deleteOverrideExpensesConnector: DeleteOrIgnoreExpensesConnector,
                                              incomeSourceConnector: IncomeSourceConnector,
                                              errorHandler: ErrorHandler,
                                              implicit val executionContext: ExecutionContext) extends Logging {

  def deleteOrIgnoreExpenses(employmentData: AllEmploymentData, taxYear: Int)(result: Result)
                            (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {

    val hmrcExpenses = employmentData.hmrcExpenses.filter(_.dateIgnored.isEmpty)
    val customerExpenses = employmentData.customerExpenses
    val eventualResult = (hmrcExpenses, customerExpenses) match {
      case (Some(_), Some(_)) => handleConnectorCall(taxYear, all)(result)
      case (Some(_), None) => handleConnectorCall(taxYear, hmrcHeld)(result)
      case (None, Some(_)) => handleConnectorCall(taxYear, customer)(result)
      case (None, None) =>
        logger.info(s"[DeleteOrIgnoreExpensesService][deleteOrIgnoreExpenses]" +
          s" No expenses data found for user and employmentId. SessionId: ${user.sessionId}")
        Future(Redirect(EmploymentSummaryController.show(taxYear)))
    }

    eventualResult.flatMap { result =>
      incomeSourceConnector.put(taxYear, user.nino, "employment")(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
        case Left(error) => errorHandler.handleError(error.status)
        case _ => result
      }
    }
  }

  private def handleConnectorCall(taxYear: Int, toRemove: String)(result: Result)
                                 (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    deleteOverrideExpensesConnector.deleteOrIgnoreExpenses(user.nino, taxYear, toRemove)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
      case Left(error) => errorHandler.handleError(error.status)
      case Right(_) => result
    }
  }
}
