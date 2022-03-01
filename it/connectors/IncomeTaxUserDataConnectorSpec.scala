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

package connectors

import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.mocks.MockAppConfig
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.ConnectorIntegrationTest

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IncomeTaxUserDataConnectorSpec extends ConnectorIntegrationTest {

  private val mtditid = "some-mtditid"
  private val sessionId = "some-sessionId"
  private val nino = "some-nino"

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  private lazy val underTest = new IncomeTaxUserDataConnector(httpClient, new MockAppConfig().config())
  private lazy val externalConnector = new IncomeTaxUserDataConnector(httpClient, new MockAppConfig().config())

  "IncomeTaxUserDataConnector" should {
    "Return a success result" when {
      "submission returns a 204" in {
        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", NO_CONTENT,
          "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.get(nino, taxYear), Duration.Inf) shouldBe Right(IncomeTaxUserData())
      }

      "submission returns a 200" in {
        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", OK,
          Json.toJson(anIncomeTaxUserData).toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.get(nino, taxYear), Duration.Inf) shouldBe Right(anIncomeTaxUserData)
      }
    }

    "Return an error result" when {
      "the stub isn't matched due to the call being external as the headers won't be passed along" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", OK,
          Json.toJson(anIncomeTaxUserData).toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(externalConnector.get(nino, taxYear)(hc), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 200 but invalid json" in {
        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", OK,
          Json.toJson("""{"invalid": true}""").toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.get(nino, taxYear), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {
        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", INTERNAL_SERVER_ERROR,
          """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.get(nino, taxYear), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {
        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", SERVICE_UNAVAILABLE,
          """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.get(nino, taxYear), Duration.Inf) shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {
        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", BAD_REQUEST,
          """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.get(nino, taxYear), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }
}
