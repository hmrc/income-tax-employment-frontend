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

package controllers.employment

import actions.ActionsProvider
import akka.actor.ActorSystem
import common.SessionValues
import config.AppConfig
import models.employment._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status, stubMessagesControllerComponents}
import support.ControllerUnitTest
import support.mocks._
import utils.InYearUtil
import views.html.employment.EmploymentSummaryView

import scala.concurrent.Future

class EmploymentSummaryControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockActionsProvider
  with MockRedirectsMapper {

  object FullModel {

    val employmentSource1: HmrcEmploymentSource = HmrcEmploymentSource(
      employmentId = "223/AB12399",
      employerName = "Mishima Zaibatsu",
      employerRef = Some("223/AB12399"),
      payrollId = Some("123456789999"),
      startDate = Some("2019-04-21"),
      cessationDate = Some(s"${taxYearEOY - 1}-03-11"),
      dateIgnored = Some(s"${taxYearEOY - 1}-04-04T01:01:01Z"),
      submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"),
      hmrcEmploymentFinancialData = Some(
        EmploymentFinancialData(
          employmentData = Some(EmploymentData(
            submittedOn = s"${taxYearEOY - 1}-02-12",
            employmentSequenceNumber = Some("123456789999"),
            companyDirector = Some(true),
            closeCompany = Some(false),
            directorshipCeasedDate = Some(s"${taxYearEOY - 1}-02-12"),
            disguisedRemuneration = Some(false),
            pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some(s"${taxYearEOY - 1}-04-23"), Some(32), Some(2))),
            Some(Deductions(
              studentLoans = Some(StudentLoans(
                uglDeductionAmount = Some(100.00),
                pglDeductionAmount = Some(100.00)
              ))
            ))
          )),
          None
        )
      ), None
    )

    val employmentSource2: HmrcEmploymentSource = HmrcEmploymentSource(
      employmentId = "223/AB12399",
      employerName = "Violet Systems",
      employerRef = Some("223/AB12399"),
      payrollId = Some("123456789999"),
      startDate = Some("2019-04-21"),
      cessationDate = Some(s"${taxYearEOY - 1}-03-11"),
      dateIgnored = Some(s"${taxYearEOY - 1}-04-04T01:01:01Z"),
      submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"),
      hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
        employmentData = Some(EmploymentData(
          submittedOn = s"${taxYearEOY - 1}-02-12",
          employmentSequenceNumber = Some("123456789999"),
          companyDirector = Some(true),
          closeCompany = Some(false),
          directorshipCeasedDate = Some(s"${taxYearEOY - 1}-02-12"),
          disguisedRemuneration = Some(false),
          pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some(s"${taxYearEOY - 1}-04-23"), Some(32), Some(2))),
          Some(Deductions(
            studentLoans = Some(StudentLoans(
              uglDeductionAmount = Some(100.00),
              pglDeductionAmount = Some(100.00)
            ))
          ))
        )),
        None
      )), None

    )

    val oneEmploymentSourceData:
      AllEmploymentData = AllEmploymentData(
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

  private val nino = "AA123456A"

  private val employmentSummaryView = app.injector.instanceOf[EmploymentSummaryView]

  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")

  implicit val actorSystem: ActorSystem = ActorSystem()
  private val actionsProvider = {
    val mockAppConfig: AppConfig = new MockAppConfig().config()
    new ActionsProvider(
      mockAuthorisedAction,
      mockEmploymentSessionService,
      mockErrorHandler,
      new InYearUtil()(mockAppConfig),
      mockRedirectsMapper,
      mockAppConfig
    )
  }

  private def controller(isEmploymentEOYEnabled: Boolean = true) = new EmploymentSummaryController(
    employmentSummaryView,
    mockEmploymentSessionService,
    new InYearUtil(),
    mockErrorHandler,
    actionsProvider
  )(stubMessagesControllerComponents, new MockAppConfig().config(isEmploymentEOYEnabled = isEmploymentEOYEnabled))

  ".addNewEmployment" should {
    "redirect to add employment page when there is no session data and no prior employments" which {
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYearEOY, None)

        val result: Future[Result] = controller().addNewEmployment(taxYearEOY)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/details/employer-name?employmentId=")
      }
    }
    "redirect to employer name page when there is no session data and some prior employment" which {
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYearEOY, Some(FullModel.oneEmploymentSourceData.copy(hmrcEmploymentData = Seq(FullModel.oneEmploymentSourceData.hmrcEmploymentData.head.copy(dateIgnored = None)))))

        val result: Future[Result] = controller().addNewEmployment(taxYearEOY)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/details/employer-name?employmentId=")
      }
    }
    "redirect to employer name page when there is session data and some prior employment" which {
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYearEOY, Some(FullModel.oneEmploymentSourceData.copy(hmrcEmploymentData = Seq(FullModel.oneEmploymentSourceData.hmrcEmploymentData.head.copy(dateIgnored = None)))))
        mockClear()

        val result: Future[Result] = controller().addNewEmployment(taxYearEOY)(fakeRequest.withSession(
          SessionValues.TEMP_NEW_EMPLOYMENT_ID -> "12345678901234567890",
          SessionValues.TAX_YEAR -> taxYearEOY.toString))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/details/employer-name?employmentId=")
      }
    }
    "redirect to select employer page when there is no session data and an ignored hmrc employment" which {
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYearEOY, Some(FullModel.oneEmploymentSourceData))

        val result: Future[Result] = controller().addNewEmployment(taxYearEOY)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/select-employer")
      }
    }
    "redirect to select employer page when there is session data and an ignored hmrc employment" which {
      s"has an SEE_OTHER($SEE_OTHER) status when session data is cleared successfully" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYearEOY, Some(FullModel.oneEmploymentSourceData))
        mockClear()

        val result: Future[Result] = controller().addNewEmployment(taxYearEOY)(fakeRequest.withSession(
          SessionValues.TEMP_NEW_EMPLOYMENT_ID -> "12345678901234567890",
          SessionValues.TAX_YEAR -> taxYearEOY.toString))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/select-employer")
      }

      s"has an INTERNAL SERVER ERROR($INTERNAL_SERVER_ERROR) status when session data is cleared successfully" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYearEOY, Some(FullModel.oneEmploymentSourceData))
        mockClear(Left())
        mockInternalServerError(InternalServerError)

        val result: Future[Result] = controller().addNewEmployment(taxYearEOY)(fakeRequest.withSession(
          SessionValues.TEMP_NEW_EMPLOYMENT_ID -> "12345678901234567890",
          SessionValues.TAX_YEAR -> taxYearEOY.toString))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".show" should {
    "render single employment summary view when there is only one employment" which {
      s"has an OK($OK) status" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYear, Some(FullModel.oneEmploymentSourceData))

        val result = await(controller().show(taxYear)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString)))
        result.header.status shouldBe OK
        await(result.body.consumeData.map(_.utf8String)).contains("Mishima Zaibatsu")  shouldBe true
      }
    }

    "render multiple employment summary view when there are two employments" which {
      s"has an OK($OK) status" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYear, Some(FullModel.multipleEmploymentSourcesData))

        val result = await(controller().show(taxYear)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString)))
        result.header.status shouldBe OK
        await(result.body.consumeData.map(_.utf8String)).contains("Violet Systems")  shouldBe true
      }
    }

    "redirect the User to the Overview page when EOY and employmentEOYEnabled is false" which {
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYearEOY, Some(FullModel.multipleEmploymentSourcesData))

        val result: Future[Result] = controller(isEmploymentEOYEnabled = false).show(taxYearEOY)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
      }
    }

    "render summary view when no data in session" which {
      s"has the SEE_OTHER($SEE_OTHER) status" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYear, None)

        val result: Future[Result] = controller().show(taxYear)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))

        status(result) shouldBe OK
      }
    }
  }
}
