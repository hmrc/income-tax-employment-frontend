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

package services

import models.errors.MissingAgentClientDetails
import models.session.SessionData
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.IM_A_TEAPOT
import play.api.mvc.Request
import play.api.test.FakeRequest
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import support.mocks.{MockSessionDataConnector, ScalamockAppConfig}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class SessionDataServiceSpec extends UnitTest with ScalamockAppConfig with MockSessionDataConnector {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testService: SessionDataService = new SessionDataService(
    sessionDataConnector = mockSessionDataConnector,
    config = scalamockAppConfig
  )

  val dummyError: APIErrorModel = APIErrorModel(IM_A_TEAPOT, APIErrorBodyModel("", ""))
  val sessionData: SessionData = SessionData(aUser.sessionId, aUser.mtditid, aUser.nino, None)

  ".getSessionData()" when {

    "V&C Session Data service feature is enabled" when {

      "the call to retrieve session data fails" when {

        "the retrieval of fallback data from the session cookie also fails to find the clients details" should {

          "return an error when fallback returns no data" in {
            mockSessionServiceEnabled(true)
            mockGetSessionDataFromSessionStore(Left(dummyError))

            implicit val request: Request[_] = FakeRequest()

            val result: MissingAgentClientDetails = intercept[MissingAgentClientDetails](await(testService.getSessionData(aUser.sessionId)(request, hc)))
            result.message shouldBe "Session Data service and Session Cookie both returned empty data"
          }
        }

        "the fallback is successful and retrieves client MTDITID and NINO from the Session Cookie" should {

          "return session data" in {
            mockSessionServiceEnabled(true)
            mockGetSessionDataFromSessionStore(Left(dummyError))

            implicit val request: Request[_] = FakeRequest()
              .withSession(
                ("ClientNino", "AA111111A"),
                ("ClientMTDID", "12345678")
              )

            val result: SessionData = await(testService.getSessionData(aUser.sessionId)(request, hc))
            result shouldBe SessionData(sessionId = aUser.sessionId, mtditid = "12345678", nino = "AA111111A")
          }
        }
      }

      "the call to retrieve session data from the downstream V&C service is successful" should {

        "return the session data" in {
          mockSessionServiceEnabled(true)
          mockGetSessionDataFromSessionStore(Right(Some(sessionData)))

          implicit val request: Request[_] = FakeRequest()

          val result: SessionData = await(testService.getSessionData(aUser.sessionId)(request, hc))
          result shouldBe sessionData
        }
      }
    }

    "V&C Session Data service feature is DISABLED" when {

      "the retrieval of fallback data from the session cookie also fails to find the clients details" should {

        "return an error when fallback returns no data" in {
          mockSessionServiceEnabled(false)

          implicit val request: Request[_] = FakeRequest()

          val result: MissingAgentClientDetails = intercept[MissingAgentClientDetails](await(testService.getSessionData(aUser.sessionId)(request, hc)))
          result.message shouldBe "Session Data service and Session Cookie both returned empty data"
        }
      }

      "the fallback is successful and retrieves client MTDITID and NINO from the Session Cookie" should {

        "return session data" in {
          mockSessionServiceEnabled(false)

          implicit val request: Request[_] = FakeRequest()
            .withSession(
              ("ClientNino", "AA111111A"),
              ("ClientMTDID", "12345678")
            )

          val result: SessionData = await(testService.getSessionData(aUser.sessionId)(request, hc))
          result shouldBe SessionData(sessionId = aUser.sessionId, mtditid = "12345678", nino = "AA111111A")
        }
      }
    }
  }
}
