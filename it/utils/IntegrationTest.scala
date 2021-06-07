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

package utils

import akka.actor.ActorSystem
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.SessionValues
import config.AppConfig
import controllers.predicates.AuthorisedAction
import helpers.{PlaySessionCookieBaker, WireMockHelper}
import models.IncomeTaxUserData
import models.employment.{AllEmploymentData, EmploymentData, EmploymentSource, Pay}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import play.api.{Application, Environment, Mode}
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import views.html.authErrorPages.AgentAuthErrorPageView

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait IntegrationTest extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with WireMockHelper with BeforeAndAfterAll {

  val nino = "AA123456A"
  val mtditid = "1234567890"
  val sessionId = "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"
  val taxYear = 2022

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid)

  implicit val actorSystem: ActorSystem = ActorSystem()

  def wsClient: WSClient = app.injector.instanceOf[WSClient]

  def appUrl(port: Int) = s"http://localhost:$port/income-through-software/return/employment-income"

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  def config: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission-frontend.host" -> wiremockHost,
    "microservice.services.income-tax-submission-frontend.port" -> wiremockPort.toString,
    "income-tax-submission-frontend.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort.toString,
    "microservice.services.income-tax-submission.host" -> wiremockHost,
    "microservice.services.income-tax-submission.port" -> wiremockPort.toString,
    "view-and-change.baseUrl" -> s"http://$wiremockHost:$wiremockPort",
    "signIn.url" -> s"/auth-login-stub/gg-sign-in",
  )

  def externalConfig: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission.host" -> "127.0.0.1",
    "microservice.services.income-tax-submission.port" -> wiremockPort.toString,
    "metrics.enabled" -> "false"
  )

  lazy val agentAuthErrorPage: AgentAuthErrorPageView = app.injector.instanceOf[AgentAuthErrorPageView]

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build

  lazy val appWithFakeExternalCall: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(externalConfig)
    .build

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]


  val defaultAcceptedConfidenceLevels = Seq(
    ConfidenceLevel.L200,
    ConfidenceLevel.L500
  )

  def authService(stubbedRetrieval: Future[_], acceptedConfidenceLevel: Seq[ConfidenceLevel]): AuthService = new AuthService(
    new MockAuthConnector(stubbedRetrieval, acceptedConfidenceLevel)
  )

  def authAction(
                  stubbedRetrieval: Future[_],
                  acceptedConfidenceLevel: Seq[ConfidenceLevel] = Seq.empty[ConfidenceLevel]
                ): AuthorisedAction = new AuthorisedAction(
    appConfig
  )(
    authService(stubbedRetrieval, if (acceptedConfidenceLevel.nonEmpty) {
      acceptedConfidenceLevel
    } else {
      defaultAcceptedConfidenceLevels
    }),
    mcc
  )

  def successfulRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L200
  )

  def insufficientConfidenceRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L50
  )

  def incorrectCredsRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L200
  )

  def playSessionCookies(taxYear: Int): String = PlaySessionCookieBaker.bakeSessionCookie(Map(
    SessionValues.TAX_YEAR -> taxYear.toString,
    SessionKeys.sessionId -> sessionId,
    SessionValues.CLIENT_NINO -> "AA123456A",
    SessionValues.CLIENT_MTDITID -> "1234567890"
  ))

  def userData(allData: AllEmploymentData): IncomeTaxUserData = IncomeTaxUserData(Some(allData))

  def userDataStub(userData: IncomeTaxUserData, nino: String, taxYear: Int): StubMapping ={

    stubGetWithHeadersCheck(
      s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", OK,
      Json.toJson(userData).toString(),("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))
  }

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  lazy val employmentsModel: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      EmploymentSource(
        employmentId = "001",
        employerName = "maggie",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some("2020-03-11"),
        dateIgnored = Some("2020-04-04T01:01:01Z"),
        submittedOn = Some("2020-01-04T05:01:01Z"),
        employmentData = Some(EmploymentData(
          submittedOn = ("2020-02-12"),
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
    hmrcExpenses = None,
    customerEmploymentData = Seq(),
    customerExpenses = None
  )

}
