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

import builders.models.employment.EmploymentBenefitsBuilder.anEmploymentBenefits
import connectors.parsers.DeleteOrIgnoreEmploymentHttpParser.DeleteOrIgnoreEmploymentResponse
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class CreateUpdateEmploymentDataConnectorSpec extends IntegrationTest {

  lazy val connector: CreateUpdateEmploymentDataConnector = app.injector.instanceOf[CreateUpdateEmploymentDataConnector]
  lazy val externalConnector: CreateUpdateEmploymentDataConnector = appWithFakeExternalCall.injector.instanceOf[CreateUpdateEmploymentDataConnector]

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  val employmentId: String = "001"
  val model = CreateUpdateEmploymentRequest(
    Some(employmentId),
    employment = Some(
      CreateUpdateEmployment(
        Some("123/12345"),
        "Misery Loves Company",
        "2020-11-11",
        None,
        None
      )
    ),
    employmentData = Some(
      CreateUpdateEmploymentData(
        CreateUpdatePay(
          564563456345.55,
          34523523454.44
        ),
        None,
        benefitsInKind = anEmploymentBenefits.benefits
      )
    ),
    hmrcEmploymentIdToIgnore = None
  )
  val url: String = s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYear"

  "CreateUpdateEmploymentDataConnector" should {

    "Return a success result" when {
      "employment returns a 204 (NO CONTENT)" in {

        stubPostWithHeadersCheck(url, NO_CONTENT, Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.createUpdateEmploymentData(nino, taxYear, model), Duration.Inf)
        result shouldBe Right(())

      }
    }
    "Return an error result" when {
      s"employment returns a $BAD_REQUEST" in {
        stubPostWithHeadersCheck(url, BAD_REQUEST, Json.toJson(model).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.createUpdateEmploymentData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $NOT_FOUND" in {
        stubPostWithHeadersCheck(url, NOT_FOUND, Json.toJson(model).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.createUpdateEmploymentData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(NOT_FOUND, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $FORBIDDEN" in {
        stubPostWithHeadersCheck(url, FORBIDDEN, Json.toJson(model).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.createUpdateEmploymentData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(FORBIDDEN, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $UNPROCESSABLE_ENTITY" in {
        stubPostWithHeadersCheck(url, UNPROCESSABLE_ENTITY, Json.toJson(model).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.createUpdateEmploymentData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(UNPROCESSABLE_ENTITY, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $INTERNAL_SERVER_ERROR" in {
        stubPostWithHeadersCheck(url, INTERNAL_SERVER_ERROR, Json.toJson(model).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.createUpdateEmploymentData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $SERVICE_UNAVAILABLE" in {
        stubPostWithHeadersCheck(url, SERVICE_UNAVAILABLE, Json.toJson(model).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.createUpdateEmploymentData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel.parsingError))
      }

      s"employment returns an unexpected result" in {
        stubPostWithHeadersCheck(url, TOO_MANY_REQUESTS, Json.toJson(model).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: DeleteOrIgnoreEmploymentResponse = Await.result(connector.createUpdateEmploymentData(nino, taxYear, model), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

    }
  }
}
