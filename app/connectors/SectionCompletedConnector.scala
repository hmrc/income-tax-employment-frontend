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


import config.AppConfig
import models.mongo.JourneyAnswers
import org.apache.pekko.Done
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import ConnectorFailureLogger._

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SectionCompletedConnector @Inject()(appConfig: AppConfig, httpClient: HttpClientV2)(implicit ec: ExecutionContext) {

  private def keepAliveUrl(journey: String, taxYear:Int) =
    url"${appConfig.incomeTaxEmploymentBEUrl}/income-tax/journey-answers/keep-alive/$journey/$taxYear"

  def completedSectionUrl(journey: String, taxYear: Int): URL =
    url"${appConfig.incomeTaxEmploymentBEUrl}/income-tax/journey-answers/$journey/$taxYear"

  def get(mtdItId: String, taxYear: Int, journey: String)(implicit hc: HeaderCarrier): Future[Option[JourneyAnswers]] = {
    httpClient
      .get(completedSectionUrl(journey, taxYear))
      .setHeader(("MTDITID", mtdItId))
      .execute[Option[JourneyAnswers]]
      .logFailureReason(connectorName = "JourneyAnswersConnector on get")
  }

  def set(answers: JourneyAnswers)(implicit hc: HeaderCarrier): Future[Done] = {
    val url = url"${appConfig.incomeTaxEmploymentBEUrl}/income-tax/journey-answers"

    val result = httpClient
      .post(url)
      .setHeader(("MTDITID", answers.mtdItId))
      .withBody(Json.toJson(answers))
      .execute[HttpResponse]
      .logFailureReason(connectorName = "JourneyAnswersConnector on set")
      .flatMap { response =>
        if (response.status == NO_CONTENT) {
          Future.successful(Done)
        } else {
          Future.failed(UpstreamErrorResponse("", response.status))
        }
      }
    result
  }

  def keepAlive(mtdItId: String, taxYear: Int, journey: String)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .post(keepAliveUrl(journey, taxYear))
      .setHeader(("MTDITID", mtdItId))
      .execute[HttpResponse]
      .logFailureReason(connectorName = "JourneyAnswersConnector on keepAlive")
      .flatMap { response =>
        if (response.status == NO_CONTENT) {
          Future.successful(Done)
        } else {
          Future.failed(UpstreamErrorResponse("", response.status))
        }
      }
}
