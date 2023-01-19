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

import connectors.DeleteOrIgnoreEmploymentConnector
import connectors.parsers.DeleteOrIgnoreEmploymentHttpParser.DeleteOrIgnoreEmploymentResponse
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockDeleteOrIgnoreEmploymentConnector extends MockFactory {

  val mockDeleteOrIgnoreEmploymentConnector: DeleteOrIgnoreEmploymentConnector = mock[DeleteOrIgnoreEmploymentConnector]

  def mockDeleteOrIgnoreEmploymentRight(nino: String,
                                        taxYear: Int,
                                        employmentId: String,
                                        toRemove: String): CallHandler5[String, Int, String, String, HeaderCarrier, Future[DeleteOrIgnoreEmploymentResponse]] = {
    (mockDeleteOrIgnoreEmploymentConnector.deleteOrIgnoreEmployment(_: String, _: Int, _: String, _: String)(_: HeaderCarrier))
      .expects(nino, taxYear, employmentId, toRemove, *)
      .returns(Future.successful(Right(())))
      .anyNumberOfTimes()
  }

  def mockDeleteOrIgnoreEmploymentLeft(nino: String,
                                       taxYear: Int,
                                       employmentId: String,
                                       toRemove: String): CallHandler5[String, Int, String, String, HeaderCarrier, Future[DeleteOrIgnoreEmploymentResponse]] = {
    (mockDeleteOrIgnoreEmploymentConnector.deleteOrIgnoreEmployment(_: String, _: Int, _: String, _: String)(_: HeaderCarrier))
      .expects(nino, taxYear, employmentId, toRemove, *)
      .returns(Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("", "")))))
      .anyNumberOfTimes()
  }
}