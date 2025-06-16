/*
 * Copyright 2025 HM Revenue & Customs
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

package actions

import common.SessionValues.{CLIENT_MTDITID, CLIENT_NINO}
import common.{EnrolmentIdentifiers, EnrolmentKeys, SessionValues}
import config.AppConfig
import models.AuthorisationRequest
import models.errors.MissingAgentClientDetails
import models.session.SessionData
import org.scalamock.handlers.{CallHandler0, CallHandler4}
import org.scalamock.scalatest.MockFactory
import play.api.Play.materializer
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import services.AuthService
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.mocks.{MockErrorHandler, MockSessionDataService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedActionSpec extends ControllerUnitTest
  with MockFactory with MockSessionDataService with MockErrorHandler {

  val mtdItId: String = "1234567890"
  val arn: String = "0987654321"
  val nino: String = "AA123456A"
  val sessionData: SessionData = SessionData(aUser.sessionId, mtdItId, nino)

  private implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(aUser.sessionId)))
  private implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private implicit val authService: AuthService = new AuthService(mockAuthConnector)
  private val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()
  private val fakeRequestWithMtditidAndNino: FakeRequest[AnyContentAsEmpty.type] = fakeAgentRequest
    .withHeaders(newHeaders = "X-Session-ID" -> aUser.sessionId)
    .withSession(CLIENT_MTDITID -> mtdItId, CLIENT_NINO -> nino)

  private val underTest: AuthorisedAction = new AuthorisedAction(
    appConfig,
    mockErrorHandler,
    authService,
    mockSessionDataService
  )(stubMessagesControllerComponents())

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  private def mockAuthAsAgent(): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {
    val enrolments: Enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
    ))
    val agentRetrievals: Some[AffinityGroup] = Some(AffinityGroup.Agent)

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(agentRetrievals))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments, *, *)
      .returning(Future.successful(enrolments))
  }

  private def mockAuth(nino: Option[String]): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {
    val enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
    ) ++ nino.fold(Seq.empty[Enrolment])(unwrappedNino =>
      Seq(Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, unwrappedNino)), "Activated"))
    ))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(Some(AffinityGroup.Individual)))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
      .returning(Future.successful(enrolments and ConfidenceLevel.L250))
  }

  trait AgentTest {
    val baseUrl = "/update-and-submit-income-tax-return/employment-income"

    val validHeaderCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionId")))

    val testBlock: AuthorisationRequest[AnyContent] => Future[Result] = user => Future.successful(Ok(s"${user.user.mtditid} ${user.user.arn.get}"))

    val mockAppConfig: AppConfig = mock[AppConfig]

    val viewAndChangeUrl: String = "/report-quarterly/income-and-expenses/view/agents/client-utr"
    val signInUrl: String = s"$baseUrl/signIn"

    def redirectUrl(awaitable: Future[Result]): String = {
      await(awaitable).header.headers.getOrElse("Location", "/")
    }

    def primaryAgentPredicate(mtdId: String): Predicate =
      Enrolment("HMRC-MTD-IT")
        .withIdentifier("MTDITID", mtdId)
        .withDelegatedAuthRule("mtd-it-auth")

    def secondaryAgentPredicate(mtdId: String): Predicate =
      Enrolment("HMRC-MTD-IT-SUPP")
        .withIdentifier("MTDITID", mtdId)
        .withDelegatedAuthRule("mtd-it-auth-supp")

    val primaryAgentEnrolment: Enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
    ))

    val supportingAgentEnrolment: Enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Supporting, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
    ))

    def mockAuthReturnException(exception: Exception,
                                predicate: Predicate): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] =
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(predicate, *, *, *)
        .returning(Future.failed(exception))

    def mockAuthReturn(enrolments: Enrolments, predicate: Predicate): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] =
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(predicate, *, *, *)
        .returning(Future.successful(enrolments))

    def mockSignInUrl(): CallHandler0[String] =
      (mockAppConfig.signInUrl _: () => String)
        .expects()
        .returning(signInUrl)
        .anyNumberOfTimes()

    def mockViewAndChangeUrl(): CallHandler0[String] =
      (mockAppConfig.viewAndChangeEnterUtrUrl _: () => String)
        .expects()
        .returning(viewAndChangeUrl)
        .anyNumberOfTimes()

    def testAuth: AuthorisedAction = {
      mockViewAndChangeUrl()
      mockSignInUrl()

      new AuthorisedAction(
        appConfig = mockAppConfig,
        authService = authService,
        errorHandler = mockErrorHandler,
        sessionDataService = mockSessionDataService
      )(
        mcc = stubMessagesControllerComponents()
      )
    }

    lazy val fakeRequestWithMtditidAndNino: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
      SessionValues.TAX_YEAR -> "2022",
      SessionValues.CLIENT_MTDITID -> mtdItId,
      SessionValues.CLIENT_NINO -> nino
    )
  }

  ".individualAuthentication" should {
    "perform the block action" when {
      "the correct enrolment exist" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, aUser.nino)), "Activated")
        ))
        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          underTest.individualAuthentication[AnyContent](block, AffinityGroup.Individual, aUser.sessionId)(fakeIndividualRequest, headerCarrierWithSession)
        }

        "returns an OK status" in {
          status(result) shouldBe OK
        }

        "returns a body of the mtditid" in {
          bodyOf(result) shouldBe mtditid
        }
      }
    }

    "return a redirect" when {
      "the nino enrolment is missing" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val enrolments = Enrolments(Set())
        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          underTest.individualAuthentication[AnyContent](block, AffinityGroup.Individual, aUser.sessionId)(fakeIndividualRequest, headerCarrierWithSession)
        }

        "returns a forbidden" in {
          status(result) shouldBe SEE_OTHER
        }
      }

      "the individual enrolment is missing but there is a nino" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val enrolments = Enrolments(Set(Enrolment("HMRC-NI", Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")))
        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          underTest.individualAuthentication[AnyContent](block, AffinityGroup.Individual, aUser.sessionId)(fakeIndividualRequest, headerCarrierWithSession)
        }

        "returns an Unauthorised" in {
          status(result) shouldBe SEE_OTHER
        }

        "returns an redirect to the correct page" in {
          await(result).header.headers.getOrElse("Location", "/") shouldBe "/update-and-submit-income-tax-return/employment-income/error/you-need-to-sign-up"
        }
      }
    }

    "return the user to IV Uplift" when {
      "the confidence level is below minimum" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid = mtdItId
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")
        ))
        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L50))
          underTest.individualAuthentication[AnyContent](block, AffinityGroup.Individual, aUser.sessionId)(fakeIndividualRequest, headerCarrierWithSession)
        }

        "has a status of 303" in {
          status(result) shouldBe SEE_OTHER
        }

        "redirects to the iv url" in {
          await(result).header.headers("Location") shouldBe "/update-and-submit-income-tax-return/iv-uplift"
        }
      }
    }
  }

  ".agentAuthenticated" when {
    "session data for Client MTDITID and/or NINO is missing" should {
      "return a redirect to View and Change service" in new AgentTest {
        mockGetSessionDataException(aUser.sessionId)(MissingAgentClientDetails("No session data"))
        val result: Future[Result] = testAuth.agentAuthentication(testBlock, aUser.sessionId)(
          request = FakeRequest().withSession(fakeRequest.session.data.toSeq :_*),
          hc = emptyHeaderCarrier
        )

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe viewAndChangeUrl
      }
    }

    "NINO and MTD IT ID are present in the session" which {
      "results in a NoActiveSession error to be returned from Auth" should {
        "return a redirect to the login page" in new AgentTest {
          mockGetSessionData(aUser.sessionId)(sessionData)
          mockAuthReturnException(BearerTokenExpired(), primaryAgentPredicate(mtdItId))

          val result: Future[Result] = testAuth.agentAuthentication(testBlock, aUser.sessionId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe s"$baseUrl/signIn"
        }
      }

      "results in an Exception other than an AuthException error being returned for Primary Agent check" should {
        "render an ISE page" in new AgentTest {
          mockGetSessionData(aUser.sessionId)(sessionData)
          mockAuthReturnException(new Exception("bang"), primaryAgentPredicate(mtdItId))
          mockInternalServerError(InternalServerError("An unexpected error occurred"))

          val result: Future[Result] = testAuth.agentAuthentication(testBlock, aUser.sessionId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe INTERNAL_SERVER_ERROR
          bodyOf(result) shouldBe "An unexpected error occurred"
        }
      }

      "Primary agent has insufficient enrolments results in an AuthorisationException error being returned from Auth" should {
        "render an ISE page when secondary agent auth call also fails with non-Auth exception" in new AgentTest {
          mockGetSessionData(aUser.sessionId)(sessionData)
          mockAuthReturnException(InsufficientEnrolments(), primaryAgentPredicate(mtdItId))
          mockAuthReturnException(new Exception("bang"), secondaryAgentPredicate(mtdItId))
          mockInternalServerError(InternalServerError("An unexpected error occurred"))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, aUser.sessionId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe INTERNAL_SERVER_ERROR
          bodyOf(result) shouldBe "An unexpected error occurred"
        }

        "return a redirect to the agent error page when secondary agent auth call also fails" in new AgentTest {
          mockGetSessionData(aUser.sessionId)(sessionData)
          mockAuthReturnException(InsufficientEnrolments(), primaryAgentPredicate(mtdItId))
          mockAuthReturnException(InsufficientEnrolments(), secondaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, aUser.sessionId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe s"$baseUrl/error/you-need-client-authorisation"
        }

        "return a redirect to the secondary agent not authorised page when a supporting agent has correct credentials" in new AgentTest {
          mockGetSessionData(aUser.sessionId)(sessionData)
          mockAuthReturnException(InsufficientEnrolments(), primaryAgentPredicate(mtdItId))
          mockAuthReturn(supportingAgentEnrolment, secondaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, aUser.sessionId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe s"$baseUrl/error/supporting-agent-not-authorised"
        }
      }

      "results in successful authorisation for a primary agent" should {
        "return a redirect to You Need Agent Services page when an ARN cannot be found" in new AgentTest {
          mockGetSessionData(aUser.sessionId)(sessionData)
          val primaryAgentEnrolmentNoArn: Enrolments = Enrolments(Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
            Enrolment(EnrolmentKeys.Agent, Seq.empty, "Activated")
          ))

          mockAuthReturn(primaryAgentEnrolmentNoArn, primaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, aUser.sessionId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe s"$baseUrl/error/you-need-agent-services-account"
        }

        "invoke block when the user is properly authenticated" in new AgentTest {
          mockGetSessionData(aUser.sessionId)(sessionData)
          mockAuthReturn(primaryAgentEnrolment, primaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, aUser.sessionId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe OK
          bodyOf(result) shouldBe s"$mtdItId $arn"
        }
      }
    }
  }

  ".invokeBlock" should {
    lazy val block: AuthorisationRequest[AnyContent] => Future[Result] = request =>
      Future.successful(Ok(s"mtditid: ${request.user.mtditid}${request.user.arn.fold("")(arn => " arn: " + arn)}"))

    "perform the block action" when {
      "the user is successfully verified as an agent" which {
        lazy val result = {
          mockGetSessionData(aUser.sessionId)(sessionData)
          mockAuthAsAgent()
          underTest.invokeBlock(fakeRequestWithMtditidAndNino, block)
        }

        "should return an OK(200) status" in {
          status(result) shouldBe OK
          bodyOf(result) shouldBe "mtditid: 1234567890 arn: 0987654321"
        }
      }

      "the user is successfully verified as an individual" in {
        lazy val result = {
          mockAuth(Some(nino))
          underTest.invokeBlock(fakeIndividualRequest, block)
        }

        status(result) shouldBe OK
        bodyOf(result) shouldBe "mtditid: 1234567890"
      }
    }

    "return a redirect" when {
      "the authorisation service returns an AuthorisationException exception" in {
        lazy val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returning(Future.failed(InsufficientEnrolments()))

          underTest.invokeBlock(fakeAgentRequest, block)
        }

        status(result) shouldBe SEE_OTHER
      }

      "there is no MTDITID value in session" in {
        val fakeRequestWithNino = fakeIndividualRequest.withSession(CLIENT_NINO -> nino)
        lazy val result = {
          mockGetSessionDataException(aUser.sessionId)(MissingAgentClientDetails("No session data"))
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.affinityGroup, *, *)
            .returning(Future.successful(Some(AffinityGroup.Agent)))

          underTest.invokeBlock(fakeRequestWithNino, block)
        }

        status(result) shouldBe SEE_OTHER
        await(result).header.headers.getOrElse("Location", "/") shouldBe "/report-quarterly/income-and-expenses/view/agents/client-utr"
      }
    }

    "redirect to the sign in page" when {
      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        lazy val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returning(Future.failed(NoActiveSession))
          underTest.invokeBlock(fakeIndividualRequest, block)
        }

        status(result) shouldBe SEE_OTHER
      }
    }

    "render ISE" when {
      "an unexpected exception is caught that is not related to Authorisation" in {

        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returning(Future.failed(new Exception("bang")))

        mockInternalServerError(InternalServerError("An unexpected error occurred"))

        val result = underTest.invokeBlock(fakeAgentRequest, block)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        bodyOf(result) shouldBe "An unexpected error occurred"
      }
    }
  }
}
