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

package services

import connectors.NrsConnector
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import models.AuthorisationRequest
import models.employment.{DecodedCreateNewEmploymentDetailsPayload, DecodedNewEmploymentData, DecodedPriorEmploymentInfo}
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsString, Writes}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.RequestUtils.getTrueUserAgent

import scala.concurrent.Future

class NrsServiceSpec extends UnitTest
  with MockFactory {

  implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")))
  private val connector: NrsConnector = mock[NrsConnector]

  private val underTest: NrsService = new NrsService(connector)

  implicit private val writesObject: Writes[DecodedCreateNewEmploymentDetailsPayload] = (o: DecodedCreateNewEmploymentDetailsPayload) => JsString(o.toString)

  private val payloadModel = DecodedCreateNewEmploymentDetailsPayload(
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
        val expectedResult: NrsSubmissionResponse = Right(())
        val headerCarrierWithTrueClientDetails = headerCarrierWithSession.copy(trueClientIp = Some("127.0.0.1"), trueClientPort = Some("80"))

        (connector.postNrsConnector(_: String, _: DecodedCreateNewEmploymentDetailsPayload)(_: HeaderCarrier, _: Writes[DecodedCreateNewEmploymentDetailsPayload]))
          .expects(aUser.nino, payloadModel, headerCarrierWithTrueClientDetails.withExtraHeaders("mtditid" -> aUser.mtditid, "User-Agent" -> "income-tax-employment-frontend", "True-User-Agent" -> "Firefox v4.4543 / Android v3.42 / IOS v134.32", "clientIP" -> "127.0.0.1", "clientPort" -> "80"), writesObject)
          .returning(Future.successful(expectedResult))

        val request = AuthorisationRequest(aUser, FakeRequest().withHeaders("User-Agent" -> "Firefox v4.4543 / Android v3.42 / IOS v134.32"))

        await(underTest.submit(aUser.nino, payloadModel, aUser.mtditid, getTrueUserAgent(request))(headerCarrierWithTrueClientDetails, writesObject)) shouldBe expectedResult
      }
    }

    "there isn't user-agent, true client ip and port" should {
      "return the connector response" in {
        implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
          new AuthorisationRequest[AnyContent](models.User("1234567890", None, "AA123456A", "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString), FakeRequest())
        val expectedResult: NrsSubmissionResponse = Right(())

        (connector.postNrsConnector(_: String, _: DecodedCreateNewEmploymentDetailsPayload)(_: HeaderCarrier, _: Writes[DecodedCreateNewEmploymentDetailsPayload]))
          .expects(aUser.nino, payloadModel, headerCarrierWithSession.withExtraHeaders("mtditid" -> aUser.mtditid, "User-Agent" -> "income-tax-employment-frontend", "True-User-Agent" -> "No user agent provided"), writesObject)
          .returning(Future.successful(expectedResult))

        await(underTest.submit(aUser.nino, payloadModel, aUser.mtditid, getTrueUserAgent)) shouldBe expectedResult
      }
    }
  }
}
