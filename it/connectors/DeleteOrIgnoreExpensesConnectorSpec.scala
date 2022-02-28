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
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import support.mocks.MockAppConfig
import uk.gov.hmrc.http.HeaderCarrier
import utils.ConnectorIntegrationTest

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class DeleteOrIgnoreExpensesConnectorSpec extends ConnectorIntegrationTest {

  private val mtditid = "some-mtditid"
  private val sessionId = "some-sessionId"
  private val nino = "some-nino"
  private val taxYear = 2022
  private val validToRemove = "HMRC-HELD"
  private val url: String = s"/income-tax-expenses/income-tax/nino/$nino/sources/$validToRemove\\?taxYear=$taxYear"

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  private lazy val underTest = new DeleteOrIgnoreExpensesConnector(httpClient, new MockAppConfig().config())

  "DeleteOrIgnoreExpensesConnector - deleteOrIgnoreExpenses" should {
    val headers = Seq(new HttpHeader("X-Session-ID", sessionId), new HttpHeader("mtditid", mtditid))

    "Return a success result" when {
      "expenses returns a 204 (NO CONTENT)" in {
        stubDeleteWithoutResponseBody(url, NO_CONTENT, headers)

        Await.result(underTest.deleteOrIgnoreExpenses(nino, taxYear, validToRemove), Duration.Inf) shouldBe Right(())
      }
    }

    "Return an error result" when {
      Seq(BAD_REQUEST, NOT_FOUND, FORBIDDEN, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>

        s"expenses returns a $status" in {
          stubDeleteWithResponseBody(url, status, APIErrorBodyModel.parsingError.toString, headers)

          Await.result(underTest.deleteOrIgnoreExpenses(nino, taxYear, validToRemove), Duration.Inf) shouldBe
            Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
        }
      }

      s"expenses returns an unexpected result" in {
        stubDeleteWithResponseBody(url, TOO_MANY_REQUESTS, APIErrorBodyModel.parsingError.toString, headers)

        Await.result(underTest.deleteOrIgnoreExpenses(nino, taxYear, validToRemove), Duration.Inf) shouldBe
          Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
    }
  }
}
