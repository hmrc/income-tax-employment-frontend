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

package config

import connectors.httpParsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.Writes
import services.NrsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockNrsService extends MockFactory {

  val mockNrsService: NrsService = mock[NrsService]

  def verifySubmitEvent[T](event: T): CallHandler[Future[NrsSubmissionResponse]] = {
    (mockNrsService.submit(_: String, _: T, _: String)(_: HeaderCarrier, _: Writes[T]))
      .expects(*, event, *, *, *)
      .returning(Future.successful(Right()))
  }

}
