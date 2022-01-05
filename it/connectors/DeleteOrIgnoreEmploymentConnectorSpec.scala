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

import connectors.parsers.DeleteOrIgnoreEmploymentHttpParser.DeleteOrIgnoreEmploymentResponse
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
      Seq(BAD_REQUEST, NOT_FOUND, FORBIDDEN, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>

        s"expenses returns a $status" in {
          stubDeleteWithHeadersCheck(url, status, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

          val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.deleteOrIgnoreEmployment(nino, taxYear, employmentId, "ALL"), Duration.Inf)
          result shouldBe Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
        }
      }

      s"employment returns an unexpected result" in {

        stubDeleteWithHeadersCheck(url, TOO_MANY_REQUESTS, APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.deleteOrIgnoreEmployment(nino, taxYear, employmentId, "ALL"), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

    }
  }
}
