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

package controllers.employment

import common.SessionValues
import models.employment.{AllEmploymentData, EmploymentSource}
import play.api.http.Status._
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{Request, Result}
import support.mocks.{MockAuditService, MockEmploymentSessionService}
import utils.{TaxYearHelper, UnitTestWithApp}
import views.html.employment.AddEmploymentView

import scala.concurrent.Future

class AddEmploymentControllerSpec extends UnitTestWithApp with MockEmploymentSessionService with MockAuditService with TaxYearHelper {

  private lazy val view = app.injector.instanceOf[AddEmploymentView]
  private lazy val controller = new AddEmploymentController()(
    mockMessagesControllerComponents,
    authorisedAction,
    inYearAction,
    view,
    mockAppConfig,
    mockEmploymentSessionService,
    mockErrorHandler,
    ec
  )

  ".show" should {

    "return a result" which {

      s"has an OK($OK) status when there is no employment" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetPriorRight(taxYearEOY, None)

          controller.show(taxYearEOY)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))
        }

        status(result) shouldBe OK
      }

      s"has a SEE_OTHER($SEE_OTHER) status when there is an employment already" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetPriorRight(taxYearEOY,
            Some(AllEmploymentData(Seq(EmploymentSource("ID-001", "Mishima Zaibatsu", None, None, None, None, None, None, None, None)), None, Seq(), None)))


          controller.show(taxYearEOY)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))
        }

        status(result) shouldBe SEE_OTHER
      }

      s"has a INTERNAL_SERVER_ERROR($INTERNAL_SERVER_ERROR) status when service throws left" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetPriorLeft(taxYearEOY)
          (mockErrorHandler.handleError(_: Int)(_: Request[_])).expects(*, *).returns(InternalServerError)

          controller.show(taxYearEOY)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))
        }

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".submit" should {

    "return a result" which {

      s"has an SEE_OTHER($SEE_OTHER) status when there is no employment" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetPriorRight(taxYearEOY, None)

          controller.submit(taxYearEOY)(fakeRequest.withFormUrlEncodedBody("value" -> "true").withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))
        }

        status(result) shouldBe SEE_OTHER
      }

      s"has a REDIRECT($SEE_OTHER) status when there is an employment already" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetPriorRight(taxYearEOY,
            Some(AllEmploymentData(Seq(EmploymentSource("ID-001", "Mishima Zaibatsu", None, None, None, None, None, None, None, None)), None, Seq(), None)))


          controller.submit(taxYearEOY)(fakeRequest.withFormUrlEncodedBody("value" -> "true").withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))
        }

        status(result) shouldBe SEE_OTHER
      }

      s"has a INTERNAL_SERVER_ERROR($INTERNAL_SERVER_ERROR) status when service throws left" in new TestWithAuth {
        val result: Future[Result] = {
          mockGetPriorLeft(taxYearEOY)
          (mockErrorHandler.handleError(_: Int)(_: Request[_])).expects(*, *).returns(InternalServerError)

          controller.submit(taxYearEOY)(fakeRequest.withFormUrlEncodedBody("value" -> "true").withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))
        }

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
