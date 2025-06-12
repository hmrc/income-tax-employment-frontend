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

import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import support.TaxYearUtils.taxYear
import support.mocks.MockAppConfig
import utils.ConnectorIntegrationTest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class UnignoreEmploymentConnectorSpec extends ConnectorIntegrationTest {

  private val mtditid = "some-mtditid"
  private val sessionId = "some-sessionId"
  private val nino = "some-nino"
  private val employmentId = "001"
  private val url = s"/income-tax-employment/income-tax/nino/$nino/sources/$employmentId/unignore\\?taxYear=$taxYear"

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  private lazy val underTest = new UnignoreEmploymentConnector(httpClient, new MockAppConfig().config())

  "UnignoreEmploymentConnector" should {
    "Return a success result" when {
      "employment returns a 204 (NO CONTENT)" in {
        stubDeleteWithHeadersCheck(url, NO_CONTENT, "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.unignoreEmployment(nino, taxYear, employmentId), Duration.Inf) shouldBe Right(())
      }
    }

    "Return an error result" when {
      Seq(BAD_REQUEST, NOT_FOUND, FORBIDDEN, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>

        s"expenses returns a $status" in {
          stubDeleteWithHeadersCheck(url, status, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

          Await.result(underTest.unignoreEmployment(nino, taxYear, employmentId), Duration.Inf) shouldBe Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
        }
      }

      s"employment returns an unexpected result" in {
        stubDeleteWithHeadersCheck(url, TOO_MANY_REQUESTS, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.unignoreEmployment(nino, taxYear, employmentId), Duration.Inf) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
    }
  }
}
