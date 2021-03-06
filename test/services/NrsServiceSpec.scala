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

package services

import connectors.NrsConnector
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import models.AuthorisationRequest
import models.employment.{DecodedCreateNewEmploymentDetailsPayload, DecodedNewEmploymentData, DecodedPriorEmploymentInfo}
import play.api.libs.json.{JsString, Writes}
import play.api.mvc.Request
import play.api.test.FakeRequest
import support.builders.models.UserBuilder.aUser
import uk.gov.hmrc.http.HeaderCarrier
import utils.RequestUtils.getTrueUserAgent
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
      ), DecodedPriorEmploymentInfo(
        "Wow Name 2", Some("222/12345")
      )
    )
  )

  ".postNrsConnector" when {

    "there is user-agent, true client ip and port" should {

      "return the connector response" in {

        val expectedResult: NrsSubmissionResponse = Right()

        val headerCarrierWithTrueClientDetails = headerCarrierWithSession.copy(trueClientIp = Some("127.0.0.1"), trueClientPort = Some("80"))

        (connector.postNrsConnector(_: String, _: DecodedCreateNewEmploymentDetailsPayload)(_: HeaderCarrier, _: Writes[DecodedCreateNewEmploymentDetailsPayload]))
          .expects(nino, payloadModel, headerCarrierWithTrueClientDetails.withExtraHeaders("mtditid" -> mtditid, "User-Agent" -> "income-tax-employment-frontend", "True-User-Agent" -> "Firefox v4.4543 / Android v3.42 / IOS v134.32", "clientIP" -> "127.0.0.1", "clientPort" -> "80"), writesObject)
          .returning(Future.successful(expectedResult))

        val request = AuthorisationRequest(aUser,FakeRequest().withHeaders("User-Agent" -> "Firefox v4.4543 / Android v3.42 / IOS v134.32"))

        val result = await(service.submit(nino, payloadModel, mtditid, getTrueUserAgent(request))(headerCarrierWithTrueClientDetails, writesObject))

        result shouldBe expectedResult
      }
    }

    "there isn't user-agent, true client ip and port" should {

      "return the connector response" in {

        val expectedResult: NrsSubmissionResponse = Right()

        (connector.postNrsConnector(_: String, _: DecodedCreateNewEmploymentDetailsPayload)(_: HeaderCarrier, _: Writes[DecodedCreateNewEmploymentDetailsPayload]))
          .expects(nino, payloadModel, headerCarrierWithSession.withExtraHeaders("mtditid" -> mtditid, "User-Agent" -> "income-tax-employment-frontend", "True-User-Agent" -> "No user agent provided"), writesObject)
          .returning(Future.successful(expectedResult))

        val result = await(service.submit(nino, payloadModel, mtditid, getTrueUserAgent))

        result shouldBe expectedResult
      }
    }

  }

}