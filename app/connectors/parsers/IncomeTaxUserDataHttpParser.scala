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

import models.IncomeTaxUserData.excludePensionIncome
import models.{APIErrorModel, IncomeTaxUserData}
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

object IncomeTaxUserDataHttpParser extends APIParser {
  type IncomeTaxUserDataResponse = Either[APIErrorModel, IncomeTaxUserData]

  override val parserName: String = "IncomeTaxUserDataHttpParser"
  override val service: String = "income-tax-submission"

  implicit object IncomeTaxUserDataHttpReads extends HttpReads[IncomeTaxUserDataResponse] {
    override def read(method: String, url: String, response: HttpResponse): IncomeTaxUserDataResponse = {
      response.status match {
        case OK => response.json.validate[IncomeTaxUserData].fold[IncomeTaxUserDataResponse](
          _ => badSuccessJsonFromAPI,
          parsedModel => Right(excludePensionIncome(parsedModel))
        )
        case NO_CONTENT => Right(IncomeTaxUserData())
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
