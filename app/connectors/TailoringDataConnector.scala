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

import config.AppConfig
import connectors.parsers.ClearExcludedJourneysHttpParser.ClearExcludedJourneysResponse
import connectors.parsers.GetExcludedJourneysHttpParser.{ExcludedJourneysResponse, GetExcludedJourneysHttpReads}
import connectors.parsers.PostExcludedJourneyHttpParser.{PostExcludedJourneyHttpReads, PostExcludedJourneyResponse}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TailoringDataConnector @Inject()(val http: HttpClientV2,
                                       val config: AppConfig)(implicit ec: ExecutionContext) {

  def getExcludedJourneys(taxYear: Int, nino: String)
                              (implicit hc: HeaderCarrier): Future[ExcludedJourneysResponse] = {
    val getExcludedJourneysUrl: String = config.incomeTaxSubmissionBEBaseUrl + s"/income-tax/nino/$nino/sources/excluded-journeys/$taxYear"
    http.get(url"$getExcludedJourneysUrl").execute[ExcludedJourneysResponse]
  }

  def clearExcludedJourney(taxYear: Int, nino: String)
                           (implicit hc: HeaderCarrier): Future[ClearExcludedJourneysResponse] = {
    val clearExcludedJourneysUrl: String = config.incomeTaxSubmissionBEBaseUrl + s"/income-tax/nino/$nino/sources/clear-excluded-journeys/$taxYear"
    http
      .post(url"$clearExcludedJourneysUrl")
      .withBody(Json.obj("journeys" -> Seq("employment")))
      .execute[ClearExcludedJourneysResponse]
  }

  def postExcludedJourney(taxYear: Int, nino: String)
                           (implicit hc: HeaderCarrier): Future[PostExcludedJourneyResponse] = {
    val postExcludedJourneyUrl: String = config.incomeTaxSubmissionBEBaseUrl + s"/income-tax/nino/$nino/sources/exclude-journey/$taxYear"
    http.post(url"$postExcludedJourneyUrl").withBody(Json.obj("journey" -> "employment")).execute[PostExcludedJourneyResponse]
  }

}
