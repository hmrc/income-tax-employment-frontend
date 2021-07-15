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
import config.{MockAuditService, MockEmploymentSessionService}
import models.employment.EmploymentDetailsViewModel
import play.api.http.Status._
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import utils.UnitTestWithApp
import views.html.employment.CheckEmploymentDetailsView

import scala.concurrent.Future

class CheckEmploymentDetailsControllerSpec extends UnitTestWithApp with MockEmploymentSessionService with MockAuditService{

  lazy val view = app.injector.instanceOf[CheckEmploymentDetailsView]
  lazy val controller = new CheckEmploymentDetailsController()(
    mockMessagesControllerComponents,
    authorisedAction,
    inYearAction,
    view,
    mockAppConfig,
    mockIncomeTaxUserDataService,
    mockAuditService,
    ec,
    mockErrorHandler,
    testClock
  )
  val taxYear = mockAppConfig.defaultTaxYear
  val employmentId = "223/AB12399"


  ".show" should {

    "return a result when GetEmploymentDataModel is in Session" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = {
          mockFind(taxYear, Ok(view(
            EmploymentDetailsViewModel(
              employerName = "Dave",
              employerRef = Some("reference"),
              employmentId = "id",
              startDate = Some("2020-02-12"),
              cessationDateQuestion = Some(true),
              cessationDate = Some("2020-02-12"),
              taxablePayToDate = Some(34234.15),
              totalTaxToDate = Some(6782.92),
              tipsAndOtherPaymentsQuestion = Some(true),
              tipsAndOtherPayments = Some(67676),
              isUsingCustomerData = false
            ), taxYear, isInYear = true
          )))
          controller.show(taxYear, employmentId = employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }

    "redirect the User to the Overview page no data in session" which {

      s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
        val result: Future[Result] = {
          mockFind(taxYear,Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller.show(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }
  }

}
