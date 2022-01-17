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

import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import models.User
import models.benefits.{DecodedAmendBenefitsPayload, DecodedCreateNewBenefitsPayload}
import models.employment.AllEmploymentData
import models.employment.createUpdate.CreateUpdateEmploymentRequest
import play.api.mvc.Request
import services.NrsService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class CheckYourBenefitsService @Inject()(nrsService: NrsService) {

  def performSubmitNrsPayload(model: CreateUpdateEmploymentRequest, employmentId: String, prior: Option[AllEmploymentData])
                             (implicit user: User[_], request: Request[_], hc: HeaderCarrier): Future[NrsSubmissionResponse] = {

    val nrsPayload: Either[DecodedAmendBenefitsPayload, DecodedCreateNewBenefitsPayload] = prior.flatMap {
      prior =>
        val priorData = prior.eoyEmploymentSourceWith(employmentId)
        priorData.map(prior => model.toAmendDecodedBenefitsPayloadModel(prior.employmentSource))
    }.map(Left(_)).getOrElse(Right(model.toCreateDecodedBenefitsPayloadModel()))

    nrsPayload match {
      case Left(amend) => nrsService.submit(user.nino, amend, user.mtditid)
      case Right(create) => nrsService.submit(user.nino, create, user.mtditid)
    }
  }
}
