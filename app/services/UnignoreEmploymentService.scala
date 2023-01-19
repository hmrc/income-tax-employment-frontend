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

package services

import audit.{AuditService, UnignoreEmploymentAudit}
import connectors.UnignoreEmploymentConnector
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import models.employment.{EmploymentSource, UnignoreEmploymentNRSModel}
import models.{APIErrorModel, User}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UnignoreEmploymentService @Inject()(unignoreEmploymentConnector: UnignoreEmploymentConnector,
                                          auditService: AuditService,
                                          nrsService: NrsService,
                                          implicit val executionContext: ExecutionContext) extends Logging {

  def unignoreEmployment(user: User, taxYear: Int, hmrcEmploymentSource: EmploymentSource)
                        (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Unit]] = {
    unignoreEmploymentConnector.unignoreEmployment(user.nino, taxYear, hmrcEmploymentSource.employmentId)(
      hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
      case Left(error) => Left(error)
      case _ =>
        sendAuditEvent(user, taxYear, hmrcEmploymentSource)
        performSubmitNrsPayload(user, hmrcEmploymentSource)
        Right(())
    }
  }

  private def sendAuditEvent(user: User, taxYear: Int, hmrcEmploymentSource: EmploymentSource)
                            (implicit hc: HeaderCarrier): Unit = {
    val employmentDetailsViewModel = hmrcEmploymentSource.toEmploymentDetailsViewModel(isUsingCustomerData = false)
    val benefits = hmrcEmploymentSource.employmentBenefits.flatMap(_.benefits)
    val deductions = hmrcEmploymentSource.employmentData.flatMap(_.deductions)

    val auditModel = UnignoreEmploymentAudit(
      taxYear,
      user.affinityGroup.toLowerCase,
      user.nino,
      user.mtditid,
      employmentDetailsViewModel,
      benefits,
      deductions
    )

    auditService.sendAudit[UnignoreEmploymentAudit](auditModel.toAuditModel)
  }

  private def performSubmitNrsPayload(user: User, hmrcEmploymentSource: EmploymentSource)
                                     (implicit hc: HeaderCarrier): Future[NrsSubmissionResponse] = {
    val employmentData = hmrcEmploymentSource.toEmploymentDetailsViewModel(isUsingCustomerData = false)
    val benefits = hmrcEmploymentSource.employmentBenefits.flatMap(_.benefits)
    val deductions = hmrcEmploymentSource.employmentData.flatMap(_.deductions)
    val nrsPayload = UnignoreEmploymentNRSModel(employmentData, benefits, deductions)

    nrsService.submit(user.nino, nrsPayload, user.mtditid, user.trueUserAgent)
  }
}
