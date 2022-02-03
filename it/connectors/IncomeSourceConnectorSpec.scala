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

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.MockAppConfig
import models.requests.RefreshIncomeSourceRequest
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.ConnectorIntegrationTest

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IncomeSourceConnectorSpec extends ConnectorIntegrationTest {

  private val mtditid = "some-mtditid"
  private val sessionId = "some-sessionId"
  private val nino = "some-nino"
  private val taxYear = 2022
  private val headers = Seq(new HttpHeader("X-Session-ID", sessionId), new HttpHeader("mtditid", mtditid))
  private val incomeSource = "some-income-source"
  private val requestBodyJson = Json.toJson(RefreshIncomeSourceRequest(incomeSource)).toString()
  private val url = s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear"

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  private lazy val underTest = new IncomeSourceConnector(httpClient, new MockAppConfig().config())

  "get" should {
    "continue successfully" when {
      "submission returns a 204" in {
        stubPutWithoutResponseBody(url, requestBodyJson, NO_CONTENT, headers)

        Await.result(underTest.put(taxYear, nino, incomeSource), Duration.Inf) shouldBe Right()
      }

      "submission returns a 404" in {
        stubPutWithoutResponseBody(url, requestBodyJson, NOT_FOUND, headers)

        Await.result(underTest.put(taxYear, nino, incomeSource), Duration.Inf) shouldBe Right()
      }
    }

    "Return an error result" when {
      Seq(BAD_REQUEST, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT).foreach { status =>
        s"expenses returns a $status" in {
          stubPutWithResponseBody(url, requestBodyJson, APIErrorBodyModel.parsingError.toString, status, headers)

          val expectedStatus = if (!Seq(BAD_REQUEST, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).contains(status)) INTERNAL_SERVER_ERROR else status
          Await.result(underTest.put(taxYear, nino, incomeSource), Duration.Inf) shouldBe Left(APIErrorModel(expectedStatus, APIErrorBodyModel.parsingError))
        }
      }
    }
  }
}
