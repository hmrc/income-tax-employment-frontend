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

package actions

import common.SessionValues
import controllers.errors.routes.UnauthorisedUserErrorController
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks.{MockAppConfig, MockAuthorisedAction, MockEmploymentSessionService, MockErrorHandler}
import utils.InYearUtil

class ActionsProviderSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockErrorHandler {

  private val employmentId = "employmentId"

  private def fakeIndividualRequest(taxYear: Int): FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> aUser.sessionId)

  private val anyBlock = (_: Request[AnyContent]) => Ok("any-result")

  private val actionsProvider = new ActionsProvider(
    mockAuthorisedAction,
    mockEmploymentSessionService,
    mockErrorHandler,
    new InYearUtil
  )

  ".inYear" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.inYear(taxYear)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYear))) shouldBe Redirect(UnauthorisedUserErrorController.show)
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.inYear(taxYearEOY)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.inYear(taxYear)(anyBlock)

      status(underTest(fakeIndividualRequest(taxYear))) shouldBe OK
    }
  }

  ".notInYear" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.notInYear(taxYearEOY)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYear))) shouldBe Redirect(UnauthorisedUserErrorController.show)
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.notInYear(taxYear)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYear))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.notInYear(taxYearEOY)(anyBlock)

      status(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe OK
    }
  }

  ".notInYearWithPriorData" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.notInYearWithPriorData(taxYearEOY)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe Redirect(UnauthorisedUserErrorController.show)
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.notInYearWithPriorData(taxYear)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYear))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorLeft(taxYearEOY)
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.notInYearWithPriorData(taxYearEOY)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when data is None" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorRight(taxYearEOY, None)

      val underTest = actionsProvider.notInYearWithPriorData(taxYearEOY)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData))

      val underTest = actionsProvider.notInYearWithPriorData(taxYearEOY)(anyBlock)

      status(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe OK
    }
  }

  ".notInYearWithSessionData" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.notInYearWithSessionData(taxYearEOY, employmentId)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe Redirect(UnauthorisedUserErrorController.show)
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.notInYearWithSessionData(taxYear, employmentId)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYear))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "redirect to Income Tax Submission Overview when EOY and employmentEOYEnabled is false" in {
      val actionsProvider = new ActionsProvider(
        mockAuthorisedAction,
        mockEmploymentSessionService,
        mockErrorHandler,
        new InYearUtil,
      )(ec ,new MockAppConfig().config(isEmploymentEOYEnabled = false))

      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.notInYearWithSessionData(taxYearEOY, employmentId)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "handle internal server error when getting session data result in database error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, employmentId, Left(InternalServerError))
      mockInternalServerError

      val underTest = actionsProvider.notInYearWithSessionData(taxYearEOY, employmentId)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when session data is None" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, employmentId, Right(None))

      val underTest = actionsProvider.notInYearWithSessionData(taxYearEOY, employmentId)(anyBlock)

      await(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, employmentId, result = Right(Some(anEmploymentUserData)))

      val underTest = actionsProvider.notInYearWithSessionData(taxYearEOY, employmentId)(anyBlock)

      status(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe OK
    }
  }
}