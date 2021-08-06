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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import connectors.httpParsers.CreateOrAmendExpensesHttpParser.CreateOrAmendExpensesResponse
import models.expenses.CreateExpensesRequestModel
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DeleteOrIgnoreExpensesConnectorSpec extends IntegrationTest {

  lazy val connector: DeleteOrIgnoreExpensesConnector = app.injector.instanceOf[DeleteOrIgnoreExpensesConnector]
  lazy val externalConnector: DeleteOrIgnoreExpensesConnector = appWithFakeExternalCall.injector.instanceOf[DeleteOrIgnoreExpensesConnector]

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  val createExpensesRequestModel = CreateExpensesRequestModel(Some(false), expenses)

  val validToRemove = "HMRC-HELD"

  val url: String = s"/income-tax-expenses/income-tax/nino/$nino/sources/$validToRemove\\?taxYear=$taxYear"

  "DeleteOrIgnoreExpensesConnector - deleteOrIgnoreExpenses" should {
    val headers = Seq(new HttpHeader("X-Session-ID", sessionId), new HttpHeader("mtditid", mtditid))

    "Return a success result" when {
      "expenses returns a 204 (NO CONTENT)" in {
        stubDeleteWithoutResponseBody(url, NO_CONTENT, headers)

        val result: CreateOrAmendExpensesResponse = Await.result(connector.deleteOrIgnoreExpenses(nino, taxYear, validToRemove), Duration.Inf)
        result shouldBe Right(())
      }
    }

    "Return an error result" when {

      Seq(BAD_REQUEST, NOT_FOUND, FORBIDDEN, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>
        s"expenses returns a $status" in {
          stubDeleteWithResponseBody(url, status, APIErrorBodyModel.parsingError.toString, headers)

          val result: CreateOrAmendExpensesResponse = Await.result(connector.deleteOrIgnoreExpenses(nino, taxYear, validToRemove), Duration.Inf)
          result shouldBe Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
        }
      }

      s"expenses returns an unexpected result" in {
        stubDeleteWithResponseBody(url, TOO_MANY_REQUESTS, APIErrorBodyModel.parsingError.toString, headers)

        val result: CreateOrAmendExpensesResponse = Await.result(connector.deleteOrIgnoreExpenses(nino, taxYear, validToRemove), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
    }
  }
}
