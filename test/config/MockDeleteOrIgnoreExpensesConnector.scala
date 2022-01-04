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

package config

import connectors.DeleteOrIgnoreExpensesConnector
import connectors.parsers.DeleteOrIgnoreExpensesHttpParser.DeleteOrIgnoreExpensesResponse
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockDeleteOrIgnoreExpensesConnector extends MockFactory {

  val mockDeleteOrIgnoreExpensesConnector: DeleteOrIgnoreExpensesConnector = mock[DeleteOrIgnoreExpensesConnector]

  def mockDeleteOrIgnoreExpensesSuccess(nino: String, taxYear: Int, toRemove: String): CallHandler4[String, Int, String, HeaderCarrier, Future[DeleteOrIgnoreExpensesResponse]] = {
    (mockDeleteOrIgnoreExpensesConnector.deleteOrIgnoreExpenses(_: String, _: Int, _: String)(_: HeaderCarrier))
      .expects(nino, taxYear, toRemove, *)
      .returns(Future.successful(Right(())))
      .anyNumberOfTimes()
  }

  def mockDeleteOrIgnoreExpensesError(nino: String, taxYear: Int, toRemove: String): CallHandler4[String, Int, String, HeaderCarrier, Future[DeleteOrIgnoreExpensesResponse]] = {
    (mockDeleteOrIgnoreExpensesConnector.deleteOrIgnoreExpenses(_: String, _: Int, _: String)(_: HeaderCarrier))
      .expects(nino, taxYear, toRemove, *)
      .returns(Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("CODE", "REASON")))))
      .anyNumberOfTimes()
  }
}
