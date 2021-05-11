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
import controllers.Assets._
import models.employment.{AllEmploymentData, EmploymentData, EmploymentExpenses, EmploymentSource, Expenses, Pay}
import play.api.http.HeaderNames.LOCATION
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.header
import utils.UnitTestWithApp
import views.html.employment.CheckEmploymentExpensesView

import scala.concurrent.Future

class CheckEmploymentExpensesControllerSpec extends UnitTestWithApp with DefaultAwaitTimeout {

  lazy val controller = new CheckEmploymentExpensesController(
    authorisedAction,
    app.injector.instanceOf[CheckEmploymentExpensesView],
    mockAppConfig,
    mockMessagesControllerComponents
  )

  val taxYear = 2022
  val expenses: Expenses = Expenses(Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8))
  val employmentExpenses: EmploymentExpenses = EmploymentExpenses(
    submittedOn = None,
    totalExpenses = None,
    expenses = Some(expenses)
  )
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
      "there is no expenses data in session" in new TestWithAuth {
        val responseF: Future[Result] = controller.show(taxYear)(fakeRequest)

        status(responseF) shouldBe SEE_OTHER
        header(LOCATION, responseF) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return status code 200 with correct content" when {
      "there is expenses data in session" in new TestWithAuth {

        val request: FakeRequest[AnyContentAsEmpty.type] =
          fakeRequest.withSession(
            SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
              Json.toJson(allData)
            ),
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          )

        val responseF: Future[Result] = controller.show(taxYear)(request)

        status(responseF) shouldBe OK
      }
    }

  }

  "calling show() as an agent" should {

    "return status code 303 with correct Location header" when {
      "there is no expenses data in session" in new TestWithAuth(isAgent = true) {
        val responseF: Future[Result] = controller.show(taxYear)(fakeRequestWithMtditidAndNino)

        status(responseF) shouldBe SEE_OTHER
        header(LOCATION, responseF) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return status code 200 with correct content" when {
      "there is expenses data in session" in new TestWithAuth(isAgent = true) {
        val request: FakeRequest[AnyContentAsEmpty.type] =
          fakeRequestWithMtditidAndNino.withSession(
            SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
              Json.toJson(allData)
            )
          )

        val responseF: Future[Result] = controller.show(taxYear)(request)

        status(responseF) shouldBe OK
      }
    }

  }

}
