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

import connectors.IncomeSourceConnector
import connectors.parsers.RefreshIncomeSourceHttpParser.RefreshIncomeSourceResponse
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockIncomeSourceConnector extends MockFactory {

  val mockIncomeSourceConnector: IncomeSourceConnector = mock[IncomeSourceConnector]

  def mockRefreshIncomeSourceResponseSuccess(taxYear: Int, nino: String): CallHandler3[Int, String, HeaderCarrier, Future[RefreshIncomeSourceResponse]] = {
    (mockIncomeSourceConnector.put(_: Int, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, *)
      .returns(Future.successful(Right(())))
      .anyNumberOfTimes()
  }

  def mockRefreshIncomeSourceResponseError(taxYear: Int, nino: String): CallHandler3[Int, String, HeaderCarrier, Future[RefreshIncomeSourceResponse]] = {
    (mockIncomeSourceConnector.put(_: Int, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, *)
      .returns(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("CODE", "REASON")))))
      .anyNumberOfTimes()
  }
}
