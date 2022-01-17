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

import audit._
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import models.User
import models.benefits.{Benefits, DecodedAmendBenefitsPayload, DecodedCreateNewBenefitsPayload}
import models.employment.AllEmploymentData
import models.employment.createUpdate.CreateUpdateEmploymentRequest
import play.api.mvc.Request
import services.{EmploymentSessionService, NrsService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourBenefitsService @Inject()(employmentSessionService: EmploymentSessionService,
                                         auditService: AuditService,
                                         nrsService: NrsService) {

  def isSingleEmploymentAndAudit(benefits: Benefits, taxYear: Int, isInYear: Boolean, allEmploymentData: AllEmploymentData)
                                (implicit user: User[_], hc: HeaderCarrier, ec: ExecutionContext): Boolean = {
    val auditModel = ViewEmploymentBenefitsAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, benefits)
    auditService.sendAudit[ViewEmploymentBenefitsAudit](auditModel.toAuditModel)

    val employmentSource = employmentSessionService.getLatestEmploymentData(allEmploymentData, isInYear)
    employmentSource.length == 1
  }

  def performSubmitNrsPayload(model: CreateUpdateEmploymentRequest, employmentId: String, prior: Option[AllEmploymentData])
                             (implicit user: User[_], request: Request[_], hc: HeaderCarrier): Future[NrsSubmissionResponse] = {

    val nrsPayload: Either[DecodedAmendBenefitsPayload, DecodedCreateNewBenefitsPayload] = prior.flatMap {
      prior =>
        val priorData = employmentSessionService.employmentSourceToUse(prior, employmentId, isInYear = false)
        priorData.map(prior => model.toAmendDecodedBenefitsPayloadModel(prior._1))
    }.map(Left(_)).getOrElse(Right(model.toCreateDecodedBenefitsPayloadModel()))

    nrsPayload match {
      case Left(amend) => nrsService.submit(user.nino, amend, user.mtditid)
      case Right(create) => nrsService.submit(user.nino, create, user.mtditid)
    }

  }
}
