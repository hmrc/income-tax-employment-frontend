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

package controllers.benefits.accommodation

import controllers.benefits.accommodation.routes.LivingAccommodationBenefitsController
import controllers.benefits.travel.routes.TravelOrEntertainmentBenefitsController
import forms.YesNoForm
import forms.benefits.accommodation.AccommodationFormsProvider
import models.employment.EmploymentBenefitsType
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{contentType, status}
import sttp.model.Method.POST
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks._
import support.{ControllerUnitTest, ViewHelper}
import utils.InYearUtil
import views.html.benefits.accommodation.AccommodationRelocationBenefitsView

class AccommodationRelocationBenefitsControllerSpec extends ControllerUnitTest with ViewHelper
  with MockAuthorisedAction
  with MockActionsProvider
  with MockAccommodationService
  with MockEmploymentSessionService
  with MockRedirectsService
  with MockErrorHandler {

  private val employmentId = "employmentId"
  private val pageView = inject[AccommodationRelocationBenefitsView]
  private val formsProvider = new AccommodationFormsProvider()

  private lazy val underTest = new AccommodationRelocationBenefitsController(
    mockAuthorisedAction,
    mockActionsProvider,
    inYearAction = new InYearUtil(),
    pageView: AccommodationRelocationBenefitsView,
    mockAccommodationService,
    mockEmploymentSessionService,
    mockRedirectsService,
    mockErrorHandler,
    formsProvider)

  ".show" should {
    "return successful response" in {
      mockEndOfYearWithSessionData(taxYearEOY, employmentId, EmploymentBenefitsType, controllerName = "AccommodationRelocationBenefitsController")

      val result = underTest.show(taxYearEOY, employmentId).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  ".submit" should {
    "render page with error when validation of form fails" in {
      mockEndOfYearWithSessionData(taxYearEOY, employmentId, EmploymentBenefitsType, controllerName = "AccommodationRelocationBenefitsController")

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "")
      val result = underTest.submit(taxYearEOY, employmentId).apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
    }

    "handle internal server error when save operation fails with database error" in {
      mockEndOfYearWithSessionData(taxYearEOY, employmentId, EmploymentBenefitsType, controllerName = "AccommodationRelocationBenefitsController")
      mockSaveSectionQuestion(aUser, taxYearEOY, employmentId, anEmploymentUserData, questionValue = true, Left(()))
      mockInternalServerError(InternalServerError)

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "true")
      val result = underTest.submit(taxYearEOY, employmentId)(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "Should save section question and return correct result when question value is true" in {
      val result: Result = mock[Result]

      mockEndOfYearWithSessionData(taxYearEOY, employmentId, EmploymentBenefitsType, controllerName = "AccommodationRelocationBenefitsController")
      mockSaveSectionQuestion(aUser, taxYearEOY, employmentId, anEmploymentUserData, questionValue = true, Right(anEmploymentUserData))
      mockBenefitsSubmitRedirect(taxYearEOY, employmentId, anEmploymentCYAModel, LivingAccommodationBenefitsController.show(taxYearEOY, employmentId), result)

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "true")

      await(underTest.submit(taxYearEOY, employmentId)(request)) shouldBe result
    }

    "Should save section question and return correct result when question value is false" in {
      val result: Result = mock[Result]

      mockEndOfYearWithSessionData(taxYearEOY, employmentId, EmploymentBenefitsType, controllerName = "AccommodationRelocationBenefitsController")
      mockSaveSectionQuestion(aUser, taxYearEOY, employmentId, anEmploymentUserData, questionValue = false, Right(anEmploymentUserData))
      mockBenefitsSubmitRedirect(taxYearEOY, employmentId, anEmploymentCYAModel, TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId), result)

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "false")

      await(underTest.submit(taxYearEOY, employmentId)(request)) shouldBe result
    }
  }
}

