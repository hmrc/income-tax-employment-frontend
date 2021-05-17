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

package connectors.httpParsers

import models.{APIErrorBodyModel, APIErrorModel, APIErrorsBodyModel}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HttpResponse
import utils.PagerDutyHelper.PagerDutyKeys.{BAD_SUCCESS_JSON_FROM_API, UNEXPECTED_RESPONSE_FROM_API}
import utils.PagerDutyHelper.pagerDutyLog

trait APIParser {

  val parserName : String
  val service : String

  def logMessage(response:HttpResponse): String ={
    s"[$parserName][read] Received ${response.status} from $service API. Body:${response.body}"
  }

  def badSuccessJsonFromAPI[Response]: Either[APIErrorModel, Response] = {
    pagerDutyLog(BAD_SUCCESS_JSON_FROM_API, s"[$parserName][read] Invalid Json from $service API.")
    Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
  }

  def handleAPIError[Response](response: HttpResponse, statusOverride: Option[Int] = None): Either[APIErrorModel, Response] = {

    val status = statusOverride.getOrElse(response.status)

    try {
      val json = response.json

      lazy val apiError = json.asOpt[APIErrorBodyModel]
      lazy val apiErrors = json.asOpt[APIErrorsBodyModel]

      (apiError, apiErrors) match {
        case (Some(apiError), _) => Left(APIErrorModel(status, apiError))
        case (_, Some(apiErrors)) => Left(APIErrorModel(status, apiErrors))
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, s"[$parserName][read] Unexpected Json from $service API.")
          Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
      }
    } catch {
      case _: Exception => Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
    }
  }
}
