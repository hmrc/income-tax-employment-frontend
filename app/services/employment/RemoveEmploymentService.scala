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

package services.employment


import audit.{AuditService, DeleteEmploymentAudit}
import common.EmploymentToRemove._
import config.ErrorHandler
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import connectors.{DeleteOrIgnoreEmploymentConnector, IncomeSourceConnector}
import controllers.employment.routes.EmploymentSummaryController
import models.User
import models.employment.{AllEmploymentData, DecodedDeleteEmploymentPayload, EmploymentSource}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import services.NrsService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

// TODO: Refactor this service to be a proper service and does not return Result
class RemoveEmploymentService @Inject()(deleteOrIgnoreEmploymentConnector: DeleteOrIgnoreEmploymentConnector,
                                        incomeSourceConnector: IncomeSourceConnector,
                                        auditService: AuditService,
                                        errorHandler: ErrorHandler,
                                        nrsService: NrsService,
                                        implicit val ec: ExecutionContext) extends Logging {

  def deleteOrIgnoreEmployment(employmentData: AllEmploymentData, taxYear: Int, employmentId: String)(result: Result)
                              (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    val hmrcDataSource = employmentData.hmrcEmploymentData.find(_.employmentId.equals(employmentId))
    val customerDataSource = employmentData.customerEmploymentData.find(_.employmentId.equals(employmentId))

    val eventualResult = (customerDataSource, hmrcDataSource) match {
      case (_, Some(hmrcEmploymentSource)) =>
        sendAuditEvent(employmentData, taxYear, hmrcEmploymentSource, isUsingCustomerData = false)
        performSubmitNrsPayload(employmentData, hmrcEmploymentSource, isUsingCustomerData = false)
        handleConnectorCall(user, taxYear, employmentId, hmrcHeld)(result)
      case (Some(customerEmploymentSource), _) =>
        sendAuditEvent(employmentData, taxYear, customerEmploymentSource, isUsingCustomerData = true)
        performSubmitNrsPayload(employmentData, customerEmploymentSource, isUsingCustomerData = true)
        handleConnectorCall(user, taxYear, employmentId, customer)(result)
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

  private def sendAuditEvent(employmentData: AllEmploymentData, taxYear: Int, employmentSource: EmploymentSource, isUsingCustomerData: Boolean)
                            (implicit user: User[_], hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val employmentDetailsViewModel = employmentSource.toEmploymentDetailsViewModel(isUsingCustomerData = isUsingCustomerData)
    val benefits = employmentSource.employmentBenefits.flatMap(_.benefits)
    val employmentExpenses = if (isUsingCustomerData) employmentData.customerExpenses else employmentData.hmrcExpenses
    val expenses = employmentExpenses.flatMap(_.expenses)
    val deductions = employmentSource.employmentData.flatMap(_.deductions)
    val auditModel = DeleteEmploymentAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, employmentDetailsViewModel, benefits, expenses, deductions)

    auditService.sendAudit[DeleteEmploymentAudit](auditModel.toAuditModel)
  }

  def performSubmitNrsPayload(employmentData: AllEmploymentData, employmentSource: EmploymentSource,
                                      isUsingCustomerData: Boolean)
                                     (implicit user: User[_], request: Request[_], hc: HeaderCarrier): Future[NrsSubmissionResponse] = {

    val employmentDetailsViewModel = employmentSource.toEmploymentDetailsViewModel(isUsingCustomerData = isUsingCustomerData)
    val employmentExpenses = if (isUsingCustomerData) employmentData.customerExpenses else employmentData.hmrcExpenses
    val expenses = employmentExpenses.flatMap(_.expenses)
    val benefits = employmentSource.employmentBenefits.flatMap(_.benefits)
    val nrsPayload = DecodedDeleteEmploymentPayload(employmentDetailsViewModel, benefits, expenses)

    nrsService.submit(user.nino, nrsPayload.toNrsPayloadModel, user.mtditid)
  }

  private def handleConnectorCall(user: User[_], taxYear: Int, employmentId: String, toRemove: String)(result: Result)
                                 (implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {

    deleteOrIgnoreEmploymentConnector.deleteOrIgnoreEmployment(user.nino, taxYear, employmentId, toRemove)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
      case Left(error) => errorHandler.handleError(error.status)
      case Right(_) => result
    }
  }
}
