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

import audit._
import models.User
import models.employment._
import models.employment.createUpdate.CreateUpdateEmploymentRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentDetailsService @Inject()(auditService: AuditService) {

  def performSubmitAudits(user: User,
                          createUpdateEmploymentRequest: CreateUpdateEmploymentRequest,
                          employmentId: String,
                          taxYear: Int,
                          prior: Option[AllEmploymentData])
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {

    val audit: Either[AuditModel[AmendEmploymentDetailsUpdateAudit], AuditModel[CreateNewEmploymentDetailsAudit]] = prior.flatMap {
      prior =>
        val priorData = prior.eoyEmploymentSourceWith(employmentId)
        priorData.map(prior => createUpdateEmploymentRequest.toAmendAuditModel(user, employmentId, taxYear, prior.employmentSource).toAuditModel)
    }.map(Left(_)).getOrElse {

      val existingEmployments = prior.map(
        prior => prior.latestEOYEmployments.map(employment => PriorEmploymentAuditInfo(employment.employerName, employment.employerRef))
      ).getOrElse(Seq.empty)

      Right(createUpdateEmploymentRequest.toCreateAuditModel(user, taxYear, existingEmployments = existingEmployments).toAuditModel)
    }

    audit match {
      case Left(amend) => auditService.sendAudit(amend)
      case Right(create) => auditService.sendAudit(create)
    }
  }

  def sendViewEmploymentDetailsAudit(user: User,
                                     employmentDetails: EmploymentDetailsViewModel,
                                     taxYear: Int)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val auditModel = ViewEmploymentDetailsAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, employmentDetails)
    auditService.sendAudit[ViewEmploymentDetailsAudit](auditModel.toAuditModel)
  }
}
