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

package controllers.studentLoans

import common.SessionValues
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Result
import support.mocks.{MockAppConfig, MockEmploymentSessionService, MockErrorHandler, MockStudentLoansService}
import utils.UnitTestWithApp
import views.html.studentLoans.UglAmountView

import scala.concurrent.Future

class UglAmountControllerSpec extends UnitTestWithApp
  with MockEmploymentSessionService
  with MockStudentLoansService
  with MockErrorHandler {

  private lazy val view: UglAmountView = app.injector.instanceOf[UglAmountView]

  private val employmentId = "1234567890"

  private def controller(slEnabled: Boolean = true, isEmploymentEOYEnabled: Boolean = true, taxYearErrorFeature: Boolean = true) = new UglAmountController(
    mockMessagesControllerComponents,
    authorisedAction,
    mockEmploymentSessionService,
    mockStudentLoansService,
    view,
    mockErrorHandler)(appConfig = new MockAppConfig().config(slEnabled = slEnabled, isEmploymentEOYEnabled = isEmploymentEOYEnabled,
    taxYearErrorEnabled = taxYearErrorFeature), ec)

  ".show" should {
    "redirect to the overview page" when {
      "employmentEOYEnabled feature switch is off" in new TestWithAuth {
        val result: Future[Result] = controller(isEmploymentEOYEnabled = false, taxYearErrorFeature = false).show(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
      }

      "studentLoansEnabled feature switch is off" in new TestWithAuth {
        val result: Future[Result] = controller(slEnabled = false, taxYearErrorFeature = false).show(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
      }
    }
  }

  ".submit" should {
    "redirect to the overview page" when {
      "employmentEOYEnabled feature switch is off" in new TestWithAuth {
        val result: Future[Result] = controller(isEmploymentEOYEnabled = false, taxYearErrorFeature = false).submit(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
      }

      "studentLoansEnabled feature switch is off" in new TestWithAuth {
        val result: Future[Result] = controller(slEnabled = false, taxYearErrorFeature = false).submit(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
      }
    }
  }

}
