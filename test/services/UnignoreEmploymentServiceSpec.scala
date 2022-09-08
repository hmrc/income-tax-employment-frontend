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
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.UserBuilder.aUser
import support.builders.models.benefits.BenefitsBuilder.aBenefits
import support.builders.models.employment.DeductionsBuilder.aDeductions
import support.builders.models.employment.EmploymentDetailsViewModelBuilder.anEmploymentDetailsViewModel
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.builders.models.employment.UnignoreEmploymentNRSModelBuilder.anUnignoreEmploymentNRSModel
import support.mocks._
import utils.UnitTest

class UnignoreEmploymentServiceSpec extends UnitTest
  with MockUnignoreEmploymentConnector
  with MockAuditService
  with MockNrsService {

  private val underTest = new UnignoreEmploymentService(
    mockUnignoreEmploymentConnector,
    mockAuditService,
    mockNrsService,
    mockExecutionContext
  )

  ".unignoreEmployment" should {
    "return a successful result" in {
      val unignoreEmploymentAudit = UnignoreEmploymentAudit(taxYear, "individual", aUser.nino, aUser.mtditid, anEmploymentDetailsViewModel, Some(aBenefits), Some(aDeductions))

      mockAuditSendEvent(unignoreEmploymentAudit.toAuditModel)
      mockUnignoreEmployment(aUser.nino, taxYear, anEmploymentSource.employmentId, Right(()))
      verifySubmitEvent(anUnignoreEmploymentNRSModel)

      await(underTest.unignoreEmployment(aUser, taxYear, anEmploymentSource)) shouldBe Right()
    }

    "return a error result" in {
      val error = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      mockUnignoreEmployment(aUser.nino, taxYear, anEmploymentSource.employmentId, Left(error))

      await(underTest.unignoreEmployment(aUser, taxYear, anEmploymentSource)) shouldBe Left(error)
    }
  }
}
