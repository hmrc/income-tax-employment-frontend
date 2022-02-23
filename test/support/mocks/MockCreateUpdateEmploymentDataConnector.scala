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

package support.mocks

import connectors.CreateUpdateEmploymentDataConnector
import connectors.parsers.CreateUpdateEmploymentDataHttpParser.CreateUpdateEmploymentDataResponse
import models.employment.createUpdate.CreateUpdateEmploymentRequest
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockCreateUpdateEmploymentDataConnector extends MockFactory {

  val mockCreateUpdateEmploymentDataConnector: CreateUpdateEmploymentDataConnector = mock[CreateUpdateEmploymentDataConnector]

  def mockCreateUpdateEmploymentData(nino: String, taxYear: Int, data: CreateUpdateEmploymentRequest)
                                    (response: CreateUpdateEmploymentDataResponse = Right(None)): CallHandler4[String, Int,
    CreateUpdateEmploymentRequest, HeaderCarrier, Future[CreateUpdateEmploymentDataResponse]] = {
    (mockCreateUpdateEmploymentDataConnector.createUpdateEmploymentData(_: String, _: Int, _: CreateUpdateEmploymentRequest)(_: HeaderCarrier))
      .expects(nino, taxYear, data, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }
}
