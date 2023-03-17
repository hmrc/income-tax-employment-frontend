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

import controllers.details.routes.EmployerPayrollIdController
import controllers.employment.routes.CheckEmploymentDetailsController
import forms.details.{DateForm, EmploymentDetailsFormsProvider}
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
import views.html.details.EmployerEndDateView

import java.time.LocalDate

class EmployerEndDateControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockEmploymentService
  with MockErrorHandler {

  private val employmentId: String = anEmploymentUserData.employmentId
  private val startDate = LocalDate.of(taxYearEOY, 1, 1)
  private val pageView = inject[EmployerEndDateView]

  private val underTest = new EmployerEndDateController(
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

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(DateForm.day -> "")
      val result = underTest.submit(taxYearEOY, employmentId).apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
    }

    "handle internal server error when EmploymentService.updateEndDate(...) fails" in {
      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData)
      mockUpdateEndDate(aUser, taxYearEOY, employmentId, anEmploymentUserData, startDate, Left(()))
      mockInternalServerError(InternalServerError)

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(
        DateForm.day -> "1",
        DateForm.month -> "1",
        DateForm.year -> taxYearEOY.toString,
      )
      val result = underTest.submit(taxYearEOY, employmentId)(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to CheckEmploymentDetails page on successful end date update when isFinished" in {
      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData)
      mockUpdateEndDate(aUser, taxYearEOY, employmentId, anEmploymentUserData, startDate, Right(anEmploymentUserData))

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(
        DateForm.day -> "1",
        DateForm.month -> "1",
        DateForm.year -> taxYearEOY.toString,
      )

      await(underTest.submit(taxYearEOY, employmentId).apply(request)) shouldBe
        Redirect(CheckEmploymentDetailsController.show(taxYearEOY, employmentId))
    }

    "redirect to CheckEmploymentDetails page on successful end date update when not finished" in {
      val userData = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentDetails = anEmploymentDetails.copy(totalTaxToDate = None)))

      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, userData)
      mockUpdateEndDate(aUser, taxYearEOY, employmentId, userData, startDate, Right(userData))

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(
        DateForm.day -> "1",
        DateForm.month -> "1",
        DateForm.year -> taxYearEOY.toString
      )

      await(underTest.submit(taxYearEOY, employmentId).apply(request)) shouldBe
        Redirect(EmployerPayrollIdController.show(taxYearEOY, employmentId))
    }
  }
}
