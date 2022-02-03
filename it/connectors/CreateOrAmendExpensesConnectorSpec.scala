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

import builders.models.expenses.ExpensesBuilder.anExpenses
import com.github.tomakehurst.wiremock.http.HttpHeader
import config.MockAppConfig
import models.requests.CreateUpdateExpensesRequest
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.ConnectorIntegrationTest

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class CreateOrAmendExpensesConnectorSpec extends ConnectorIntegrationTest {

  private val mtditid = "some-mtditid"
  private val sessionId = "some-sessionId"
  private val nino = "some-nino"
  private val taxYear = 2022
  private val url = s"/income-tax-expenses/income-tax/nino/$nino/sources\\?taxYear=$taxYear"
  private val createExpensesRequestModel = CreateUpdateExpensesRequest(Some(false), anExpenses)

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  private val underTest = new CreateOrAmendExpensesConnector(httpClient, new MockAppConfig().config())

  "CreateOrAmendExpensesConnector - createOrAmendExpenses" should {
    val requestBodyJson = Json.toJson(createExpensesRequestModel).toString()
    val headers = Seq(new HttpHeader("X-Session-ID", sessionId), new HttpHeader("mtditid", mtditid))

    "Return a success result" when {
      "expenses returns a 204 (NO CONTENT)" in {
        stubPutWithResponseBody(url, requestBodyJson, APIErrorBodyModel.parsingError.toString, NO_CONTENT, headers)

        Await.result(underTest.createOrAmendExpenses(nino, taxYear, createExpensesRequestModel), Duration.Inf) shouldBe Right(())
      }
    }

    "Return an error result" when {
      Seq(BAD_REQUEST, NOT_FOUND, FORBIDDEN, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>
        s"expenses returns a $status" in {
          stubPutWithResponseBody(url, requestBodyJson, APIErrorBodyModel.parsingError.toString, status, headers)

          Await.result(underTest.createOrAmendExpenses(nino, taxYear, createExpensesRequestModel), Duration.Inf) shouldBe
            Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
        }
      }

      s"expenses returns an unexpected result" in {
        stubPutWithResponseBody(url, requestBodyJson, APIErrorBodyModel.parsingError.toString, TOO_MANY_REQUESTS, headers)

        Await.result(underTest.createOrAmendExpenses(nino, taxYear, createExpensesRequestModel), Duration.Inf) shouldBe
          Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
    }
  }
}
