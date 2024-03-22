/*
 * Copyright 2023 HM Revenue & Customs
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

import models.tailoring.{ExcludeJourneyModel, ExcludedJourneysResponseModel}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import support.mocks.MockAppConfig
import utils.ConnectorIntegrationTest
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class TailoringDataConnectorISpec extends ConnectorIntegrationTest {

  private val mtditid = "some-mtditid"
  private val sessionId = "some-sessionId"
  private val nino = "some-nino"
  private val urlGet = s"/income-tax-submission-service/income-tax/nino/$nino/sources/excluded-journeys/$taxYear"
  private val urlClear = s"/income-tax-submission-service/income-tax/nino/$nino/sources/clear-excluded-journeys/$taxYear"
  private val urlPost = s"/income-tax-submission-service/income-tax/nino/$nino/sources/exclude-journey/$taxYear"

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  private lazy val underTest = new TailoringDataConnector(httpClient, new MockAppConfig().config())

  ".clearExcludedJourney" should {
    "Return a success result" when {
      "returns a 204 (NO CONTENT)" in {
        stubPostWithHeadersCheck(urlClear, NO_CONTENT, Json.obj("journeys" -> Seq("employment")).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.clearExcludedJourney(taxYear, nino), Duration.Inf) shouldBe Right(true)
      }
    }

    "Return an error result" when {

        s"returned a $INTERNAL_SERVER_ERROR" in {
          stubPostWithHeadersCheck(urlClear, INTERNAL_SERVER_ERROR, Json.obj("journeys" -> Seq("employment")).toString(),
            APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

          Await.result(underTest.clearExcludedJourney(taxYear, nino), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
        }

      s"returns an unexpected result" in {
        stubPostWithHeadersCheck(urlClear, TOO_MANY_REQUESTS, Json.obj("journeys" -> Seq("employment")).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.clearExcludedJourney(taxYear, nino), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
    }
  }
  ".getExcludedJourneys" should {
    "Return a success result" when {
      "returns a OK with a valid response" in {
        stubGetWithHeadersCheck(urlGet, OK, Json.toJson(ExcludedJourneysResponseModel(Seq(ExcludeJourneyModel("employment", None)))).toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.getExcludedJourneys(taxYear, nino), Duration.Inf) shouldBe Right(ExcludedJourneysResponseModel(Seq(ExcludeJourneyModel("employment", None))))
      }
    }

    "Return an error result" when {

        s"returned a $INTERNAL_SERVER_ERROR" in {
          stubGetWithHeadersCheck(urlGet, INTERNAL_SERVER_ERROR, Json.toJson(ExcludedJourneysResponseModel(Seq(ExcludeJourneyModel("employment", None)))).toString,
            "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

          Await.result(underTest.getExcludedJourneys(taxYear, nino), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
        }

        s"returned a BadJson error" in {
          stubGetWithHeadersCheck(urlGet, OK, "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

          Await.result(underTest.getExcludedJourneys(taxYear, nino), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
        }

      s"returns an unexpected result" in {
        stubGetWithHeadersCheck(urlGet, TOO_MANY_REQUESTS, Json.toJson(ExcludedJourneysResponseModel(Seq(ExcludeJourneyModel("employment", None)))).toString,
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.getExcludedJourneys(taxYear, nino), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
    }
  }
  ".postExcludedJourneys" should {
    "Return a success result" when {
      "returns a OK with a valid response" in {
        stubPostWithHeadersCheck(urlPost, NO_CONTENT, Json.obj("journey" -> "employment").toString, "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.postExcludedJourney(taxYear, nino), Duration.Inf) shouldBe Right(true)
      }
    }

    "Return an error result" when {

        s"returned a $INTERNAL_SERVER_ERROR" in {
          stubPostWithHeadersCheck(urlPost, INTERNAL_SERVER_ERROR, Json.obj("journey" -> "employment").toString, "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

          Await.result(underTest.postExcludedJourney(taxYear, nino), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
        }

      s"returns an unexpected result" in {
        stubPostWithHeadersCheck(urlPost, TOO_MANY_REQUESTS, Json.obj("journey" -> "employment").toString, "{}",
          "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.postExcludedJourney(taxYear, nino), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
    }
  }
}
