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
import akka.stream.{ActorMaterializer, Materializer}
import config.AppConfig
import controllers.predicates.AuthorisedAction
import helpers.WireMockHelper
import models.User
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import play.api.{Application, Environment, Mode}
import services.AuthService
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.authErrorPages.AgentAuthErrorPageView

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait IntegrationTest extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with WireMockHelper with BeforeAndAfterAll {

  val nino = "A123456A"
  val mtditid = "1234567890"

  implicit lazy val user: User[AnyContent] = new User[AnyContent](mtditid, None, nino)

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid)

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = ActorMaterializer()

  val startUrl = s"http://localhost:$port/income-through-software/return/personal-income"

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  def config: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission-frontend.host" -> wiremockHost,
    "microservice.services.income-tax-submission-frontend.port" -> wiremockPort.toString,
    "income-tax-submission-frontend.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort.toString,
    "microservice.services.income-tax-dividends.host" -> wiremockHost,
    "microservice.services.income-tax-dividends.port" -> wiremockPort.toString,
    "microservice.services.income-tax-interest.host" -> wiremockHost,
    "microservice.services.income-tax-interest.port" -> wiremockPort.toString,
    "microservice.services.income-tax-gift-aid.host" -> wiremockHost,
    "microservice.services.income-tax-gift-aid.port" -> wiremockPort.toString,
    "signIn.url" -> s"/auth-login-stub/gg-sign-in",
  )

  lazy val agentAuthErrorPage: AgentAuthErrorPageView = app.injector.instanceOf[AgentAuthErrorPageView]

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
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
    ConfidenceLevel.L300,
    ConfidenceLevel.L500
  )

  def authService(stubbedRetrieval: Future[_], acceptedConfidenceLevel: Seq[ConfidenceLevel]): AuthService = new AuthService(
    new MockAuthConnector(stubbedRetrieval, acceptedConfidenceLevel)
  )

  def authAction(
                  stubbedRetrieval: Future[_],
                  acceptedConfidenceLevel: Seq[ConfidenceLevel] = Seq.empty[ConfidenceLevel]
                ): AuthorisedAction = new AuthorisedAction(
    appConfig,
    agentAuthErrorPage
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

}
