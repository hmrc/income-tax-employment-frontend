/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.{DeleteOrIgnoreEmploymentConnector, IncomeSourceConnector}
import models.employment.{AllEmploymentData, EmploymentSource}
import models.{APIErrorModel, User}
import play.api.Logging
import services.DeleteOrIgnoreExpensesService

import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveEmploymentService @Inject()(deleteOrIgnoreEmploymentConnector: DeleteOrIgnoreEmploymentConnector,
                                        incomeSourceConnector: IncomeSourceConnector,
                                        deleteOrIgnoreExpensesService: DeleteOrIgnoreExpensesService,
                                        auditService: AuditService,
                                        implicit val ec: ExecutionContext) extends Logging {


  def deleteOrIgnoreEmployment(employmentData: AllEmploymentData, taxYear: Int, employmentId: String, user: User)
                              (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Unit]] = {
    val hmrcDataSource = employmentData.hmrcEmploymentData.find(_.employmentId.equals(employmentId))
    val customerDataSource = employmentData.customerEmploymentData.find(_.employmentId.equals(employmentId))

    val eventualResult = (customerDataSource, hmrcDataSource) match {
      case (_, Some(hmrcEmploymentSource)) =>
        sendAuditEvent(taxYear, hmrcEmploymentSource.toEmploymentSource, isUsingCustomerData = false, user)
        handleConnectorCall(taxYear, employmentId, hmrcEmploymentSource.toRemove, user)
      case (Some(customerEmploymentSource), _) =>
        sendAuditEvent(taxYear, customerEmploymentSource, isUsingCustomerData = true, user)
        handleConnectorCall(taxYear, employmentId, customer, user)
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

  private def sendAuditEvent(taxYear: Int,
                             employmentSource: EmploymentSource, isUsingCustomerData: Boolean, user: User)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val employmentDetailsViewModel = employmentSource.toEmploymentDetailsViewModel(isUsingCustomerData = isUsingCustomerData)
    val benefits = employmentSource.employmentBenefits.flatMap(_.benefits)
    val deductions = employmentSource.employmentData.flatMap(_.deductions)
    val auditModel = DeleteEmploymentAudit(taxYear, user.affinityGroup.toLowerCase,
      user.nino, user.mtditid, employmentDetailsViewModel, benefits, deductions)

    auditService.sendAudit[DeleteEmploymentAudit](auditModel.toAuditModel)
  }

  private def handleConnectorCall(taxYear: Int, employmentId: String, toRemove: String, user: User)
                                 (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Unit]] = {

    deleteOrIgnoreEmploymentConnector.deleteOrIgnoreEmployment(user.nino, taxYear,
      employmentId, toRemove)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(_) => Future.successful(Right(()))
    }
  }
}
