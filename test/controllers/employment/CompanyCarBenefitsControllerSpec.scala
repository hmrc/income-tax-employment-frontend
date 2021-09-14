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
import controllers.employment.routes.CompanyCarBenefitsController
import forms.YesNoForm
import models.employment.EmploymentBenefits
import models.{User, mongo}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.Results.{InternalServerError, Ok, Redirect, ServiceUnavailable}
import play.api.mvc.{Action, AnyContent, Request, Result}
import utils.{Clock, UnitTestWithApp}
import views.html.employment.CompanyCarBenefitsView

import scala.concurrent.Future

class CompanyCarBenefitsControllerSpec extends UnitTestWithApp with MockEmploymentSessionService {

  val taxYear = 2021
  val employmentId = "223/AB12399"
  lazy val view = app.injector.instanceOf[CompanyCarBenefitsView]
  val form = YesNoForm.yesNoForm(
    missingInputError = "CompanyCarBenefits.error"
  )
  lazy val employmentsCYAFill = employmentsModel.hmrcEmploymentData.head

  lazy val employmentUserData = new EmploymentUserData(
    sessionId,
    mtditid,
    nino,
    taxYear,
    employmentId,
    false,
    EmploymentCYAModel(employmentsCYAFill, false)
  )

  lazy val employmentUserDataNoCarVanFuel = new EmploymentUserData(
    sessionId,
    mtditid,
    nino,
    taxYear,
    employmentId,
    false,
    EmploymentCYAModel(employmentsCYAFill.copy(employmentBenefits = None), false)
  )

  lazy val controller = new CompanyCarBenefitsController()(
    mockMessagesControllerComponents,
    authorisedAction,
    inYearAction,
    view,
    mockAppConfig,
    mockIncomeTaxUserDataService,
    mockErrorHandler,
    testClock)

  ".show" should {

    "return a result" which {

      "has an Ok Status" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, Some(employmentUserData))
          controller.show(taxYear, employmentId)(fakeRequest)
        }
        status(result) shouldBe OK
      }

      "have a redirect Status when no session Data" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, None)
          controller.show(taxYear, employmentId)(fakeRequest)
        }
        status(result) shouldBe SEE_OTHER
      }
    }
  }

  ".submit" should {

    "return a result" which {

      "has a SEE_OTHER status with valid form body true" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, Some(employmentUserData))
          (mockErrorHandler.internalServerError()(_: User[_])).expects(*).returns(InternalServerError("500"))

          val redirect = CompanyCarBenefitsController.show(taxYear, employmentId).url

          (mockIncomeTaxUserDataService.createOrUpdateSessionData(_: String, _: EmploymentCYAModel, _: Int, _: Boolean)
          (_: Result)(_: Result)(_: User[_], _: Clock)).expects(*, *, *, *, *, *, *, *).returns(Future(Redirect(redirect, SEE_OTHER)))

          controller.submit(taxYear, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "true"))
        }
        status(result) shouldBe SEE_OTHER
      }

      "has a SEE_OTHER status with valid form body false" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, Some(employmentUserData))
          (mockErrorHandler.internalServerError()(_: User[_])).expects(*).returns(InternalServerError("500"))

          val redirect = CompanyCarBenefitsController.show(taxYear, employmentId).url

          (mockIncomeTaxUserDataService.createOrUpdateSessionData(_: String, _: EmploymentCYAModel, _: Int, _: Boolean)
          (_: Result)(_: Result)(_: User[_], _: Clock)).expects(*, *, *, *, *, *, *, *).returns(Future(Redirect(redirect, SEE_OTHER)))

          controller.submit(taxYear, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "false"))
        }
        status(result) shouldBe SEE_OTHER
      }

      "has a SEE_OTHER status with valid form body true but no carVanFuel benefits in session" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, Some(employmentUserDataNoCarVanFuel))
          val redirect = CompanyCarBenefitsController.show(taxYear, employmentId).url

          controller.submit(taxYear, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "false"))
        }
        status(result) shouldBe SEE_OTHER
      }

      "has a SEE_OTHER status with valid form body but no session" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, None)

          controller.submit(taxYear, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "false"))
        }
        status(result) shouldBe SEE_OTHER
      }

      "has a BAD_REQUEST status with invalid form body" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetSessionData(taxYear, employmentId, Some(employmentUserData))
          controller.submit(taxYear, employmentId)(fakeRequest)
        }
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

}
