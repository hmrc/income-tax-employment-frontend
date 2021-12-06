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
import models.employment.{DecodedCreateNewEmploymentDetailsPayload, DecodedNewEmploymentData, DecodedPriorEmploymentInfo}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.libs.json.{JsString, Writes}
import play.mvc.Http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.IntegrationTest

class NrsConnectorSpec extends IntegrationTest {

  lazy val connector: NrsConnector = app.injector.instanceOf[NrsConnector]

  implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  implicit val writesObject: Writes[DecodedCreateNewEmploymentDetailsPayload] = (o: DecodedCreateNewEmploymentDetailsPayload) => JsString(o.toString)

  val payloadModel = DecodedCreateNewEmploymentDetailsPayload(
    employmentData = DecodedNewEmploymentData(
      employerName = Some("Name"),
      employerRef = Some("123/12345"),
      startDate = Some("10-10-2000"),
      cessationDate = Some("10-10-2000"),
      taxablePayToDate = Some(55),
      totalTaxToDate = Some(55),
      payrollId = Some("1235")
    ),
    existingEmployments = Seq(
      DecodedPriorEmploymentInfo(
        "Wow Name", Some("123/12345")
      ),DecodedPriorEmploymentInfo(
        "Wow Name 2", Some("222/12345")
      )
    )
  )

  val expectedHeaders = Seq(new HttpHeader("mtditid", mtditid))

  val url: String = s"/income-tax-nrs-proxy/$nino/itsa-personal-income-submission"

  ".NrsConnector" should {

    "return an Accepted response when successful" in {

      stubPost(url, ACCEPTED, "{}")
      val result = await(connector.postNrsConnector(nino, payloadModel))

      result shouldBe Right()
    }

    "return an InternalServerError" in {

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Internal Server Error"))

      stubPost(url, INTERNAL_SERVER_ERROR, expectedResult.toJson.toString())
      val result = await(connector.postNrsConnector(nino, payloadModel))

      result shouldBe Left(expectedResult)
    }

    "return a NotFound error" in {

      val expectedResult = APIErrorModel(NOT_FOUND, APIErrorBodyModel("NOT_FOUND", "NRS returning not found error"))

      stubPost(url, NOT_FOUND, expectedResult.toJson.toString())
      val result = await(connector.postNrsConnector(nino, payloadModel))

      result shouldBe Left(expectedResult)
    }

    "return a ParsingError when an unexpected error has occurred" in {

      val expectedResult = APIErrorModel(CONFLICT, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API"))

      stubPost(url, CONFLICT, expectedResult.toJson.toString())
      val result = await(connector.postNrsConnector(nino, payloadModel))

      result shouldBe Left(expectedResult)
    }

  }
}