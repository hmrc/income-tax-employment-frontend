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
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import connectors.{DeleteOrIgnoreExpensesConnector, IncomeSourceConnector}
import controllers.employment.routes.EmploymentSummaryController
import models.AuthorisationRequest
import models.employment.AllEmploymentData
import models.expenses.DecodedDeleteEmploymentExpensesPayload
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class DeleteOrIgnoreExpensesService @Inject()(deleteOverrideExpensesConnector: DeleteOrIgnoreExpensesConnector,
                                              incomeSourceConnector: IncomeSourceConnector,
                                              errorHandler: ErrorHandler,
                                              nrsService: NrsService,
                                              implicit val executionContext: ExecutionContext) extends Logging {

  def deleteOrIgnoreExpenses(employmentData: AllEmploymentData, taxYear: Int)(result: Result)
                            (implicit authorisationRequest: AuthorisationRequest[_], hc: HeaderCarrier): Future[Result] = {

    val hmrcExpenses = employmentData.hmrcExpenses.filter(_.dateIgnored.isEmpty)
    val customerExpenses = employmentData.customerExpenses
    val eventualResult = (hmrcExpenses, customerExpenses) match {
      case (Some(_), Some(_)) =>
        performSubmitNrsPayload(employmentData)
        handleConnectorCall(taxYear, all)(result)
      case (Some(_), None) =>
        performSubmitNrsPayload(employmentData)
        handleConnectorCall(taxYear, hmrcHeld)(result)
      case (None, Some(_)) =>
        performSubmitNrsPayload(employmentData)
        handleConnectorCall(taxYear, customer)(result)
      case (None, None) =>
        logger.info(s"[DeleteOrIgnoreExpensesService][deleteOrIgnoreExpenses]" +
          s" No expenses data found for user and employmentId. SessionId: ${authorisationRequest.user.sessionId}")
        Future(Redirect(EmploymentSummaryController.show(taxYear)))
    }

    eventualResult.flatMap { result =>
      incomeSourceConnector.put(taxYear, authorisationRequest.user.nino, "employment")(hc.withExtraHeaders("mtditid" -> authorisationRequest.user.mtditid)).map {
        case Left(error) => errorHandler.handleError(error.status)
        case _ => result
      }
    }
  }

  def performSubmitNrsPayload(employmentData: AllEmploymentData)(implicit request: Request[_], authorisationRequest: AuthorisationRequest[_],
                                                                 hc: HeaderCarrier): Future[NrsSubmissionResponse] = {

    val latestExpenses = employmentData.latestEOYExpenses.map(expensesData =>
      DecodedDeleteEmploymentExpensesPayload(expensesData.latestExpenses.expenses).toNrsPayloadModel)

    nrsService.submit(authorisationRequest.user.nino, latestExpenses, authorisationRequest.user.mtditid)

  }

  private def handleConnectorCall(taxYear: Int, toRemove: String)(result: Result)
                                 (implicit authorisationRequest: AuthorisationRequest[_], hc: HeaderCarrier): Future[Result] = {
    deleteOverrideExpensesConnector.deleteOrIgnoreExpenses(authorisationRequest.user.nino, taxYear, toRemove)(hc.withExtraHeaders("mtditid" -> authorisationRequest.user.mtditid)).map {
      case Left(error) => errorHandler.handleError(error.status)
      case Right(_) => result
    }
  }
}
