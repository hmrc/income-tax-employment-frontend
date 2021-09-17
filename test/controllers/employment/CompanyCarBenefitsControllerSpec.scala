/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.employment

import config.MockEmploymentSessionService
import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.employment.EmploymentSource
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, Ok, Redirect}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import utils.UnitTestWithApp
import views.html.employment.CompanyCarBenefitsView

import scala.concurrent.Future

class CompanyCarBenefitsControllerSpec extends UnitTestWithApp with MockEmploymentSessionService {

  private val taxYear = 2021
  private val employmentId = "223/AB12399"
  private lazy val view = app.injector.instanceOf[CompanyCarBenefitsView]
  private val form = YesNoForm.yesNoForm(
    missingInputError = "CompanyCarBenefits.error"
  )
  private lazy val employmentsCYAFill: EmploymentSource = employmentsModel.hmrcEmploymentData.head

  private lazy val employmentUserData = new EmploymentUserData(
    sessionId,
    mtditid,
    nino,
    taxYear,
    employmentId,
    false,
    EmploymentCYAModel(employmentsCYAFill, isUsingCustomerData = false)
  )

  private lazy val employmentUserDataWithoutBenefits = new EmploymentUserData(
    sessionId,
    mtditid,
    nino,
    taxYear,
    employmentId,
    false,
    EmploymentCYAModel(employmentsCYAFill.copy(employmentBenefits = None), isUsingCustomerData = false)
  )

  private lazy val controller = new CompanyCarBenefitsController()(
    mockMessagesControllerComponents,
    authorisedAction,
    inYearAction,
    view,
    mockAppConfig,
    mockIncomeTaxUserDataService,
    mockErrorHandler,
    testClock)

  ".show" should {
    "get user session data and return the result from the given execution block" in new TestWithAuth {
      val anyResult = Ok
      val result: Future[Result] = {
        mockGetSessionData(taxYear, employmentId, anyResult)
        controller.show(taxYear, employmentId)(fakeRequest)
      }

      status(result) shouldBe anyResult.header.status
    }
  }

  ".handleShow" should {
    "return Redirect result income tax submission overview" when {
      "when employment user data not present" in {
        await(controller.handleShow(taxYear, employmentId, None)) shouldBe Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
      }
    }

    "render page" when {
      "with empty form when no benefits" in {
        val result = controller.handleShow(taxYear, employmentId, Some(employmentUserDataWithoutBenefits))

        status(result) shouldBe OK
        contentAsString(result) shouldNot include("checked")
      }

      "with non empty form when there are benefits" in {
        val result = controller.handleShow(taxYear, employmentId, Some(employmentUserData))

        status(result) shouldBe OK
        contentAsString(result) should include("checked")
      }
    }
  }

  ".submit" should {
    "return a result" which {
      "has a SEE_OTHER status with valid form body true" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, InternalServerError("500"))
          controller.submit(taxYear, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "true"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "has a SEE_OTHER status with valid form body false" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, InternalServerError("500"))
          controller.submit(taxYear, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "false"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "has a SEE_OTHER status with valid form body true but no carVanFuel benefits in session" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, Redirect("/any-url"))
          controller.submit(taxYear, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "false"))
        }
        status(result) shouldBe SEE_OTHER
      }

      "has a SEE_OTHER status with valid form body but no session" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, Redirect("/any-url"))

          controller.submit(taxYear, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "false"))
        }
        status(result) shouldBe SEE_OTHER
      }

      "has a BAD_REQUEST status with invalid form body" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, BadRequest)
          controller.submit(taxYear, employmentId)(fakeRequest)
        }
        status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
