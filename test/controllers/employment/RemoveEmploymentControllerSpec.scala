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
import config.{MockEmploymentSessionService, MockRemoveEmploymentService}
import controllers.employment.routes.EmploymentSummaryController
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{Request, Result}
import utils.UnitTestWithApp
import views.html.employment.RemoveEmploymentView

import scala.concurrent.Future

class RemoveEmploymentControllerSpec extends UnitTestWithApp
  with MockEmploymentSessionService
  with MockRemoveEmploymentService {

  private val taxYear = 2022
  private val validTaxYearEOY: Int = taxYear - 1
  private val employmentId = "001"
  private val employerName = "maggie"

  private lazy val view: RemoveEmploymentView = app.injector.instanceOf[RemoveEmploymentView]

  private lazy val controller = new RemoveEmploymentController()(
    mockMessagesControllerComponents,
    authorisedAction,
    inYearAction,
    view,
    mockAppConfig,
    mockEmploymentSessionService,
    mockRemoveEmploymentService,
    mockErrorHandler,
    ec
  )

  ".show" should {
    "return a result" which {
      s"has an OK($OK) status when there is employment data" in new TestWithAuth {
        mockFind(validTaxYearEOY, Ok(view(validTaxYearEOY, employmentId, employerName, lastEmployment = false)))

        val result: Future[Result] = controller.show(validTaxYearEOY, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> validTaxYearEOY.toString
        ))

        status(result) shouldBe OK
        bodyOf(result).contains(employerName) shouldBe true
      }

      s"has a SEE_OTHER($SEE_OTHER) status when no employment data is found for that employmentId " in new TestWithAuth {
        mockFind(validTaxYearEOY, Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(validTaxYearEOY)))

        val result: Future[Result] = controller.show(validTaxYearEOY, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> validTaxYearEOY.toString
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
        mockGetPriorRight(validTaxYearEOY, Some(employmentsModel))
        mockDeleteOrIgnore(employmentsModel, validTaxYearEOY, employmentId)(Redirect(EmploymentSummaryController.show(validTaxYearEOY)))

        val result: Future[Result] = controller.submit(validTaxYearEOY, employmentId)(fakeRequest
          .withFormUrlEncodedBody("value" -> "true")
          .withSession(
            SessionValues.TAX_YEAR -> validTaxYearEOY.toString
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe EmploymentSummaryController.show(validTaxYearEOY).url
        bodyOf(result).contains(employerName) shouldBe false
      }

      "there's no employment data found for that employmentId" in new TestWithAuth {
        mockGetPriorRight(validTaxYearEOY, Some(employmentsModel))

        val result: Future[Result] = controller.submit(validTaxYearEOY, "unknown-employment-id")(fakeRequest
          .withSession(SessionValues.TAX_YEAR -> validTaxYearEOY.toString))

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
        mockGetPriorLeft(validTaxYearEOY)

        (mockErrorHandler.handleError(_: Int)(_: Request[_])).expects(*, *).returns(InternalServerError)

        controller.submit(validTaxYearEOY, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> validTaxYearEOY.toString
        ))
      }

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
