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

package services.expenses

import audit._
import models.User
import models.employment._
import models.expenses.Expenses
import models.requests.CreateUpdateExpensesRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentExpensesService @Inject()(auditService: AuditService) {

  def performSubmitAudits(user: User, createUpdateExpensesRequest: CreateUpdateExpensesRequest, taxYear: Int, prior: Option[AllEmploymentData])
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val audit = prior
      .flatMap(prior => prior.latestEOYExpenses.map(prior => createUpdateExpensesRequest.toAmendAuditModel(user, taxYear, prior.latestExpenses).toAuditModel))
      .map(Left(_))
      .getOrElse(Right(createUpdateExpensesRequest.toCreateAuditModel(user, taxYear).toAuditModel))

    audit match {
      case Left(amend) => auditService.sendAudit(amend)
      case Right(create) => auditService.sendAudit(create)
    }
  }

  def sendViewEmploymentExpensesAudit(user: User, taxYear: Int, expenses: Expenses)
                                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val auditModel = ViewEmploymentExpensesAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, expenses)
    auditService.sendAudit[ViewEmploymentExpensesAudit](auditModel.toAuditModel)
  }
}
