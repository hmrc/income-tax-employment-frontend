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

package actions

import common.{EnrolmentIdentifiers, EnrolmentKeys, SessionValues}
import models.AuthorisationRequest
import play.api.Play.materializer
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, stubMessagesControllerComponents}
import services.AuthService
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.mocks.MockAuthorisedAction
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedActionSpec extends ControllerUnitTest with MockAuthorisedAction {

  private implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")))
  private val fakeRequestWithMtditidAndNino: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionValues.CLIENT_MTDITID -> "1234567890",
    SessionValues.CLIENT_NINO -> "AA123456A",
    SessionValues.TAX_YEAR -> taxYear.toString,
    SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(",")
  ).withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")
  private val fakeRequestWithNino: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(SessionValues.CLIENT_NINO -> "AA123456A")

  private implicit val authService: AuthService = new AuthService(mockAuthConnector)

  private val underTest = new AuthorisedAction(appConfig, authService)(stubMessagesControllerComponents())

  ".enrolmentGetIdentifierValue" should {
    "return the value for the given identifier" in {
      val returnValue = "anIdentifierValue"
      val returnValueAgent = "anAgentIdentifierValue"

      val enrolments = Enrolments(Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, returnValue)), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, returnValueAgent)), "Activated")
      ))

      underTest.enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments) shouldBe Some(returnValue)
      underTest.enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) shouldBe Some(returnValueAgent)
    }

    "return a None" when {
      val key = "someKey"
      val identifierKey = "anIdentifier"
      val returnValue = "anIdentifierValue"

      val enrolments = Enrolments(Set(Enrolment(key, Seq(EnrolmentIdentifier(identifierKey, returnValue)), "someState")))

      "the given identifier cannot be found" in {
        underTest.enrolmentGetIdentifierValue(key, "someOtherIdentifier", enrolments) shouldBe None
      }

      "the given key cannot be found" in {
        underTest.enrolmentGetIdentifierValue("someOtherKey", identifierKey, enrolments) shouldBe None
      }
    }
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

        val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L200))

          await(underTest.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, headerCarrierWithSession))
        }

        "returns an OK status" in {
          result.header.status shouldBe OK
        }

        "returns a body of the mtditid" in {
          await(result.body.consumeData.map(_.utf8String)) shouldBe mtditid
        }
      }
    }

    "return a redirect" when {
      "the session id does not exist in the headers" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, aUser.nino)), "Activated")
        ))

        val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L200))

          underTest.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, HeaderCarrier())
        }

        "returns an SEE_OTHER status" in {
          status(result) shouldBe SEE_OTHER
        }
      }

      "the nino enrolment is missing" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val enrolments = Enrolments(Set())

        val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L200))

          underTest.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, headerCarrierWithSession)
        }

        "returns a forbidden" in {
          status(result) shouldBe SEE_OTHER
        }
      }

      "the individual enrolment is missing but there is a nino" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val nino = "AA123456A"
        val enrolments = Enrolments(Set(Enrolment("HMRC-NI", Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")))

        val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L200))

          await(underTest.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, headerCarrierWithSession))
        }

        "returns an Unauthorised" in {
          result.header.status shouldBe SEE_OTHER
        }
        "returns an redirect to the correct page" in {
          result.header.headers("Location") shouldBe "/error/you-need-to-sign-up"
        }
      }
    }

    "return the user to IV Uplift" when {
      "the confidence level is below minimum" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid = "1234567890"
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, "AA123456A")), "Activated")
        ))

        val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L50))

          await(underTest.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, headerCarrierWithSession))
        }

        "has a status of 303" in {
          result.header.status shouldBe SEE_OTHER
        }

        "redirects to the iv url" in {
          result.header.headers("Location") shouldBe "/update-and-submit-income-tax-return/iv-uplift"
        }
      }
    }
  }

  ".agentAuthenticated" should {
    val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(s"${request.user.mtditid} ${request.user.arn.get}"))

    "perform the block action" when {
      "the agent is authorised for the given user" which {
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
          Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
        ))

        val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returning(Future.successful(enrolments))

          await(underTest.agentAuthentication(block)(fakeRequestWithMtditidAndNino, headerCarrierWithSession))
        }

        "has a status of OK" in {
          result.header.status shouldBe OK
        }

        "has the correct body" in {
          await(result.body.consumeData.map(_.utf8String)) shouldBe "1234567890 0987654321"
        }
      }
    }

    "return an SEE_OTHER" when {
      "the agent does not have a session id" which {
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
          Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
        ))

        val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returning(Future.successful(enrolments))

          underTest.agentAuthentication(block)(fakeRequestWithMtditidAndNino, HeaderCarrier())
        }

        "has a status of SEE_OTHER" in {
          status(result) shouldBe SEE_OTHER
        }
      }

      "the authorisation service returns an AuthorisationException exception" in {
        object AuthException extends AuthorisationException("Some reason")
        val result = {
          mockAuthReturnException(AuthException)
          mockAuthorisedAction.agentAuthentication(block)(fakeRequestWithMtditidAndNino, headerCarrierWithSession)
        }

        status(result) shouldBe SEE_OTHER
      }
    }

    "redirect to the sign in page" when {
      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        lazy val result = {
          mockAuthReturnException(NoActiveSession)

          underTest.agentAuthentication(block)(fakeRequestWithMtditidAndNino, headerCarrierWithSession)
        }

        status(result) shouldBe SEE_OTHER
      }
    }

    "return a redirect" when {
      "the user does not have an enrolment for the agent" in {
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated")
        ))
        val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returning(Future.successful(enrolments))

          underTest.agentAuthentication(block)(fakeRequestWithMtditidAndNino, headerCarrierWithSession)
        }

        status(result) shouldBe SEE_OTHER
      }
    }
  }

  ".invokeBlock" should {
    lazy val block: AuthorisationRequest[AnyContent] => Future[Result] = request =>
      Future.successful(Ok(s"mtditid: ${request.user.mtditid}${request.user.arn.fold("")(arn => " arn: " + arn)}"))

    "perform the block action" when {
      "the user is successfully verified as an agent" which {
        lazy val result = {
          mockAuthAsAgent()

          await(underTest.invokeBlock(fakeRequestWithMtditidAndNino, block))
        }

        "should return an OK(200) status" in {
          result.header.status shouldBe OK
          await(result.body.consumeData.map(_.utf8String)) shouldBe "mtditid: 1234567890 arn: 0987654321"
        }
      }

      "the user is successfully verified as an individual" in {
        val result = {
          mockAuth(Some("AA123456A"))
          await(underTest.invokeBlock(fakeIndividualRequest, block))
        }

        result.header.status shouldBe OK
        await(result.body.consumeData.map(_.utf8String)) shouldBe "mtditid: 1234567890"
      }
    }

    "return a redirect" when {
      "the authorisation service returns an AuthorisationException exception" in {
        object AuthException extends AuthorisationException("Some reason")

        val result = {
          mockAuthReturnException(AuthException)

          underTest.invokeBlock(fakeRequest, block)
        }

        status(result) shouldBe SEE_OTHER
      }

      "there is no MTDITID value in session for an agent" in {
        val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.affinityGroup, *, *)
            .returning(Future.successful(Some(AffinityGroup.Agent)))

          await(underTest.invokeBlock(fakeRequestWithNino, block))
        }

        result.header.status shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe "/report-quarterly/income-and-expenses/view/agents/client-utr"
      }
    }

    "redirect to the sign in page" when {
      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        lazy val result = {
          mockAuthReturnException(NoActiveSession)

          underTest.invokeBlock(fakeRequest, block)
        }

        status(result) shouldBe SEE_OTHER
      }
    }
  }
}
