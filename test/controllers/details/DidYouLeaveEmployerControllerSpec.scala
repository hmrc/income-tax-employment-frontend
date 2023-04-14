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

package controllers.details

import controllers.details.routes.{EmployerEndDateController, PayeRefController}
import controllers.employment.routes.CheckEmploymentDetailsController
import forms.YesNoForm
import forms.details.EmploymentDetailsFormsProvider
import models.employment.EmploymentDetailsType
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.Helpers.{contentType, status}
import sttp.model.Method.POST
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks.{MockActionsProvider, MockEmploymentService, MockErrorHandler}
import views.html.details.DidYouLeaveEmployerView

class DidYouLeaveEmployerControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockEmploymentService
  with MockErrorHandler {

  private val employmentId: String = anEmploymentUserData.employmentId

  private val pageView = inject[DidYouLeaveEmployerView]

  private val underTest = new DidYouLeaveEmployerController(
    mockActionsProvider,
    pageView,
    new EmploymentDetailsFormsProvider(),
    mockEmploymentService,
    mockErrorHandler
  )

  ".show" should {
    "return a successful response" in {
      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData)

      val result = underTest.show(taxYearEOY, employmentId).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  ".submit" should {
    "render page with error when validation of form fails" in {
      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData)

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "")
      val result = underTest.submit(taxYearEOY, employmentId).apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
    }

    "handle internal server error when employmentService.updateDidYouLeaveQuestion(...) fails" in {
      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData)
      mockUpdateDidYouLeaveQuestion(aUser, taxYearEOY, employmentId, anEmploymentUserData, questionValue = true, Left(()))
      mockInternalServerError(InternalServerError)

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "true")
      val result = underTest.submit(taxYearEOY, employmentId)(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "save question and return correct result when employmentDetails is finished" in {
      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData)
      mockUpdateDidYouLeaveQuestion(aUser, taxYearEOY, employmentId, anEmploymentUserData, questionValue = true, Right(anEmploymentUserData))

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "true")

      await(underTest.submit(taxYearEOY, employmentId)(request)) shouldBe Redirect(CheckEmploymentDetailsController.show(taxYearEOY, employmentId))
    }

    "save question and return correct result when employmentDetails is not finished" when {
      "and question value is true and endDate is missing" in {
        val employmentDetails = anEmploymentDetails.copy(taxablePayToDate = None, didYouLeaveQuestion = Some(true))
        val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentDetails = employmentDetails))
        val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "true")

        mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData)
        mockUpdateDidYouLeaveQuestion(aUser, taxYearEOY, employmentId, anEmploymentUserData, questionValue = true, Right(employmentUserData))

        await(underTest.submit(taxYearEOY, employmentId)(request)) shouldBe Redirect(EmployerEndDateController.show(taxYearEOY, employmentId))
      }

      "and question value is false" in {
        val employmentDetails = anEmploymentDetails.copy(taxablePayToDate = None, didYouLeaveQuestion = Some(false))
        val employmentUserData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentDetails = employmentDetails))
        val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "false")

        mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData)
        mockUpdateDidYouLeaveQuestion(aUser, taxYearEOY, employmentId, anEmploymentUserData, questionValue = false, Right(employmentUserData))


        await(underTest.submit(taxYearEOY, employmentId)(request)) shouldBe Redirect(PayeRefController.show(taxYearEOY, employmentId))
      }
    }
  }
}
