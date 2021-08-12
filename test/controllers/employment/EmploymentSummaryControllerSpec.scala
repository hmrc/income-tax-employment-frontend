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
import config.MockEmploymentSessionService
import forms.YesNoForm
import models.employment.{AllEmploymentData, Deductions, EmploymentData, EmploymentSource, Pay, StudentLoans}
import play.api.data.Form
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import utils.UnitTestWithApp
import views.html.employment.{MultipleEmploymentsSummaryView, SingleEmploymentSummaryView, SingleEmploymentSummaryViewEOY}

import scala.concurrent.Future

class EmploymentSummaryControllerSpec extends UnitTestWithApp with MockEmploymentSessionService {

  object FullModel {

    val employmentSource1 = EmploymentSource(
      employmentId = "223/AB12399",
      employerName = "Mishima Zaibatsu",
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
        pay = Some(Pay(Some(34234.15), Some(6782.92), Some(67676), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
        Some(Deductions(
          studentLoans = Some(StudentLoans(
            uglDeductionAmount = Some(100.00),
            pglDeductionAmount = Some(100.00)
          ))
        ))
      )),
      None
    )

    val employmentSource2 = EmploymentSource(
      employmentId = "223/AB12399",
      employerName = "Violet Systems",
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
        pay = Some(Pay(Some(34234.15), Some(6782.92), Some(67676), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
        Some(Deductions(
          studentLoans = Some(StudentLoans(
            uglDeductionAmount = Some(100.00),
            pglDeductionAmount = Some(100.00)
          ))
        ))
      )),
      None
    )

    val oneEmploymentSourceData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(employmentSource1),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )

    val multipleEmploymentSourcesData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(employmentSource1, employmentSource2),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }

  lazy val singleView = app.injector.instanceOf[SingleEmploymentSummaryView]
  lazy val singleEOYView = app.injector.instanceOf[SingleEmploymentSummaryViewEOY]
  lazy val multipleView = app.injector.instanceOf[MultipleEmploymentsSummaryView]

  lazy val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "employment.addAnother.error"
  )

  lazy val controller = new EmploymentSummaryController()(
    mockMessagesControllerComponents,
    authorisedAction,
    mockAppConfig,
    singleView,
    multipleView,
    singleEOYView,
    mockIncomeTaxUserDataService,
    inYearAction
  )


  val taxYear:Int = mockAppConfig.defaultTaxYear

  ".show" should {

    "render single employment summary view when there is only one employment" which {
      s"has an OK($OK) status" in new TestWithAuth {
        mockFind(taxYear, Ok(singleView(taxYear, FullModel.employmentSource1, false)))

        val result: Future[Result] = controller.show(taxYear)(fakeRequest)
        status(result) shouldBe OK
        bodyOf(result).contains("Mishima Zaibatsu") shouldBe true
      }
    }

    "render multiple employment summary view when there are two employments" which {
      s"has an OK($OK) status" in new TestWithAuth {
        mockFind(taxYear, Ok(multipleView(taxYear, Seq(FullModel.employmentSource1, FullModel.employmentSource2), false, false, yesNoForm)))

        val result: Future[Result] = controller.show(taxYear)(fakeRequest)
        status(result) shouldBe OK
        bodyOf(result).contains("Violet Systems") shouldBe true
      }
    }

    "redirect the User to the Overview page no data in session" which {

      s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth{
        mockFind(taxYear, Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))

        val result: Future[Result] = controller.show(taxYear)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }
  }

}
