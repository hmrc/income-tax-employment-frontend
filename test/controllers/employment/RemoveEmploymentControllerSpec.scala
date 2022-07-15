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
import controllers.employment.routes.EmploymentSummaryController
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{Request, Result}
import support.mocks.{MockEmploymentSessionService, MockErrorHandler, MockRemoveEmploymentService}
import utils.UnitTest
import views.html.employment.RemoveEmploymentView

import scala.concurrent.{ExecutionContext, Future}

class RemoveEmploymentControllerSpec extends UnitTest
  with MockEmploymentSessionService
  with MockRemoveEmploymentService
  with MockErrorHandler {

  private val employmentId = "001"
  private val employerName = "maggie"

  private lazy val view: RemoveEmploymentView = app.injector.instanceOf[RemoveEmploymentView]
  implicit private lazy val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit private val messages: Messages = getMessages(isWelsh = false)

  private lazy val controller = new RemoveEmploymentController(
    mockMessagesControllerComponents,
    authorisedAction,
    inYearAction,
    view,
    mockEmploymentSessionService,
    mockRemoveEmploymentService,
    mockErrorHandler)(mockAppConfig, ec)

  ".show" should {
    "return a result" which {
      s"has an OK($OK) status when there is employment data" in new TestWithAuth {
        mockFind(taxYearEOY, Ok(view(taxYearEOY, employmentId, employerName, lastEmployment = false, isHmrcEmployment = false)))

        val result: Future[Result] = controller.show(taxYearEOY, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))

        status(result) shouldBe OK
        bodyOf(result).contains(employerName) shouldBe true
      }

      s"has a SEE_OTHER($SEE_OTHER) status when no employment data is found for that employmentId " in new TestWithAuth {
        mockFind(taxYearEOY, Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)))

        val result: Future[Result] = controller.show(taxYearEOY, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

      s"has a SEE_OTHER($SEE_OTHER) status it's not end of year" in new TestWithAuth {
        mockFind(taxYear, Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))

        val result: Future[Result] = controller.show(taxYear, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }
  }

  ".submit" should {
    s"return a SEE_OTHER($SEE_OTHER) status" when {
      s"form is submitted" in new TestWithAuth {
        mockGetPriorRight(taxYearEOY, Some(employmentsModel))
        mockDeleteOrIgnore(employmentsModel, taxYearEOY, employmentId)

        val result: Future[Result] = controller.submit(taxYearEOY, employmentId)(fakeRequest
          .withFormUrlEncodedBody("value" -> "true")
          .withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe EmploymentSummaryController.show(taxYearEOY).url
        bodyOf(result).contains(employerName) shouldBe false
      }

      "there's no employment data found for that employmentId" in new TestWithAuth {
        mockGetPriorRight(taxYearEOY, Some(employmentsModel))

        val result: Future[Result] = controller.submit(taxYearEOY, "unknown-employment-id")(fakeRequest
          .withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
      }

      "it's not end of year" in new TestWithAuth {
        mockGetPriorRight(taxYear, Some(employmentsModel))

        val result: Future[Result] = controller.submit(taxYear, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }

    "return an error if the call fails" in new TestWithAuth {
      val result: Future[Result] = {
        mockGetPriorLeft(taxYearEOY)

        (mockErrorHandler.handleError(_: Int)(_: Request[_])).expects(*, *).returns(InternalServerError)

        controller.submit(taxYearEOY, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))
      }

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
