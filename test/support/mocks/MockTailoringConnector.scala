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

package support.mocks

import connectors.TailoringDataConnector
import connectors.parsers.ClearExcludedJourneysHttpParser.ClearExcludedJourneysResponse
import connectors.parsers.GetExcludedJourneysHttpParser.ExcludedJourneysResponse
import connectors.parsers.PostExcludedJourneyHttpParser.PostExcludedJourneyResponse
import models.tailoring.ExcludedJourneysResponseModel
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockTailoringConnector extends MockFactory {

  val mockTailoringDataConnector: TailoringDataConnector = mock[TailoringDataConnector]

  def mockGetExcludedJourneys(userData: ExcludedJourneysResponseModel, taxYear: Int, nino: String): CallHandler3[Int, String, HeaderCarrier, Future[ExcludedJourneysResponse]] = {
    (mockTailoringDataConnector.getExcludedJourneys( _: Int, _: String)(_: HeaderCarrier))
        .expects(taxYear, nino, *)
        .returns(Future.successful(Right(userData)))
        .anyNumberOfTimes()
  }

  def mockClearExcludedJourneys(taxYear: Int, nino: String): CallHandler3[Int, String, HeaderCarrier, Future[ClearExcludedJourneysResponse]] = {
    (mockTailoringDataConnector.clearExcludedJourney( _: Int, _: String)(_: HeaderCarrier))
        .expects(taxYear, nino, *)
        .returns(Future.successful(Right(true)))
        .anyNumberOfTimes()
  }

  def mockPostExcludedJourneys(taxYear: Int, nino: String): CallHandler3[Int, String, HeaderCarrier, Future[PostExcludedJourneyResponse]] = {
    (mockTailoringDataConnector.postExcludedJourney( _: Int, _: String)(_: HeaderCarrier))
        .expects(taxYear, nino, *)
        .returns(Future.successful(Right(true)))
        .anyNumberOfTimes()
  }

}
