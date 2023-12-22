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

import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.builders.models.otheremployment.session.OtherEmploymentIncomeCYAModelBuilder.anOtherEmploymentIncomeCYAModel
import support.mocks.{MockAuditService, MockEmploymentSessionService, MockNrsService, MockOtherEmploymentInfoService}
import support.{TaxYearProvider, UnitTest}

import scala.concurrent.ExecutionContext

class OtherEmploymentInfoServiceSpec extends UnitTest
  with TaxYearProvider
  with MockEmploymentSessionService
  with MockNrsService
  with MockAuditService
  with MockOtherEmploymentInfoService {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val employmentId = "some-employment-id"

  private val underTest = new OtherEmploymentInfoService(mockEmploymentSessionService, ec)

  "updateLumpSum" should {
    "update the existing lump sum" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val givenEmploymentUserDataWithLumpSum = givenEmploymentUserData.copy(employment = anEmploymentCYAModel(otherEmploymentIncome = Some(anOtherEmploymentIncomeCYAModel)))

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, givenEmploymentUserDataWithLumpSum.employment, Right(givenEmploymentUserDataWithLumpSum))

      await(underTest.updateLumpSums(aUser, taxYearEOY, employmentId, givenEmploymentUserData, anOtherEmploymentIncomeCYAModel.taxableLumpSums))shouldBe Right(givenEmploymentUserDataWithLumpSum)
    }
  }
}
