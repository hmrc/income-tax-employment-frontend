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

import audit.{AuditService, DeleteEmploymentExpensesAudit}
import common.EmploymentToRemove._
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import connectors.{DeleteOrIgnoreExpensesConnector, IncomeSourceConnector}
import models.employment.AllEmploymentData
import models.expenses.{DecodedDeleteEmploymentExpensesPayload, Expenses}
import models.{APIErrorModel, AuthorisationRequest, User}
import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class DeleteOrIgnoreExpensesService @Inject()(deleteOverrideExpensesConnector: DeleteOrIgnoreExpensesConnector,
                                              incomeSourceConnector: IncomeSourceConnector,
                                              auditService: AuditService,
                                              nrsService: NrsService,
                                              implicit val executionContext: ExecutionContext) extends Logging {

  def deleteOrIgnoreExpenses(user: User, employmentData: AllEmploymentData, taxYear: Int)
                            (implicit authorisationRequest: AuthorisationRequest[_], hc: HeaderCarrier): Future[Either[APIErrorModel, Unit]] = {

    val hmrcExpenses = employmentData.hmrcExpenses.filter(_.dateIgnored.isEmpty)
    val customerExpenses = employmentData.customerExpenses
    val eventualResult = (hmrcExpenses, customerExpenses) match {
      case (Some(_), Some(_)) =>
        sendAuditEvent(user, taxYear, employmentData)
        performSubmitNrsPayload(user, employmentData)
        handleConnectorCall(user, taxYear, all)
      case (Some(_), None) =>
        sendAuditEvent(user, taxYear, employmentData)
        performSubmitNrsPayload(user, employmentData)
        handleConnectorCall(user, taxYear, hmrcHeld)
      case (None, Some(_)) =>
        sendAuditEvent(user, taxYear, employmentData)
        performSubmitNrsPayload(user, employmentData)
        handleConnectorCall(user, taxYear, customer)
      case (None, None) =>
        logger.info(s"[DeleteOrIgnoreExpensesService][deleteOrIgnoreExpenses]" +
          s" No expenses data found for user and employmentId. SessionId: ${authorisationRequest.user.sessionId}")
        Future(Right())
    }

    eventualResult.flatMap {
      case Left(error) => Future(Left(error))
      case Right(_) =>
        incomeSourceConnector.put(taxYear, authorisationRequest.user.nino)(hc.withExtraHeaders("mtditid" -> authorisationRequest.user.mtditid)).map {
          case Left(error) => Left(error)
          case _ => Right()
        }
    }
  }

  private def sendAuditEvent(user: User, taxYear: Int, employmentData: AllEmploymentData)
                            (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Unit = {

    val latestExpenses: Expenses = employmentData.latestEOYExpenses.flatMap(expensesData =>
      expensesData.latestExpenses.expenses).getOrElse(Expenses())

    val auditModel = DeleteEmploymentExpensesAudit(
      taxYear,
      user.affinityGroup.toLowerCase,
      user.nino,
      user.mtditid,
      latestExpenses
    )
    auditService.sendAudit[DeleteEmploymentExpensesAudit](auditModel.toAuditModel)
  }

  def performSubmitNrsPayload(user: User, employmentData: AllEmploymentData)(implicit request: Request[_],
                                                                             hc: HeaderCarrier): Future[NrsSubmissionResponse] = {

    val latestExpenses = employmentData.latestEOYExpenses.map(expensesData =>
      DecodedDeleteEmploymentExpensesPayload(expensesData.latestExpenses.expenses).toNrsPayloadModel)

    nrsService.submit(user.nino, latestExpenses, user.mtditid)

  }

  private def handleConnectorCall(user: User, taxYear: Int, toRemove: String)(implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Unit]] = {
    deleteOverrideExpensesConnector.deleteOrIgnoreExpenses(user.nino, taxYear, toRemove)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
      case Left(error) => Left(error)
      case Right(_) => Right()
    }
  }
}
