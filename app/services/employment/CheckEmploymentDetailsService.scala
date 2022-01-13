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
import models.employment._
import models.employment.createUpdate.CreateUpdateEmploymentRequest
import play.api.mvc.Request
import services.{EmploymentSessionService, NrsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentDetailsService @Inject()(employmentSessionService: EmploymentSessionService,
                                              nrsService: NrsService,
                                              auditService: AuditService) {

  def performSubmitAudits(model: CreateUpdateEmploymentRequest, employmentId: String, taxYear: Int, prior: Option[AllEmploymentData])
                         (implicit user: User[_], hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {

    val audit: Either[AuditModel[AmendEmploymentDetailsUpdateAudit], AuditModel[CreateNewEmploymentDetailsAudit]] = prior.flatMap {
      prior =>
        val priorData = employmentSessionService.employmentSourceToUse(prior, employmentId, isInYear = false)
        priorData.map(prior => model.toAmendAuditModel(employmentId, taxYear, prior._1).toAuditModel)
    }.map(Left(_)).getOrElse {

      val existingEmployments = prior.map {
        prior =>
          employmentSessionService.getLatestEmploymentData(prior, isInYear = false).map {
            employment =>
              PriorEmploymentAuditInfo(employment.employerName, employment.employerRef)
          }
      }.getOrElse(Seq.empty)

      Right(model.toCreateAuditModel(taxYear, existingEmployments = existingEmployments).toAuditModel)
    }

    audit match {
      case Left(amend) => auditService.sendAudit(amend)
      case Right(create) => auditService.sendAudit(create)
    }
  }

  def performSubmitNrsPayload(model: CreateUpdateEmploymentRequest, employmentId: String, prior: Option[AllEmploymentData])
                             (implicit user: User[_], request: Request[_], hc: HeaderCarrier): Future[NrsSubmissionResponse] = {
    val nrsPayload: Either[DecodedAmendEmploymentDetailsPayload, DecodedCreateNewEmploymentDetailsPayload] = prior.flatMap {
      prior =>
        val priorData = employmentSessionService.employmentSourceToUse(prior, employmentId, isInYear = false)
        priorData.map(prior => model.toAmendDecodedPayloadModel(employmentId, prior._1))
    }.map(Left(_)).getOrElse {

      val existingEmployments = prior.map {
        prior =>
          employmentSessionService.getLatestEmploymentData(prior, isInYear = false).map {
            employment =>
              DecodedPriorEmploymentInfo(employment.employerName, employment.employerRef)
          }
      }.getOrElse(Seq.empty)

      Right(model.toCreateDecodedPayloadModel(existingEmployments))
    }

    nrsPayload match {
      case Left(amend) => nrsService.submit(user.nino, amend, user.mtditid)
      case Right(create) => nrsService.submit(user.nino, create, user.mtditid)
    }
  }

  def isSingleEmploymentAndAudit(employmentDetails: EmploymentDetailsViewModel,
                                 taxYear: Int,
                                 isInYear: Boolean,
                                 allEmploymentData: Option[AllEmploymentData])
                                (implicit user: User[_], hc: HeaderCarrier, ec: ExecutionContext): Boolean = {
    val auditModel = ViewEmploymentDetailsAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, employmentDetails)
    auditService.sendAudit[ViewEmploymentDetailsAudit](auditModel.toAuditModel)

    val employmentSource = allEmploymentData match {
      case Some(allEmploymentData) => employmentSessionService.getLatestEmploymentData(allEmploymentData, isInYear)
      case None => Seq[EmploymentSource]()
    }

    employmentSource.length <= 1
  }
}