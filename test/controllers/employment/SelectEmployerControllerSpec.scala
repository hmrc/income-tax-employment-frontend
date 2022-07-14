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

import actions.ActionsProvider
import common.SessionValues
import config.AppConfig
import controllers.details.routes.EmployerNameController
import controllers.employment.routes.EmploymentSummaryController
import forms.details.EmployerNameForm.employerName
import forms.employment.SelectEmployerForm
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, Redirect}
import support.builders.models.UserBuilder.aUser
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.mocks._
import utils.{InYearUtil, UnitTest}
import views.html.employment.SelectEmployerView

import scala.concurrent.{ExecutionContext, Future}

class SelectEmployerControllerSpec extends UnitTest
  with MockEmploymentSessionService
  with MockUnignoreEmploymentService
  with MockErrorHandler
  with MockActionsProvider
  with MockRedirectsMatcherUtils {

  private lazy val view: SelectEmployerView = app.injector.instanceOf[SelectEmployerView]
  implicit private lazy val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val actionsProvider = {
    val mockAppConfig: AppConfig = new MockAppConfig().config()
    new ActionsProvider(
      mockAuthorisedAction,
      mockEmploymentSessionService,
      mockErrorHandler,
      new InYearUtil()(mockAppConfig),
      mockRedirectsMatcherUtils,
      mockAppConfig
    )
  }

  private lazy val controller = new SelectEmployerController(
    actionsProvider,
    view,
    mockUnignoreEmploymentService,
    mockEmploymentSessionService,
    mockErrorHandler,
    new SelectEmployerForm)(mockMessagesControllerComponents, mockAppConfig, ec)

  ".show" should {
    "return a result" which {
      s"has an OK($OK) status when there is employment data" in new TestWithAuth {
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(anAllEmploymentData.hmrcEmploymentData.head.copy(dateIgnored = Some("2019-04-21"))))))

        val result: Future[Result] = controller.show(taxYearEOY)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))

        status(result) shouldBe OK
        bodyOf(result).contains(employerName) shouldBe true
      }

      s"has a SEE_OTHER($SEE_OTHER) status when it's not end of year" in new TestWithAuth {
        mockFind(taxYear, Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))

        val result: Future[Result] = controller.show(taxYear)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

      s"has a SEE_OTHER($SEE_OTHER) status when there is no ignored employments" in new TestWithAuth {
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData))

        val result: Future[Result] = controller.show(taxYearEOY)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) should include(EmployerNameController.show(taxYearEOY, "id").url.dropRight(2))
      }

      s"has a SEE_OTHER($SEE_OTHER) status when there is no ignored employments and an id in session" in new TestWithAuth {
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData))

        val result: Future[Result] = controller.show(taxYearEOY)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString,
          SessionValues.TEMP_NEW_EMPLOYMENT_ID -> "id"
        ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) should include(EmployerNameController.show(taxYearEOY, "id").url)
      }
    }
  }

  ".submit" should {
    s"return a SEE_OTHER($SEE_OTHER) status" when {

      s"form is submitted" in new TestWithAuth {
        private val dateIgnored: Some[String] = Some("2019-04-21")
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(anAllEmploymentData.hmrcEmploymentData.head.copy(dateIgnored = dateIgnored)))))
        mockUnignore(aUser.copy(sessionId = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"), taxYearEOY, anEmploymentSource.copy(dateIgnored = dateIgnored), Right(()))
        mockClear(clearCya = false)

        val result: Future[Result] = controller.submit(taxYearEOY)(fakeRequest
          .withFormUrlEncodedBody("value" -> anEmploymentSource.employmentId)
          .withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe EmploymentSummaryController.show(taxYearEOY).url
      }

      s"form is submitted with an id in session" in new TestWithAuth {
        private val dateIgnored: Some[String] = Some("2019-04-21")
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(anAllEmploymentData.hmrcEmploymentData.head.copy(dateIgnored = dateIgnored)))))
        mockUnignore(aUser.copy(sessionId = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"), taxYearEOY, anEmploymentSource.copy(dateIgnored = dateIgnored), Right(()))
        mockClear()

        val result: Future[Result] = controller.submit(taxYearEOY)(fakeRequest
          .withFormUrlEncodedBody("value" -> anEmploymentSource.employmentId)
          .withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString,
            SessionValues.TEMP_NEW_EMPLOYMENT_ID -> "id"
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe EmploymentSummaryController.show(taxYearEOY).url
      }
    }

    s"return a INTERNAL SERVER ERROR($INTERNAL_SERVER_ERROR) status" when {
      s"unignore fails" in new TestWithAuth {
        private val dateIgnored: Some[String] = Some("2019-04-21")
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(anAllEmploymentData.hmrcEmploymentData.head.copy(dateIgnored = dateIgnored)))))
        mockUnignore(aUser.copy(sessionId = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"), taxYearEOY, anEmploymentSource.copy(dateIgnored = dateIgnored),
          Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))
        mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

        val result: Future[Result] = controller.submit(taxYearEOY)(fakeRequest
          .withFormUrlEncodedBody("value" -> anEmploymentSource.employmentId)
          .withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    s"return a INTERNAL SERVER ERROR($INTERNAL_SERVER_ERROR) status" when {
      s"clear fails" in new TestWithAuth {
        private val dateIgnored: Some[String] = Some("2019-04-21")
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(anAllEmploymentData.hmrcEmploymentData.head.copy(dateIgnored = dateIgnored)))))
        mockUnignore(aUser.copy(sessionId = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"), taxYearEOY, anEmploymentSource.copy(dateIgnored = dateIgnored), Right(()))
        mockClear(Left(()))
        mockInternalServerError(InternalServerError)

        val result: Future[Result] = controller.submit(taxYearEOY)(fakeRequest
          .withFormUrlEncodedBody("value" -> anEmploymentSource.employmentId)
          .withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString,
            SessionValues.TEMP_NEW_EMPLOYMENT_ID -> "id"
          ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      s"clear fails with no id in session" in new TestWithAuth {
        private val dateIgnored: Some[String] = Some("2019-04-21")
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(anAllEmploymentData.hmrcEmploymentData.head.copy(dateIgnored = dateIgnored)))))
        mockUnignore(aUser.copy(sessionId = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"), taxYearEOY, anEmploymentSource.copy(dateIgnored = dateIgnored), Right(()))
        mockClear(Left(()), clearCya = false)
        mockInternalServerError(InternalServerError)

        val result: Future[Result] = controller.submit(taxYearEOY)(fakeRequest
          .withFormUrlEncodedBody("value" -> anEmploymentSource.employmentId)
          .withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    s"return a SEE OTHER($SEE_OTHER) status" when {
      s"clear removes the prior cya data" in new TestWithAuth {
        private val dateIgnored: Some[String] = Some("2019-04-21")
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(anAllEmploymentData.hmrcEmploymentData.head.copy(dateIgnored = dateIgnored)))))
        mockUnignore(aUser.copy(sessionId = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"), taxYearEOY, anEmploymentSource.copy(dateIgnored = dateIgnored), Right(()))
        mockClear(Right(()))

        val result: Future[Result] = controller.submit(taxYearEOY)(fakeRequest
          .withFormUrlEncodedBody("value" -> anEmploymentSource.employmentId)
          .withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString,
            SessionValues.TEMP_NEW_EMPLOYMENT_ID -> "id"
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe EmploymentSummaryController.show(taxYearEOY).url
      }
    }

    s"return a SEE_OTHER($SEE_OTHER) status" when {
      s"there are no ignored employments" in new TestWithAuth {
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData))

        val result: Future[Result] = controller.submit(taxYearEOY)(fakeRequest
          .withFormUrlEncodedBody("value" -> "employmentId")
          .withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) should include(EmployerNameController.show(taxYearEOY, "id").url.dropRight(2))
      }
    }

    s"return a SEE_OTHER($SEE_OTHER) status" when {
      s"adding a new employment" in new TestWithAuth {
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(anAllEmploymentData.hmrcEmploymentData.head.copy(dateIgnored = Some("2019-04-21"))))))

        val result: Future[Result] = controller.submit(taxYearEOY)(fakeRequest
          .withFormUrlEncodedBody("value" -> SessionValues.ADD_A_NEW_EMPLOYER)
          .withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) should include(EmployerNameController.show(taxYearEOY, "id").url.dropRight(2))
      }
    }

    s"return a BAD REQUEST($BAD_REQUEST) status" when {
      s"invalid form" in new TestWithAuth {
        mockGetPriorRight(taxYearEOY, Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(anAllEmploymentData.hmrcEmploymentData.head.copy(dateIgnored = Some("2019-04-21"))))))

        val result: Future[Result] = controller.submit(taxYearEOY)(fakeRequest
          .withFormUrlEncodedBody("value" -> "12345678945678567")
          .withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
