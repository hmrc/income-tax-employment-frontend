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

import config.AppConfig
import connectors.httpParsers.CreateUpdateEmploymentDataHttpParser._
import connectors.httpParsers.CreateUpdateEmploymentDataHttpParser.CreateUpdateEmploymentDataHttpReads
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import javax.inject.Inject
import models.employment.createUpdate.CreateUpdateEmploymentRequest

import scala.concurrent.{ExecutionContext, Future}

class CreateUpdateEmploymentDataConnector @Inject()(val http: HttpClient,
                                                    val config: AppConfig)(implicit ec: ExecutionContext) {

  def createUpdateEmploymentData(nino: String, taxYear: Int, data: CreateUpdateEmploymentRequest)
                                (implicit hc: HeaderCarrier): Future[CreateUpdateEmploymentDataResponse] = {

    val url: String = config.incomeTaxEmploymentBEUrl + s"/income-tax/nino/$nino/sources?taxYear=$taxYear"

    http.POST[CreateUpdateEmploymentRequest,CreateUpdateEmploymentDataResponse](url, data)
  }
}
