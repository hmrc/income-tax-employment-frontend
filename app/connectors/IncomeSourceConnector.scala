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

import config.AppConfig
import connectors.parsers.RefreshIncomeSourceHttpParser.{RefreshIncomeSourceHttpReads, RefreshIncomeSourceResponse}
import models.requests.RefreshIncomeSourceRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceConnector @Inject()(val http: HttpClient,
                                      val config: AppConfig)(implicit ec: ExecutionContext) {

  def put(taxYear: Int, nino: String)(implicit hc: HeaderCarrier): Future[RefreshIncomeSourceResponse] = {
    val targetUrl = config.incomeTaxSubmissionBEBaseUrl + s"/income-tax/nino/$nino/sources/session?taxYear=$taxYear"
    val employment = "employment"
    http.PUT[RefreshIncomeSourceRequest, RefreshIncomeSourceResponse](targetUrl, RefreshIncomeSourceRequest(employment))
  }
}
