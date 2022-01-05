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

package services.expenses

import audit._
import models.User
import models.employment._
import models.expenses.Expenses
import models.requests.CreateUpdateExpensesRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.EmploymentExpensesUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentExpensesService @Inject()(auditService: AuditService) {

  def performSubmitAudits(model: CreateUpdateExpensesRequest, taxYear: Int, prior: Option[AllEmploymentData])
                         (implicit user: User[_], hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {

    val audit: Either[AuditModel[AmendEmploymentExpensesUpdateAudit], AuditModel[CreateNewEmploymentExpensesAudit]] =
      prior.flatMap {
        prior =>
          val priorData = EmploymentExpensesUtils.getLatestExpenses(prior, isInYear = false)
          priorData.map(prior => model.toAmendAuditModel(taxYear, prior._1).toAuditModel)
      }.map(Left(_)).getOrElse {
        Right(model.toCreateAuditModel(taxYear).toAuditModel)
      }
    audit match {
      case Left(amend) => auditService.sendAudit(amend)
      case Right(create) => auditService.sendAudit(create)
    }
  }

  def sendViewEmploymentExpensesAudit(taxYear: Int, expenses: Expenses)
                                     (implicit user: User[_], hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val auditModel = ViewEmploymentExpensesAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, expenses)
    auditService.sendAudit[ViewEmploymentExpensesAudit](auditModel.toAuditModel)
  }
}
