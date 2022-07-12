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
import controllers.benefits.fuel.routes.CarVanFuelBenefitsController
import controllers.errors.routes.UnauthorisedUserErrorController
import models.employment.EmploymentDetailsType
import models.redirects.ConditionalRedirect
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks.{MockAuthorisedAction, MockEmploymentSessionService, MockErrorHandler, MockRedirectsMatcherUtils}
import utils.InYearUtil

class ActionsProviderSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockRedirectsMatcherUtils
  with MockErrorHandler {

  private val employmentId = "employmentId"

  private def fakeIndividualRequest(taxYear: Int): FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders(newHeaders = "X-Session-ID" -> aUser.sessionId)

  private val anyBlock = (_: Request[AnyContent]) => Ok("any-result")

  private val actionsProvider = new ActionsProvider(
    mockAuthorisedAction,
    mockEmploymentSessionService,
    mockErrorHandler,
    new InYearUtil,
    mockRedirectsMatcherUtils,
    appConfig
  )

  ".endOfYearWithSessionData" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, employmentId, EmploymentDetailsType, controllerName = "some-controller-name")(anyBlock)

      await(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe Redirect(UnauthorisedUserErrorController.show)
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.endOfYearWithSessionData(taxYear, employmentId, EmploymentDetailsType, controllerName = "some-controller-name")(anyBlock)

      await(underTest(fakeIndividualRequest(taxYear))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return relevant redirect for missing data" in {
      val resultRedirects: Seq[ConditionalRedirect] = Seq(ConditionalRedirect(CarVanFuelBenefitsController.show(taxYearEOY, employmentId)))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, employmentId, aUser, result = Right(Some(anEmploymentUserData)))
      mockMatchToRedirects(controllerName = "some-controller-name", taxYearEOY, employmentId, anEmploymentCYAModel, resultRedirects)

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, employmentId, EmploymentDetailsType, controllerName = "some-controller-name")(anyBlock)

      await(underTest(fakeIndividualRequest(taxYearEOY))) shouldBe Redirect(CarVanFuelBenefitsController.show(taxYearEOY, employmentId))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, employmentId, aUser, result = Right(Some(anEmploymentUserData)))
      mockMatchToRedirects(controllerName = "some-controller-name", taxYearEOY, employmentId, anEmploymentCYAModel, Seq.empty)

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, employmentId, EmploymentDetailsType, controllerName = "some-controller-name")(anyBlock)

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
}