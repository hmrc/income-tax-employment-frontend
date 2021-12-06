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

package services

import connectors.NrsConnector
import connectors.httpParsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import models.employment.{DecodedCreateNewEmploymentDetailsPayload, DecodedNewEmploymentData, DecodedPriorEmploymentInfo}
import play.api.libs.json.{JsString, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.Future

class NrsServiceSpec extends UnitTest {

  val connector: NrsConnector = mock[NrsConnector]
  val service: NrsService = new NrsService(connector)

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

  ".postNrsConnector" when {

    "there is a true client ip and port" should {

      "return the connector response" in {

        val expectedResult: NrsSubmissionResponse = Right()

        val headerCarrierWithTrueClientDetails = headerCarrierWithSession.copy(trueClientIp = Some("127.0.0.1"), trueClientPort = Some("80"))

        (connector.postNrsConnector(_: String, _: DecodedCreateNewEmploymentDetailsPayload)(_: HeaderCarrier, _: Writes[DecodedCreateNewEmploymentDetailsPayload]))
          .expects(nino, payloadModel, headerCarrierWithTrueClientDetails.withExtraHeaders("mtditid" -> mtditid, "clientIP" -> "127.0.0.1", "clientPort" -> "80"), writesObject)
          .returning(Future.successful(expectedResult))

        val result = await(service.submit(nino, payloadModel, mtditid)(headerCarrierWithTrueClientDetails, writesObject))

        result shouldBe expectedResult
      }
    }

    "there isn't a true client ip and port" should {

      "return the connector response" in {

        val expectedResult: NrsSubmissionResponse = Right()

        (connector.postNrsConnector(_: String, _: DecodedCreateNewEmploymentDetailsPayload)(_: HeaderCarrier, _: Writes[DecodedCreateNewEmploymentDetailsPayload]))
          .expects(nino, payloadModel, headerCarrierWithSession.withExtraHeaders("mtditid" -> mtditid), writesObject)
          .returning(Future.successful(expectedResult))

        val result = await(service.submit(nino, payloadModel, mtditid))

        result shouldBe expectedResult
      }
    }

  }

}