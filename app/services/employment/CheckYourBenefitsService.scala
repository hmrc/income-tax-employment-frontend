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
import models.benefits.{DecodedAmendBenefitsPayload}
import models.employment.AllEmploymentData
import models.employment.createUpdate.CreateUpdateEmploymentRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourBenefitsService @Inject()(auditService: AuditService) {
  def performSubmitAudits(user: User,
                          createUpdateEmploymentRequest: CreateUpdateEmploymentRequest,
                          employmentId: String,
                          taxYear: Int,
                          prior: Option[AllEmploymentData])
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Option[Future[AuditResult]] = {

    prior.flatMap {
      prior =>
        val priorData = prior.eoyEmploymentSourceWith(employmentId)
        priorData.flatMap {
          prior => {
            val benefitsData: DecodedAmendBenefitsPayload = createUpdateEmploymentRequest.toAmendDecodedBenefitsPayloadModel(prior.employmentSource)
            if (benefitsData.priorEmploymentBenefitsData.hasBenefitsPopulated) {
              val amendEvent = AmendEmploymentBenefitsUpdateAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid,
                benefitsData.priorEmploymentBenefitsData, benefitsData.employmentBenefitsData
              ).toAuditModel
              Some(auditService.sendAudit(amendEvent))
            } else {
              val data = createUpdateEmploymentRequest.toCreateDecodedBenefitsPayloadModel().employmentBenefitsData
              if (data.hasBenefitsPopulated) {
                val createEvent = CreateNewEmploymentBenefitsAudit(
                  taxYear = taxYear,
                  userType = user.affinityGroup.toLowerCase,
                  nino = user.nino,
                  mtditid = user.mtditid,
                  employerName = prior.employmentSource.employerName,
                  employerRef = prior.employmentSource.employerRef,
                  employmentBenefitsData = data
                ).toAuditModel
                Some(auditService.sendAudit(createEvent))
              } else {
                None
              }
            }
          }
        }
    }
  }
}
