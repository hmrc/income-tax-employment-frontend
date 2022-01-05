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

package connectors.parsers

import models.APIErrorModel
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

object NrsSubmissionHttpParser extends APIParser {
  type NrsSubmissionResponse = Either[APIErrorModel, Unit]
  override val parserName: String = "NrsSubmissionHttpParser"
  override val service: String = "income-tax-nrs-proxy"

  implicit object NrsSubmissionHttpReads extends HttpReads[NrsSubmissionResponse] {
    override def read(method: String, url: String, response: HttpResponse): NrsSubmissionResponse = {
      response.status match {
        case OK => Right()
        case NOT_FOUND | BAD_REQUEST | UNAUTHORIZED =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response)
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response)
      }
    }
  }
}
