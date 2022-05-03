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

import audit.UnignoreEmploymentAudit
import models.employment._
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.UserBuilder.aUser
import support.mocks._
import utils.UnitTest

class UnignoreEmploymentServiceSpec extends UnitTest
  with MockUnignoreEmploymentConnector
  with MockAuditService
  with MockNrsService {

  private val service: UnignoreEmploymentService = new UnignoreEmploymentService(
    mockUnignoreEmploymentConnector,
    mockAuditService,
    mockNrsService,
    mockExecutionContext
  )

  ".unignoreEmployment" should {
    "return a successful result" in {

      val unignoreEmploymentAudit = UnignoreEmploymentAudit(taxYear, "individual", nino, mtditid, "employmentId")

      mockAuditSendEvent(unignoreEmploymentAudit.toAuditModel)
      verifySubmitEvent(UnignoreEmploymentNRSModel("employmentId"))

      mockUnignoreEmployment(nino, taxYear, "employmentId", Right(()))

      await(service.unignoreEmployment(aUser, taxYear, "employmentId")) shouldBe Right()
    }

    "return a error result" in {

      val error = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)
      mockUnignoreEmployment(nino, taxYear, "employmentId", Left(error))

      await(service.unignoreEmployment(aUser, taxYear, "employmentId")) shouldBe Left(error)
    }
  }
}
