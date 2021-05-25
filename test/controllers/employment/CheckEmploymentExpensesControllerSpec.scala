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
import config.MockIncomeTaxUserDataService
import controllers.Assets._
import models.employment._
import play.api.http.HeaderNames.LOCATION
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.header
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import utils.UnitTestWithApp
import views.html.employment.CheckEmploymentExpensesView

import scala.concurrent.Future

class CheckEmploymentExpensesControllerSpec extends UnitTestWithApp with DefaultAwaitTimeout with MockIncomeTaxUserDataService{

  lazy val view: CheckEmploymentExpensesView = app.injector.instanceOf[CheckEmploymentExpensesView]

  lazy val controller = new CheckEmploymentExpensesController(
    authorisedAction,
    view,
    mockIncomeTaxUserDataService,
    mockAppConfig,
    mockMessagesControllerComponents,
    ec
  )

  val taxYear = 2022

  val allData: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      EmploymentSource(
        employmentId = "223/AB12399",
        employerName = "maggie",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some("2020-03-11"),
        dateIgnored = Some("2020-04-04T01:01:01Z"),
        submittedOn = Some("2020-01-04T05:01:01Z"),
        employmentData = Some(EmploymentData(
          submittedOn = "2020-02-12",
          employmentSequenceNumber = Some("123456789999"),
          companyDirector = Some(true),
          closeCompany = Some(false),
          directorshipCeasedDate = Some("2020-02-12"),
          occPen = Some(false),
          disguisedRemuneration = Some(false),
          pay = Pay(34234.15, 6782.92, Some(67676), "CALENDAR MONTHLY", "2020-04-23", Some(32), Some(2))
        )),
        None
      )
    ),
    hmrcExpenses = Some(employmentExpenses),
    customerEmploymentData = Seq(),
    customerExpenses = None
  )

  "calling show() as an individual" should {

    "return status code 303 with correct Location header" when {
      "there is no expenses data in the database" in new TestWithAuth {
        val responseF: Future[Result] = {
          mockFind(taxYear,Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller.show(taxYear)(fakeRequest)
        }

        status(responseF) shouldBe SEE_OTHER
        header(LOCATION, responseF) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return status code 200 with correct content" when {
      "there is expenses data in the database" in new TestWithAuth {

        val request: FakeRequest[AnyContentAsEmpty.type] =
          fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          )

        val responseF: Future[Result] = {
          mockFind(taxYear,Ok(view(taxYear, allData.hmrcExpenses.get)))
          controller.show(taxYear)(request)
        }

        status(responseF) shouldBe OK
      }
    }

  }

  "calling show() as an agent" should {

    "return status code 303 with correct Location header" when {
      "there is no expenses data in the database" in new TestWithAuth(isAgent = true) {
        val responseF: Future[Result] = {
          mockFind(taxYear,Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller.show(taxYear)(fakeRequestWithMtditidAndNino)
        }

        status(responseF) shouldBe SEE_OTHER
        header(LOCATION, responseF) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return status code 200 with correct content" when {
      "there is expenses data in the database" in new TestWithAuth(isAgent = true) {
        val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithMtditidAndNino

        val responseF: Future[Result] = {
          mockFind(taxYear,Ok(view(taxYear, allData.hmrcExpenses.get)))
          controller.show(taxYear)(request)
        }

        status(responseF) shouldBe OK
      }
    }

  }

}
