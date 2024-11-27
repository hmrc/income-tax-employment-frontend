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
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.test.Helpers.{contentType, redirectLocation, status}
import services.SectionCompletedService
import sttp.model.Method.POST
import support.ControllerUnitTest
import support.mocks.{MockAuthorisedAction, MockErrorHandler, MockSectionCompletedService}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.http.HeaderCarrier
import views.html.SectionCompletedStateView

import java.util.Calendar
import scala.concurrent.ExecutionContext.Implicits.global

class SectionCompletedStateControllerSpec extends ControllerUnitTest {

  implicit val view: SectionCompletedStateView = app.injector.instanceOf[SectionCompletedStateView]

  trait Test extends MockAuthorisedAction with MockSectionCompletedService with MockErrorHandler {
    val hc: HeaderCarrier = HeaderCarrier()

    implicit val authorisedAction: AuthorisedAction = mockAuthorisedAction
    implicit val errorHandler: ErrorHandler = mockErrorHandler
    implicit val ec: Any = ec
    implicit val sectionCompletedService: SectionCompletedService = mockSectionCompletedService

    class TestController extends SectionCompletedStateController() {}

    lazy val target = new TestController()
  }

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
      "journey name is correct and status is 'Completed'" in new Test {
        mockAuth(Some(nino))
        private val journeyData = Json.obj("journey" -> "employment-summary", "status" -> "completed")
        mockGet(mtdId, taxYear, journey, Some(journeyAnswers.copy(data = journeyData)))
        private val sessionRequest = fakeIndividualRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> taxYear.toString
        )

        private val result = target.show(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe OK
      }

      "journey name is correct and status is 'inProgress'" in new Test {
        mockAuth(Some(nino))
        private val journeyData = Json.obj("journey" -> "employment-summary", "status" -> "inProgress")
        mockGet(mtdId, taxYear, journey, Some(journeyAnswers.copy(data = journeyData)))
        private val sessionRequest = fakeIndividualRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> taxYear.toString
        )

        private val result = target.show(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe OK
      }

      "journey name is correct and status is 'notStarted'" in new Test {
        mockAuth(Some(nino))
        private val journeyData = Json.obj("journey" -> "employment-summary", "status" -> "notStarted")
        mockGet(mtdId, taxYear, journey, Some(journeyAnswers.copy(data = journeyData)))
        private val sessionRequest = fakeIndividualRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> taxYear.toString
        )

        private val result = target.show(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe OK
      }

      "journey name is correct but Service.get returns no data" in new Test {
        mockAuth(Some(nino))
        mockGet(mtdId, taxYear, journey, None)
        private val sessionRequest = fakeIndividualRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> taxYear.toString
        )

        private val result = target.show(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe OK
      }
    }
    "return a 400 result" when {
      "journey name is incorrect" in new Test {
        mockAuth(Some(nino))
        mockHandleError(BAD_REQUEST, BadRequest)
        private val journeyName = "incorrect-name"
        private val sessionRequest = fakeIndividualRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> taxYear.toString
        )

        private val result = target.show(taxYear, journeyName).apply(sessionRequest)
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  ".submit" should {
    "display the SectionCompletedView" when {
      "form has errors" in new Test {
        mockAuth(Some(nino))
        private val sessionRequest = fakeIndividualRequest
          .withMethod(POST.method)
          .withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> taxYear.toString
          )
          .withFormUrlEncodedBody(YesNoForm.yesNo -> "")

        private val result = target.submit(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe BAD_REQUEST
        contentType(result) shouldBe Some("text/html")
      }
    }
    "save and redirect to common task list" when {
      "form is correct with correct journeyName" in new Test {
        mockAuth(Some(nino))
        mockSet(Done)
        private val sessionRequest = fakeIndividualRequest
          .withMethod(POST.method)
          .withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> taxYear.toString
          )
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)

        private val result = target.submit(taxYear, journey).apply(sessionRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/2025/tasklist")
      }
    }
    "error handler called" when {
      "journeyName is incorrect" in new Test {
        mockAuth(Some(nino))
        mockHandleError(BAD_REQUEST, BadRequest)
        private val journeyName = "incorrect-name"
        private val sessionRequest = fakeIndividualRequest
          .withMethod(POST.method)
          .withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> taxYear.toString
          )
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)

        private val result = target.submit(taxYear, journeyName).apply(sessionRequest)
        status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
