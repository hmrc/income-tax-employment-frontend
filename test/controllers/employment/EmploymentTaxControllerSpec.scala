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

import common.SessionValues
import config.MockEmploymentSessionService
import controllers.employment.routes.CheckEmploymentDetailsController
import models.User
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import utils.{Clock, UnitTestWithApp}
import views.html.employment.EmploymentTaxView

import scala.concurrent.{ExecutionContext, Future}

class EmploymentTaxControllerSpec extends UnitTestWithApp with MockEmploymentSessionService {

  object Model {

    val employmentSource1 = EmploymentDetails(
      "Mishima Zaibatsu",
      employerRef = Some("223/AB12399"),
      startDate = Some("2019-04-21"),
      currentDataIsHmrcHeld = true
    )
    val employmentCyaModel = EmploymentCYAModel(employmentSource1)
    val employmentUserData = EmploymentUserData(sessionId, mtditid, nino, taxYear, employmentId, false, employmentCyaModel)
  }

  val taxYear = 2021
  val employmentId = "223/AB12399"

  lazy val view = app.injector.instanceOf[EmploymentTaxView]

  lazy val controller = new EmploymentTaxController()(
    mockMessagesControllerComponents,
    authorisedAction,
    mockAppConfig,
    view,
    mockIncomeTaxUserDataService,
    inYearAction,
    mockErrorHandler,
    testClock
  )

  ".show" should {

    "return a result when " which {
      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = {

          (mockIncomeTaxUserDataService.getSessionData(_: Int, _: String)(_: User[_])).expects(taxYear, employmentId, *)
            .returns(Future(Some(Model.employmentUserData)))

          controller.show(taxYear, employmentId = employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }

      s"has a $SEE_OTHER status when no cya in session" in new TestWithAuth {
        val result: Future[Result] = {

          (mockIncomeTaxUserDataService.getSessionData(_: Int, _: String)(_: User[_])).expects(taxYear, employmentId, *)
            .returns(Future(None))

          controller.show(taxYear, employmentId = employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=223%2FAB12399"
      }
    }
  }

  ".submit" should {

    "return a result when " which {
        s"Has a $SEE_OTHER status when cya in session" in new TestWithAuth {
          val result: Future[Result] = {

            val redirect = CheckEmploymentDetailsController.show(taxYear, employmentId).url

            (mockIncomeTaxUserDataService.getSessionDataAndReturnResult(_: Int, _: String)(_: String)(
              _:EmploymentUserData => Future[Result])(_: User[_])).expects(taxYear, employmentId, redirect, *, *).returns(Future(Redirect(redirect)))

            controller.submit(taxYear, employmentId = employmentId)(fakeRequest.withFormUrlEncodedBody("amount" -> "32").withSession(
              SessionValues.TAX_YEAR -> taxYear.toString
            ))
          }

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId).url
        }
      }
    }
}
