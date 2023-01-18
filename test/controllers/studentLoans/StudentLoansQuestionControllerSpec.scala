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

package controllers.studentLoans

import common.SessionValues
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status, stubMessagesControllerComponents}
import support.ControllerUnitTest
import support.mocks.{MockAppConfig, MockAuthorisedAction, MockEmploymentSessionService, MockErrorHandler}
import utils.InYearUtil
import views.html.studentLoans.StudentLoansQuestionView

import scala.concurrent.Future

class StudentLoansQuestionControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockErrorHandler {

  private lazy val view: StudentLoansQuestionView = app.injector.instanceOf[StudentLoansQuestionView]

  private val nino = "AA123456A"
  private val employmentId = "1234567890"

  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")

  private def controller(slEnabled: Boolean = true, isEmploymentEOYEnabled: Boolean = true, taxYearErrorFeature: Boolean = true) = new StudentLoansQuestionController(
    stubMessagesControllerComponents,
    view,
    mockEmploymentSessionService,
    mockAuthorisedAction,
    new InYearUtil(),
    mockErrorHandler)(appConfig = new MockAppConfig().config(slEnabled = slEnabled, isEmploymentEOYEnabled = isEmploymentEOYEnabled,
    taxYearErrorEnabled = taxYearErrorFeature))

  ".show" should {
    "redirect to the overview page" when {
      "employmentEOYEnabled feature switch is off" in {
        mockAuth(Some(nino))
        val result: Future[Result] = controller(isEmploymentEOYEnabled = false, taxYearErrorFeature = false).show(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
      }

      "studentLoansEnabled feature switch is off" in {
        mockAuth(Some(nino))
        val result: Future[Result] = controller(slEnabled = false, taxYearErrorFeature = false).show(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
      }
    }
  }

  ".submit" should {
    "redirect to the overview page" when {
      "employmentEOYEnabled feature switch is off" in {
        mockAuth(Some(nino))
        val result: Future[Result] = controller(isEmploymentEOYEnabled = false, taxYearErrorFeature = false).submit(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
      }

      "studentLoansEnabled feature switch is off" in {
        mockAuth(Some(nino))
        val result: Future[Result] = controller(slEnabled = false, taxYearErrorFeature = false).submit(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
      }
    }
  }

}
