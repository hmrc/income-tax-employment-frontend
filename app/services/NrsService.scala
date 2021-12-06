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
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class NrsService @Inject() (nrsConnector: NrsConnector) {

  def submit[A](nino: String, payload: A, mtditid: String)(implicit hc: HeaderCarrier, writes: Writes[A]): Future[NrsSubmissionResponse] = {

    val extraHeaders = Seq(
      Some("mtditid" -> mtditid),
      hc.trueClientIp.map(ip => "clientIP" -> ip),
      hc.trueClientPort.map(port => "clientPort" -> port)
    ).flatten

    nrsConnector.postNrsConnector(nino, payload)(hc.withExtraHeaders(extraHeaders: _*), writes)
  }

}