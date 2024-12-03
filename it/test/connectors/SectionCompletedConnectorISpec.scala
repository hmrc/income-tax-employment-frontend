/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.mongo.JourneyAnswers
import models.mongo.JourneyStatus.Completed
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import utils.IntegrationTest

import java.time.Instant

class SectionCompletedConnectorISpec extends IntegrationTest {

  def stubGet(url: String, status: Integer, body: String): StubMapping =
    stubFor(get(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(body)
      )
    )

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  private lazy val connector: connectors.SectionCompletedConnector = app.injector.instanceOf[SectionCompletedConnector]
  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  private def keepAliveUrl(journey: String, taxYear: Int) =
    s"/income-tax-employment/income-tax/journey-answers/keep-alive/$journey/$taxYear"

  private def completedSectionUrl(journey: String, taxYear: Int) =
    s"/income-tax-employment/income-tax/journey-answers/$journey/$taxYear"


  private val mtditId: String = "1234567890"
  val journeyName = "employment-summary"
  val data: JsObject = Json.obj(
    "status" -> Completed.toString
  )
  private val answers = JourneyAnswers(mtditId, taxYear, journeyName, data, lastUpdated = Instant.ofEpochSecond(1))

  s".get" when {

    "request is made, return user answers when the server returns them" in {

      stubGet(s"${completedSectionUrl(journeyName, taxYear)}", OK, Json.toJson(answers).toString)

      val result = connector.get(mtditId, taxYear, journeyName).futureValue

      result.value mustEqual answers
    }

    "must return None when the server returns NOT_FOUND" in {

      stubGet(s"${completedSectionUrl(journeyName, taxYear)}", NOT_FOUND, "{}")

      val result = connector.get(mtditId, taxYear, journeyName).futureValue

      result must not be defined
    }

    "must return a failed future when the server returns an error" in {

      stubGet(s"${completedSectionUrl(journeyName, taxYear)}", INTERNAL_SERVER_ERROR, "{}")

      connector.get(mtditId, taxYear, journeyName).failed.futureValue
    }

    "must return a failed future when the server returns an unexpected response" in {

      stubGet(s"${completedSectionUrl(journeyName, taxYear)}", OK, "{}")

      connector.get(mtditId, taxYear, journeyName).failed.futureValue
    }
  }

  ".set" when {
    "must post user answers to the server" in {

      stubPost(s"/income-tax-employment/income-tax/journey-answers", NO_CONTENT, "{}")


      connector.set(answers).futureValue
    }

    "must return a failed future when the server returns error" in {
      stubPost(s"/income-tax-employment/income-tax/journey-answers", INTERNAL_SERVER_ERROR, "{}")


      connector.set(answers).failed.futureValue
    }

    "must return a failed future when the server returns an unexpected response code" in {
      stubPost(s"/income-tax-employment/income-tax/journey-answers", OK, "{}")


      connector.set(answers).failed.futureValue
    }
  }

  ".keepAlive" when {
    val keepAliveUrlUrl = keepAliveUrl(journeyName,taxYear)
    "must post to the server" in {

      stubPost(s"$keepAliveUrlUrl", NO_CONTENT, "{}")

      connector.keepAlive(mtditId, taxYear, journeyName).futureValue
    }

    "must return a failed future when the server returns error" in {

      stubPost(s"$keepAliveUrlUrl", INTERNAL_SERVER_ERROR, "{}")


      connector.keepAlive(mtditId, taxYear, journeyName).failed.futureValue
    }

    "must return a failed future when the server returns an unexpected response code" in {

      stubPost(s"$keepAliveUrlUrl", OK, "{}")


      connector.keepAlive(mtditId, taxYear, journeyName).failed.futureValue
    }
  }

}
