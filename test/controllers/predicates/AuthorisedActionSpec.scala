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

package controllers.predicates

import common.{EnrolmentIdentifiers, EnrolmentKeys}
import models.User
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedActionSpec extends UnitTest {

  val auth: AuthorisedAction = authorisedAction

  ".enrolmentGetIdentifierValue" should {

    "return the value for the given identifier" in {
      val returnValue = "anIdentifierValue"
      val returnValueAgent = "anAgentIdentifierValue"

      val enrolments = Enrolments(Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, returnValue)), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, returnValueAgent)), "Activated")
      ))

      auth.enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments) shouldBe Some(returnValue)
      auth.enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) shouldBe Some(returnValueAgent)
    }

    "return a None" when {
      val key = "someKey"
      val identifierKey = "anIdentifier"
      val returnValue = "anIdentifierValue"

      val enrolments = Enrolments(Set(Enrolment(key, Seq(EnrolmentIdentifier(identifierKey, returnValue)), "someState")))


      "the given identifier cannot be found" in {
        auth.enrolmentGetIdentifierValue(key, "someOtherIdentifier", enrolments) shouldBe None
      }

      "the given key cannot be found" in {
        auth.enrolmentGetIdentifierValue("someOtherKey", identifierKey, enrolments) shouldBe None
      }

    }

  }

  ".individualAuthentication" should {

    "perform the block action" when {

      "the correct enrolment exist" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L200))
          auth.individualAuthentication[AnyContent](block)(fakeRequest, headerCarrierWithSession)
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

      "the session id does not exist in the headers" which {
          val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
          val mtditid = "AAAAAA"
          val enrolments = Enrolments(Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
            Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")
          ))

          lazy val result: Future[Result] = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, allEnrolments and confidenceLevel, *, *)
              .returning(Future.successful(enrolments and ConfidenceLevel.L200))
            auth.individualAuthentication[AnyContent](block)(fakeRequest, emptyHeaderCarrier)
          }

          "returns an SEE_OTHER status" in {
            status(result) shouldBe SEE_OTHER
          }
      }

      "the nino enrolment is missing" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val enrolments = Enrolments(Set())

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L200))
          auth.individualAuthentication[AnyContent](block)(fakeRequest, headerCarrierWithSession)
        }

        "returns a forbidden" in {
          status(result) shouldBe SEE_OTHER
        }
      }

      "the individual enrolment is missing but there is a nino" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val nino = "AA123456A"
        val enrolments = Enrolments(Set(Enrolment("HMRC-NI", Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L200))
          auth.individualAuthentication[AnyContent](block)(fakeRequest, headerCarrierWithSession)
        }

        "returns an Unauthorised" in {
          status(result) shouldBe SEE_OTHER
        }
        "returns an redirect to the correct page" in {
          redirectUrl(result) shouldBe "/income-through-software/return/employment-income/error/you-need-to-sign-up"
        }
      }

    }

    "return the user to IV Uplift" when {

      "the confidence level is below minimum" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "1234567890"
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, "AA123456A")), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L50))
          auth.individualAuthentication[AnyContent](block)(fakeRequest, headerCarrierWithSession)
        }

        "has a status of 303" in {
          status(result) shouldBe SEE_OTHER
        }

        "redirects to the iv url" in {
          await(result).header.headers("Location") shouldBe "/income-through-software/return/iv-uplift"
        }
      }
    }
  }

  ".agentAuthenticated" should {

    val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(s"${user.mtditid} ${user.arn.get}"))

    "perform the block action" when {

      "the agent is authorised for the given user" which {

        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
          Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
        ))

        lazy val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returning(Future.successful(enrolments))

          auth.agentAuthentication(block)(fakeRequestWithMtditidAndNino, headerCarrierWithSession)
        }

        "has a status of OK" in {
          status(result) shouldBe OK
        }

        "has the correct body" in {
          bodyOf(result) shouldBe "1234567890 0987654321"
        }
      }
    }

    "return an SEE_OTHER" when {

      "the agent does not have a session id" which {

        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
          Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
        ))

        lazy val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returning(Future.successful(enrolments))

          auth.agentAuthentication(block)(fakeRequestWithMtditidAndNino, emptyHeaderCarrier)
        }

        "has a status of SEE_OTHER" in {
          status(result) shouldBe SEE_OTHER
        }
      }

      "the authorisation service returns an AuthorisationException exception" in {
        object AuthException extends AuthorisationException("Some reason")

        lazy val result = {
          mockAuthReturnException(AuthException)
          auth.agentAuthentication(block)(fakeRequestWithMtditidAndNino, headerCarrierWithSession)
        }
        status(result) shouldBe SEE_OTHER
      }

    }

    "redirect to the sign in page" when {

      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        lazy val result = {
          mockAuthReturnException(NoActiveSession)
          auth.agentAuthentication(block)(fakeRequestWithMtditidAndNino, headerCarrierWithSession)
        }

        status(result) shouldBe SEE_OTHER
      }
    }

    "return a redirect" when {

      "the user does not have an enrolment for the agent" in {
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated")
        ))

        lazy val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returning(Future.successful(enrolments))
          auth.agentAuthentication(block)(fakeRequestWithMtditidAndNino, headerCarrierWithSession)
        }
        status(result) shouldBe SEE_OTHER
      }
    }

  }

  ".invokeBlock" should {

    lazy val block: User[AnyContent] => Future[Result] = user =>
      Future.successful(Ok(s"mtditid: ${user.mtditid}${user.arn.fold("")(arn => " arn: " + arn)}"))

    "perform the block action" when {

      "the user is successfully verified as an agent" which {

        lazy val result = {
          mockAuthAsAgent()
          auth.invokeBlock(fakeRequestWithMtditidAndNino, block)
        }

        "should return an OK(200) status" in {
          status(result) shouldBe OK
          bodyOf(result) shouldBe "mtditid: 1234567890 arn: 0987654321"
        }
      }

      "the user is successfully verified as an individual" in {

        lazy val result = {
          mockAuth(Some("AA123456A"))
          auth.invokeBlock(fakeRequest, block)
        }

        status(result) shouldBe OK

        bodyOf(result) shouldBe "mtditid: 1234567890"
      }
    }

    "return a redirect" when {

      "the authorisation service returns an AuthorisationException exception" in {
        object AuthException extends AuthorisationException("Some reason")

        lazy val result = {
          mockAuthReturnException(AuthException)
          auth.invokeBlock(fakeRequest, block)
        }
        status(result) shouldBe SEE_OTHER
      }

      "there is no MTDITID value in session for an agent" in {
        lazy val result = {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.affinityGroup, *, *)
            .returning(Future.successful(Some(AffinityGroup.Agent)))

          auth.invokeBlock(fakeRequestWithNino, block)
        }
        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe "/report-quarterly/income-and-expenses/view/agents/client-utr"
      }

    }

    "redirect to the sign in page" when {
      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        lazy val result = {
          mockAuthReturnException(NoActiveSession)
          auth.invokeBlock(fakeRequest, block)
        }

        status(result) shouldBe SEE_OTHER
      }
    }
  }

}

