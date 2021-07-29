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

import connectors.httpParsers.DeleteOrIgnoreEmploymentHttpParser.DeleteOrIgnoreEmploymentResponse
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DeleteOrIgnoreEmploymentConnectorSpec extends IntegrationTest {

  lazy val connector: DeleteOrIgnoreEmploymentConnector = app.injector.instanceOf[DeleteOrIgnoreEmploymentConnector]
  lazy val externalConnector: DeleteOrIgnoreEmploymentConnector = appWithFakeExternalCall.injector.instanceOf[DeleteOrIgnoreEmploymentConnector]

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  val employmentId: String = "001"

  val url: String = s"/income-tax-employment/income-tax/nino/$nino/sources/$employmentId/ALL\\?taxYear=$taxYear"

  "DeleteOrIgnoreEmploymentConnector" should {

    "Return a success result" when {
      "employment returns a 204 (NO CONTENT)" in {

        stubDeleteWithHeadersCheck(url, NO_CONTENT, "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.deleteOrIgnoreEmployment(nino, taxYear, employmentId, "ALL"), Duration.Inf)
        result shouldBe Right(())

      }
    }
    "Return an error result" when {
      s"employment returns a $BAD_REQUEST" in {

        stubDeleteWithHeadersCheck(url, BAD_REQUEST, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.deleteOrIgnoreEmployment(nino, taxYear, employmentId, "ALL"), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $NOT_FOUND" in {

        stubDeleteWithHeadersCheck(url, NOT_FOUND, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.deleteOrIgnoreEmployment(nino, taxYear, employmentId, "ALL"), Duration.Inf)
        result shouldBe Left(APIErrorModel(NOT_FOUND, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $FORBIDDEN" in {

        stubDeleteWithHeadersCheck(url, FORBIDDEN, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.deleteOrIgnoreEmployment(nino, taxYear, employmentId, "ALL"), Duration.Inf)
        result shouldBe Left(APIErrorModel(FORBIDDEN, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $UNPROCESSABLE_ENTITY" in {

        stubDeleteWithHeadersCheck(url, UNPROCESSABLE_ENTITY, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.deleteOrIgnoreEmployment(nino, taxYear, employmentId, "ALL"), Duration.Inf)
        result shouldBe Left(APIErrorModel(UNPROCESSABLE_ENTITY, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $INTERNAL_SERVER_ERROR" in {

        stubDeleteWithHeadersCheck(url, INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.deleteOrIgnoreEmployment(nino, taxYear, employmentId, "ALL"), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $SERVICE_UNAVAILABLE" in {

        stubDeleteWithHeadersCheck(url, SERVICE_UNAVAILABLE, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.deleteOrIgnoreEmployment(nino, taxYear, employmentId, "ALL"), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel.parsingError))
      }

      s"employment returns an unexpected result" in {

        stubDeleteWithHeadersCheck(url, TOO_MANY_REQUESTS, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.deleteOrIgnoreEmployment(nino, taxYear, employmentId, "ALL"), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

    }
  }
}
