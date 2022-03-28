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

import audit.{AuditService, UnignoreEmploymentAudit}
import connectors.UnignoreEmploymentConnector
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import javax.inject.Inject
import models.employment.UnignoreEmploymentNRSModel
import models.{APIErrorModel, CommonAuthorisationRequest, User}
import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import utils.RequestUtils.getTrueUserAgent

import scala.concurrent.{ExecutionContext, Future}

class UnignoreEmploymentService @Inject()(unignoreEmploymentConnector: UnignoreEmploymentConnector,
                                          auditService: AuditService,
                                          nrsService: NrsService,
                                          implicit val executionContext: ExecutionContext) extends Logging {

  def unignoreEmployment(user: User, taxYear: Int, employmentId: String)
                        (implicit authorisationRequest: CommonAuthorisationRequest, hc: HeaderCarrier): Future[Either[APIErrorModel, Unit]] = {

    unignoreEmploymentConnector.unignoreEmployment(authorisationRequest.user.nino, taxYear, employmentId)(
      hc.withExtraHeaders("mtditid" -> authorisationRequest.user.mtditid)).map {
      case Left(error) => Left(error)
      case _ =>
        sendAuditEvent(user, taxYear, employmentId)
        performSubmitNrsPayload(user, employmentId)(authorisationRequest.request, hc)
        Right()
    }
  }

  private def sendAuditEvent(user: User, taxYear: Int, employmentId: String)
                            (implicit hc: HeaderCarrier): Unit = {

    val auditModel = UnignoreEmploymentAudit(
      taxYear,
      user.affinityGroup.toLowerCase,
      user.nino,
      user.mtditid,
      employmentId
    )

    auditService.sendAudit[UnignoreEmploymentAudit](auditModel.toAuditModel)
  }

  private def performSubmitNrsPayload(user: User, employmentId: String)
                                     (implicit request: Request[_], hc: HeaderCarrier): Future[NrsSubmissionResponse] = {
    nrsService.submit(user.nino, UnignoreEmploymentNRSModel(employmentId), user.mtditid, getTrueUserAgent)
  }
}
