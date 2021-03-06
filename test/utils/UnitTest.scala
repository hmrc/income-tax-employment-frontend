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

package utils

import akka.actor.ActorSystem
import com.codahale.metrics.SharedMetricRegistries
import common.SessionValues
import config.AppConfig
import models.benefits.Benefits
import models.employment._
import models.expenses.Expenses
import models.{AuthorisationRequest, IncomeTaxUserData}
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc._
import play.api.test.{FakeRequest, Helpers}
import support.mocks.{MockAppConfig, MockAuthorisedAction}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import views.html.errors.AgentAuthErrorPageView

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

@deprecated("Use UnitTest in support package for unit tests")
trait UnitTest extends AnyWordSpec with Matchers with MockFactory with BeforeAndAfterEach with GuiceOneAppPerSuite with TestTaxYearHelper
  with MockAuthorisedAction {

  class TestWithAuth(isAgent: Boolean = false, nino: Option[String] = Some("AA123456A")) {
    if (isAgent) mockAuthAsAgent() else mockAuth(nino)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
  }

  implicit val actorSystem: ActorSystem = ActorSystem()

  implicit val testClock: Clock = UnitTestClock

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  val sessionId: String = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> sessionId)
  val fakeRequestWithMtditidAndNino: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionValues.CLIENT_MTDITID -> "1234567890",
    SessionValues.CLIENT_NINO -> "AA123456A",
    SessionValues.TAX_YEAR -> taxYear.toString,
    SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(",")
  ).withHeaders("X-Session-ID" -> sessionId)
  val fakeRequestWithNino: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(

    SessionValues.CLIENT_NINO -> "AA123456A"
  )
  implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
  val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()

  implicit val mockAppConfig: AppConfig = new MockAppConfig().config()
  implicit val mockControllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  implicit val mockExecutionContext: ExecutionContext = ExecutionContext.Implicits.global
  val agentAuthErrorPageView: AgentAuthErrorPageView = app.injector.instanceOf[AgentAuthErrorPageView]

  implicit lazy val mockMessagesControllerComponents: MessagesControllerComponents = Helpers.stubMessagesControllerComponents()
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString), fakeRequest)

  protected implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  protected lazy val defaultMessages: Messages = messagesApi.preferred(fakeRequest.withHeaders())
  protected lazy val welshMessages: Messages = messagesApi.preferred(Seq(Lang("cy")))

  protected def getMessages(isWelsh: Boolean): Messages = if (isWelsh) welshMessages else defaultMessages

  val inYearAction = new InYearUtil

  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  def redirectUrl(awaitable: Future[Result]): String = {
    await(awaitable).header.headers.getOrElse("Location", "/")
  }

  def getSession(awaitable: Future[Result]): Session = {
    await(awaitable).session
  }

  val nino = "AA123456A"
  val mtditid = "1234567890"

  val userData: IncomeTaxUserData = IncomeTaxUserData(
    Some(employmentsModel)
  )

  lazy val employmentsModel: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(),
    hmrcExpenses = Some(employmentExpenses),
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
        submittedOn = (s"${taxYearEOY - 1}-02-12"),
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

  lazy val expenses: Expenses = Expenses(
    Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)
  )
  lazy val employmentExpenses: EmploymentExpenses = EmploymentExpenses(
    submittedOn = None,
    dateIgnored = None,
    totalExpenses = None,
    expenses = Some(expenses)
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

  val amendBenefits: Benefits = Benefits(
    accommodation = Some(10),
    assets = Some(10),
    assetTransfer = Some(10),
    beneficialLoan = Some(10),
    car = Some(10),
    carFuel = Some(10),
    educationalServices = Some(10),
    entertaining = Some(10),
    expenses = Some(10),
    medicalInsurance = Some(10),
    telephone = Some(10),
    service = Some(10),
    taxableExpenses = Some(10),
    van = Some(10),
    vanFuel = Some(10),
    mileage = Some(10),
    nonQualifyingRelocationExpenses = Some(10),
    nurseryPlaces = Some(10),
    otherItems = Some(10),
    paymentsOnEmployeesBehalf = Some(10),
    personalIncidentalExpenses = Some(10),
    qualifyingRelocationExpenses = Some(10),
    employerProvidedProfessionalSubscriptions = Some(10),
    employerProvidedServices = Some(10),
    incomeTaxPaidByDirector = Some(10),
    travelAndSubsistence = Some(10),
    vouchersAndCreditCards = Some(10),
    nonCash = Some(10)
  )
}
