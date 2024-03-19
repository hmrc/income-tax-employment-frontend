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

import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import support.builders.models.employment.EmploymentBenefitsBuilder.anEmploymentBenefits
import support.mocks.MockAppConfig
import uk.gov.hmrc.http.HeaderCarrier
import utils.ConnectorIntegrationTest

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class CreateUpdateEmploymentDataConnectorSpec extends ConnectorIntegrationTest {

  private val mtditid = "some-mtditid"
  private val sessionId = "some-sessionId"
  private val nino = "some-nino"
  private val employmentId = "001"
  private val url = s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYear"

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  private val createUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
    Some(employmentId),
    employment = Some(CreateUpdateEmployment(Some("123/12345"), "Misery Loves Company", s"${taxYearEOY-1}-11-11")),
    employmentData = Some(CreateUpdateEmploymentData(CreateUpdatePay(564563456345.55, 34523523454.44), benefitsInKind = anEmploymentBenefits.benefits, offPayrollWorker = Some(true))),
    hmrcEmploymentIdToIgnore = None
  )

  private val underTest = new CreateUpdateEmploymentDataConnector(httpClient, new MockAppConfig().config())

  "CreateUpdateEmploymentDataConnector" should {
    "Return a success result" when {
      "employment returns a 204 (NO CONTENT)" in {
        stubPostWithHeadersCheck(url, NO_CONTENT, Json.toJson(createUpdateEmploymentRequest).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.createUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest), Duration.Inf) shouldBe Right(None)
      }

      "employment returns a 201 (CREATED)" in {
        stubPostWithHeadersCheck(url, CREATED, Json.toJson(createUpdateEmploymentRequest).toString(), """{"employmentId":"1234567890"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.createUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest), Duration.Inf) shouldBe Right(Some("1234567890"))
      }
    }

    "Return an error result" when {
      s"employment returns a $BAD_REQUEST" in {
        stubPostWithHeadersCheck(url, BAD_REQUEST, Json.toJson(createUpdateEmploymentRequest).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.createUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest), Duration.Inf) shouldBe
          Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $NOT_FOUND" in {
        stubPostWithHeadersCheck(url, NOT_FOUND, Json.toJson(createUpdateEmploymentRequest).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.createUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest), Duration.Inf) shouldBe
          Left(APIErrorModel(NOT_FOUND, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $FORBIDDEN" in {
        stubPostWithHeadersCheck(url, FORBIDDEN, Json.toJson(createUpdateEmploymentRequest).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.createUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest), Duration.Inf) shouldBe
          Left(APIErrorModel(FORBIDDEN, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $UNPROCESSABLE_ENTITY" in {
        stubPostWithHeadersCheck(url, UNPROCESSABLE_ENTITY, Json.toJson(createUpdateEmploymentRequest).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.createUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest), Duration.Inf) shouldBe
          Left(APIErrorModel(UNPROCESSABLE_ENTITY, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $INTERNAL_SERVER_ERROR" in {
        stubPostWithHeadersCheck(url, INTERNAL_SERVER_ERROR, Json.toJson(createUpdateEmploymentRequest).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.createUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest), Duration.Inf) shouldBe
          Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      s"employment returns a $SERVICE_UNAVAILABLE" in {
        stubPostWithHeadersCheck(url, SERVICE_UNAVAILABLE, Json.toJson(createUpdateEmploymentRequest).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.createUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest), Duration.Inf) shouldBe
          Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel.parsingError))
      }

      s"employment returns an unexpected result" in {
        stubPostWithHeadersCheck(url, TOO_MANY_REQUESTS, Json.toJson(createUpdateEmploymentRequest).toString(),
          APIErrorBodyModel.parsingError.toString, "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        Await.result(underTest.createUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest), Duration.Inf) shouldBe
          Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
    }
  }
}
