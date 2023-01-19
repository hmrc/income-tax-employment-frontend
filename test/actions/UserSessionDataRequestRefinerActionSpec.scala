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

package actions

import controllers.employment.routes.{CheckEmploymentDetailsController, CheckYourBenefitsController}
import models.UserSessionDataRequest
import models.employment.{EmploymentBenefitsType, EmploymentDetailsType}
import models.mongo.DataNotFoundError
import play.api.mvc.Results.{InternalServerError, Redirect}
import support.UnitTest
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks.{MockAppConfig, MockEmploymentSessionService, MockErrorHandler}

import scala.concurrent.ExecutionContext

class UserSessionDataRequestRefinerActionSpec extends UnitTest
  with MockEmploymentSessionService
  with MockErrorHandler {

  private val taxYear = 2022
  private val employmentId = "some-employment-id"
  private val appConfig = new MockAppConfig().config()
  private val executionContext = ExecutionContext.global


  private val underTest = UserSessionDataRequestRefinerAction(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentDetailsType,
    employmentSessionService = mockEmploymentSessionService,
    errorHandler = mockErrorHandler,
    appConfig = appConfig
  )(executionContext)

  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".refine" should {
    "handle InternalServerError when when getting session data result in database error" in {
      mockGetSessionData(taxYear, employmentId, anAuthorisationRequest.user, Left(DataNotFoundError))
      mockInternalServerError(InternalServerError)

      await(underTest.refine(anAuthorisationRequest)) shouldBe Left(InternalServerError)
    }

    "return a redirect to Check Employment Details when session data is None and with EmploymentDetailsType" in {
      mockGetSessionData(taxYear, employmentId, anAuthorisationRequest.user, Right(None))

      val underTestAction = underTest.copy(employmentType = EmploymentDetailsType)(executionContext)

      await(underTestAction.refine(anAuthorisationRequest)) shouldBe Left(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
    }

    "return a redirect to Check Employment Details when session data is None and with EmploymentBenefitsType" in {
      mockGetSessionData(taxYear, employmentId, anAuthorisationRequest.user, Right(None))

      val underTestAction = underTest.copy(employmentType = EmploymentBenefitsType)(executionContext)

      await(underTestAction.refine(anAuthorisationRequest)) shouldBe Left(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
    }

    "return UserSessionDataRequest when period data exists" in {
      val employmentUserData = anEmploymentUserData.copy(employmentId = employmentId, taxYear = taxYear)

      mockGetSessionData(taxYear, employmentId, anAuthorisationRequest.user, Right(Some(employmentUserData)))

      await(underTest.refine(anAuthorisationRequest)) shouldBe Right(UserSessionDataRequest(employmentUserData, anAuthorisationRequest.user, anAuthorisationRequest.request))
    }
  }
}
