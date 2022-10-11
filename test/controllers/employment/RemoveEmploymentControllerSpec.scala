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

import akka.actor.ActorSystem
import common.SessionValues
import controllers.employment.routes.EmploymentSummaryController
import models.AuthorisationRequest
import models.benefits.Benefits
import models.employment._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status, stubMessagesControllerComponents}
import support.ControllerUnitTest
import support.builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import support.mocks.{MockAuthorisedAction, MockEmploymentSessionService, MockErrorHandler, MockRemoveEmploymentService}
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.InYearUtil
import views.html.employment.RemoveEmploymentView

import scala.concurrent.Future

class RemoveEmploymentControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockRemoveEmploymentService
  with MockErrorHandler {

  private val nino = "AA123456A"
  private val employmentId = "001"
  private val employerName = "maggie"

  private lazy val view: RemoveEmploymentView = app.injector.instanceOf[RemoveEmploymentView]

  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, nino, "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
      fakeRequest)
  implicit private val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest.withHeaders())
  implicit val actorSystem: ActorSystem = ActorSystem()

  private lazy val controller = new RemoveEmploymentController(
    stubMessagesControllerComponents,
    mockAuthorisedAction,
    new InYearUtil(),
    view,
    mockEmploymentSessionService,
    mockRemoveEmploymentService,
    mockErrorHandler)(appConfig, ec)

  lazy val employmentsModel: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(),
    hmrcExpenses = Some(anEmploymentExpenses),
    customerEmploymentData = Seq(EmploymentSource(
      employmentId = "001",
      employerName = "maggie",
      employerRef = Some("223/AB12399"),
      payrollId = Some("123456789999"),
      startDate = Some("2019-04-21"),
      cessationDate = Some(s"${taxYearEOY - 1}-03-11"),
      dateIgnored = Some(s"${taxYearEOY - 1}-04-04T01:01:01Z"),
      submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"),
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
      Some(EmploymentBenefits(
        submittedOn = s"${taxYearEOY - 1}-02-12",
        benefits = Some(allBenefits)
      ))
    )),
    customerExpenses = None
  )
  lazy val allBenefits: Benefits = Benefits(
    car = Some(1.23),
    carFuel = Some(2.00),
    van = Some(3.00),
    vanFuel = Some(4.00),
    mileage = Some(5.00),
    accommodation = Some(6.00),
    qualifyingRelocationExpenses = Some(7.00),
    nonQualifyingRelocationExpenses = Some(8.00),
    travelAndSubsistence = Some(9.00),
    personalIncidentalExpenses = Some(10.00),
    entertaining = Some(11.00),
    telephone = Some(12.00),
    employerProvidedServices = Some(13.00),
    employerProvidedProfessionalSubscriptions = Some(14.00),
    service = Some(15.00),
    medicalInsurance = Some(16.00),
    nurseryPlaces = Some(17.00),
    beneficialLoan = Some(18.00),
    educationalServices = Some(19.00),
    incomeTaxPaidByDirector = Some(20.00),
    paymentsOnEmployeesBehalf = Some(21.00),
    expenses = Some(22.00),
    taxableExpenses = Some(23.00),
    vouchersAndCreditCards = Some(24.00),
    nonCash = Some(25.00),
    otherItems = Some(26.00),
    assets = Some(27.00),
    assetTransfer = Some(280000.00)
  )

  ".show" should {
    "return a result" which {
      s"has an OK($OK) status when there is employment data" in {
        mockAuth(Some(nino))
        mockFind(taxYearEOY, Ok(view(taxYearEOY, employmentId, employerName, isHmrcEmployment = false, startDate = "")))

        val result = await(controller.show(taxYearEOY, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        )))

        result.header.status shouldBe OK
        await(result.body.consumeData.map(_.utf8String)).contains(employerName)  shouldBe true
      }

      s"has a SEE_OTHER($SEE_OTHER) status when no employment data is found for that employmentId " in {
        mockAuth(Some(nino))
        mockFind(taxYearEOY, Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)))

        val result: Future[Result] = controller.show(taxYearEOY, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }

      s"has a SEE_OTHER($SEE_OTHER) status it's not end of year" in {
        mockAuth(Some(nino))
        mockFind(taxYear, Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))

        val result: Future[Result] = controller.show(taxYear, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }

  ".submit" should {
    s"return a SEE_OTHER($SEE_OTHER) status" when {
      s"form is submitted" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYearEOY, Some(employmentsModel))
        mockDeleteOrIgnore(employmentsModel, taxYearEOY, employmentId)

        val result = await(controller.submit(taxYearEOY, employmentId)(fakeRequest
          .withFormUrlEncodedBody("value" -> "true")
          .withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          )))

        result.header.status shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe EmploymentSummaryController.show(taxYearEOY).url
        await(result.body.consumeData.map(_.utf8String)).contains(employerName) shouldBe false
      }

      "there's no employment data found for that employmentId" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYearEOY, Some(employmentsModel))

        val result: Future[Result] = controller.submit(taxYearEOY, "unknown-employment-id")(fakeRequest
          .withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
      }

      "it's not end of year" in {
        mockAuth(Some(nino))
        mockGetPriorRight(taxYear, Some(employmentsModel))

        val result: Future[Result] = controller.submit(taxYear, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return an error if the call fails" in {
      mockAuth(Some(nino))
      val result: Future[Result] = {
        mockGetPriorLeft(taxYearEOY)

        (mockErrorHandler.handleError(_: Int)(_: Request[_])).expects(*, *).returns(InternalServerError)

        controller.submit(taxYearEOY, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))
      }

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
