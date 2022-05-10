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
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import connectors.{DeleteOrIgnoreEmploymentConnector, IncomeSourceConnector}
import javax.inject.Inject
import models.employment.{AllEmploymentData, DecodedDeleteEmploymentPayload, EmploymentSource}
import models.{APIErrorModel, AuthorisationRequest, User}
import play.api.Logging
import play.api.mvc.Request
import services.{DeleteOrIgnoreExpensesService, NrsService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class RemoveEmploymentService @Inject()(deleteOrIgnoreEmploymentConnector: DeleteOrIgnoreEmploymentConnector,
                                        incomeSourceConnector: IncomeSourceConnector,
                                        deleteOrIgnoreExpensesService: DeleteOrIgnoreExpensesService,
                                        auditService: AuditService,
                                        nrsService: NrsService,
                                        implicit val ec: ExecutionContext) extends Logging {


  def deleteOrIgnoreEmployment(employmentData: AllEmploymentData, taxYear: Int, employmentId: String, user: User)
                              (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Unit]] = {
    val hmrcDataSource = employmentData.hmrcEmploymentData.find(_.employmentId.equals(employmentId))
    val customerDataSource = employmentData.customerEmploymentData.find(_.employmentId.equals(employmentId))

    val eventualResult = (customerDataSource, hmrcDataSource) match {
      case (_, Some(hmrcEmploymentSource)) =>
        sendAuditEvent(employmentData, taxYear, hmrcEmploymentSource.toEmploymentSource, isUsingCustomerData = false, user)
        performSubmitNrsPayload(employmentData, hmrcEmploymentSource.toEmploymentSource, isUsingCustomerData = false, user)
        handleConnectorCall(taxYear, employmentId, hmrcEmploymentSource.toRemove, employmentData, employmentData.isLastEOYEmployment, user)
      case (Some(customerEmploymentSource), _) =>
        sendAuditEvent(employmentData, taxYear, customerEmploymentSource, isUsingCustomerData = true, user)
        performSubmitNrsPayload(employmentData, customerEmploymentSource, isUsingCustomerData = true, user)
        handleConnectorCall(taxYear, employmentId, customer, employmentData, employmentData.isLastEOYEmployment, user)
      case (None, None) =>
        logger.info(s"[RemoveEmploymentService][deleteOrIgnoreEmployment]" +
          s" No employment data found for user and employmentId. SessionId: ${user.sessionId}")
        Future(Right(()))
    }

    eventualResult.flatMap {
      case Left(error) => Future(Left(error))
      case Right(_) => incomeSourceConnector.put(taxYear, user.nino)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
        case Left(error) => Left(error)
        case _ => Right(())
      }
    }
  }

  private def sendAuditEvent(employmentData: AllEmploymentData, taxYear: Int,
                             employmentSource: EmploymentSource, isUsingCustomerData: Boolean, user: User)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val employmentDetailsViewModel = employmentSource.toEmploymentDetailsViewModel(isUsingCustomerData = isUsingCustomerData)
    val benefits = employmentSource.employmentBenefits.flatMap(_.benefits)
    val employmentExpenses = if (isUsingCustomerData) employmentData.customerExpenses else employmentData.notIgnoredHmrcExpenses
    val expenses = employmentExpenses.flatMap(_.expenses)
    val deductions = employmentSource.employmentData.flatMap(_.deductions)
    val auditModel = DeleteEmploymentAudit(taxYear, user.affinityGroup.toLowerCase,
      user.nino, user.mtditid, employmentDetailsViewModel, benefits, expenses, deductions)

    auditService.sendAudit[DeleteEmploymentAudit](auditModel.toAuditModel)
  }

  def performSubmitNrsPayload(employmentData: AllEmploymentData, employmentSource: EmploymentSource,
                              isUsingCustomerData: Boolean, user: User)
                             (implicit hc: HeaderCarrier): Future[NrsSubmissionResponse] = {

    val employmentDetailsViewModel = employmentSource.toEmploymentDetailsViewModel(isUsingCustomerData = isUsingCustomerData)
    val employmentExpenses = if (isUsingCustomerData) employmentData.customerExpenses else employmentData.notIgnoredHmrcExpenses
    val expenses = employmentExpenses.flatMap(_.expenses)
    val benefits = employmentSource.employmentBenefits.flatMap(_.benefits)
    val deductions = employmentSource.employmentData.flatMap(_.deductions)
    val nrsPayload = DecodedDeleteEmploymentPayload(employmentDetailsViewModel, benefits, expenses, deductions)

    nrsService.submit(user.nino, nrsPayload.toNrsPayloadModel, user.mtditid, user.trueUserAgent)
  }

  private def handleConnectorCall(taxYear: Int, employmentId: String, toRemove: String,
                                  allEmploymentData: AllEmploymentData, isLastEmployment: Boolean, user: User)
                                 (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Unit]] = {

    deleteOrIgnoreEmploymentConnector.deleteOrIgnoreEmployment(user.nino, taxYear,
      employmentId, toRemove)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(error) => Future(Left(error))
      case Right(_) => if(isLastEmployment) {
          deleteOrIgnoreExpensesService.deleteOrIgnoreExpenses(user, allEmploymentData, taxYear)
      } else {
        Future(Right(()))
      }
    }
  }
}
