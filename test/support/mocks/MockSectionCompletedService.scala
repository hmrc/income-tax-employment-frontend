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

package support.mocks

import models.mongo.JourneyAnswers
import org.apache.pekko.Done
import org.scalamock.handlers.{CallHandler2, CallHandler4}
import org.scalamock.scalatest.MockFactory
import services.SectionCompletedService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSectionCompletedService extends MockFactory {

  protected val mockSectionCompletedService: SectionCompletedService = mock[SectionCompletedService]

  def mockGet(mtdItId: String, taxYear: Int, journey: String, result: Option[JourneyAnswers]): CallHandler4[String, Int, String, HeaderCarrier, Future[Option[JourneyAnswers]]] = {
    (mockSectionCompletedService.get(_: String, _: Int, _: String)(_:HeaderCarrier))
      .expects(mtdItId, taxYear, journey, *)
      .returns(Future.successful(result))
  }

  def mockSet(result: Done): CallHandler2[JourneyAnswers, HeaderCarrier, Future[Done]] = {
    (mockSectionCompletedService.set(_: JourneyAnswers)(_:HeaderCarrier))
      .expects(*, *)
      .returns(Future.successful(result))
  }

}
