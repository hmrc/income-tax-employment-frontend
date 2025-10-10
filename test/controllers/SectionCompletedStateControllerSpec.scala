/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import actions.AuthorisedAction
import common.SessionValues
import config.ErrorHandler
import forms.YesNoForm
import models.mongo.JourneyAnswers
import org.apache.pekko.Done
import org.apache.pekko.dispatch.ExecutionContexts.global
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import play.api.test.Helpers.{contentType, redirectLocation, status, stubMessagesControllerComponents}
import services.SectionCompletedService
import sttp.model.Method.POST
import support.ControllerUnitTest
import support.mocks.{MockAuthorisedAction, MockErrorHandler, MockSectionCompletedService}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.authorise.Predicate
import views.html.SectionCompletedStateView

import java.util.Calendar
import scala.concurrent.Future

class SectionCompletedStateControllerSpec extends ControllerUnitTest with
  MockAuthorisedAction with
  MockSectionCompletedService with
  MockErrorHandler {

  implicit val view: SectionCompletedStateView = app.injector.instanceOf[SectionCompletedStateView]
  implicit val authorisedAction: AuthorisedAction = mockAuthorisedAction
  implicit val errorHandler: ErrorHandler = mockErrorHandler
  implicit val sectionCompletedService: SectionCompletedService = mockSectionCompletedService
  override implicit val cc = stubMessagesControllerComponents()
  private def testController = new SectionCompletedStateController()
  

  val nino  = "AA123456A"
  val mtdId = "1234567890"
  val journey = "employment-summary"
  val journeyAnswers: JourneyAnswers = JourneyAnswers(
    mtdItId = mtdId,
    taxYear = taxYear,
    journey = journey,
    data = Json.obj("journey" -> "employment-summary"),
    lastUpdated = Calendar.getInstance().toInstant
  )

  val predicate: Predicate = Enrolment("HMRC-MTD-IT")
    .withIdentifier("MTDITID", mtdId)
    .withDelegatedAuthRule("mtd-it-auth")

  ".show" should {
    "display the SectionCompletedView" when {
      "journey name is correct and status is 'Completed'" in {
        mockAuth(Some(nino))
        val journeyData = Json.obj("journey" -> "employment-summary", "status" -> "completed")
        mockGet(mtdId, taxYear, journey, Some(journeyAnswers.copy(data = journeyData)))
        val sessionRequest = fakeIndividualRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> taxYear.toString
        )
        val result: Future[Result] = testController.show(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe OK
      }

      "journey name is correct and status is 'inProgress'" in {
        mockAuth(Some(nino))
        val journeyData = Json.obj("journey" -> "employment-summary", "status" -> "inProgress")
        mockGet(mtdId, taxYear, journey, Some(journeyAnswers.copy(data = journeyData)))
        val sessionRequest = fakeIndividualRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> taxYear.toString
        )

        val result: Future[Result] = testController.show(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe OK
      }

      "journey name is correct and status is 'notStarted'" in {
        mockAuth(Some(nino))
        val journeyData = Json.obj("journey" -> "employment-summary", "status" -> "notStarted")
        mockGet(mtdId, taxYear, journey, Some(journeyAnswers.copy(data = journeyData)))
        val sessionRequest = fakeIndividualRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> taxYear.toString
        )

        val result: Future[Result] = testController.show(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe OK
      }

      "journey name is correct but Service.get returns no data" in {
        mockAuth(Some(nino))
        mockGet(mtdId, taxYear, journey, None)
        val sessionRequest = fakeIndividualRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> taxYear.toString
        )

        val result: Future[Result] = testController.show(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe OK
      }
    }
    "return a 400 result" when {
      "journey name is incorrect" in {
        mockAuth(Some(nino))
        mockHandleError(BAD_REQUEST, BadRequest)
        val journeyName = "incorrect-name"
        val sessionRequest = fakeIndividualRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> taxYear.toString
        )

        val result: Future[Result] = testController.show(taxYear, journeyName).apply(sessionRequest)
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  ".submit" should {
    "display the SectionCompletedView" when {
      "form has errors" in {
        mockAuth(Some(nino))
        val sessionRequest = fakeIndividualRequest
          .withMethod(POST.method)
          .withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> taxYear.toString
          )
          .withFormUrlEncodedBody(YesNoForm.yesNo -> "")

        val result: Future[Result] = testController.submit(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe BAD_REQUEST
        contentType(result) shouldBe Some("text/html")
      }
    }
    "save and redirect to common task list" when {
      "form is correct with correct journeyName" in {
        mockAuth(Some(nino))
        mockSet(Done)
        val sessionRequest = fakeIndividualRequest
          .withMethod(POST.method)
          .withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> taxYear.toString
          )
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)

        val result: Future[Result] = testController.submit(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/$taxYear/tasklist")
      }
    }
    "error handler called" when {
      "journeyName is incorrect" in {
        mockAuth(Some(nino))
        mockHandleError(BAD_REQUEST, BadRequest)
        val journeyName = "incorrect-name"
        val sessionRequest = fakeIndividualRequest
          .withMethod(POST.method)
          .withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> taxYear.toString
          )
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)

        val result: Future[Result] = testController.submit(taxYear, journeyName).apply(sessionRequest)
        status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
