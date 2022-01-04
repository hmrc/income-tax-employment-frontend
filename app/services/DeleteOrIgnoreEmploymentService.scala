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
import connectors.{DeleteOrIgnoreEmploymentConnector, IncomeSourceConnector}
import controllers.employment.routes.EmploymentSummaryController
import models.User
import models.employment.{AllEmploymentData, EmploymentSource}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteOrIgnoreEmploymentService @Inject()(deleteOrIgnoreEmploymentConnector: DeleteOrIgnoreEmploymentConnector,
                                                incomeSourceConnector: IncomeSourceConnector,
                                                errorHandler: ErrorHandler,
                                                implicit val executionContext: ExecutionContext) extends Logging {

  def deleteOrIgnoreEmployment(user: User[_], employmentData: AllEmploymentData, taxYear: Int, employmentId: String)(result: Result)
                              (implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
    val eventualResult = (customerData(employmentData, employmentId), hmrcData(employmentData, employmentId)) match {
      case (_, Some(_)) => handleConnectorCall(user, taxYear, employmentId, hmrcHeld)(result)
      case (Some(_), _) => handleConnectorCall(user, taxYear, employmentId, customer)(result)
      case (None, None) =>
        logger.info(s"[DeleteOrIgnoreEmploymentService][deleteOrIgnoreEmployment]" +
          s" No employment data found for user and employmentId. SessionId: ${user.sessionId}")
        Future(Redirect(EmploymentSummaryController.show(taxYear)))
    }

    eventualResult.flatMap { result =>
      incomeSourceConnector.put(taxYear, user.nino, "employment")(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
        case Left(error) => errorHandler.handleError(error.status)
        case _ => result
      }
    }
  }

  private def hmrcData(allEmploymentData: AllEmploymentData, employmentId: String): Option[EmploymentSource] =
    allEmploymentData.hmrcEmploymentData.find(source => source.employmentId.equals(employmentId))

  private def customerData(allEmploymentData: AllEmploymentData, employmentId: String): Option[EmploymentSource] =
    allEmploymentData.customerEmploymentData.find(source => source.employmentId.equals(employmentId))

  private def handleConnectorCall(user: User[_], taxYear: Int, employmentId: String, toRemove: String)(result: Result)
                                 (implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {

    deleteOrIgnoreEmploymentConnector.deleteOrIgnoreEmployment(user.nino, taxYear, employmentId, toRemove)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
      case Left(error) => errorHandler.handleError(error.status)
      case Right(_) => result
    }
  }

}
