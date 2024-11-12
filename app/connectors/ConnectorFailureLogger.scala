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

import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, JsValidationException, RequestId, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

object ConnectorFailureLogger extends Logging{
  implicit class FromResultToConnectorFailureLogger[T](httpResult: Future[T]) {
    def logFailureReason(connectorName: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[T] = {
      val requestId = hc.requestId.getOrElse(RequestId("Undefined"))
      httpResult.recoverWith {
        case e: UpstreamErrorResponse =>
          logger.warn(s"Received error status ${e.statusCode} from $connectorName with requestId: ${requestId.value}")
          Future.failed(e)
        case e: JsValidationException =>
          logger.warn(s"Unable to parse the content of a response from $connectorName with requestId: ${requestId.value}")
          Future.failed(e)
        case e =>
          logger.warn(s"Received an error from $connectorName with requestId: ${requestId.value}")
          Future.failed(e)
      }
    }
  }
}
