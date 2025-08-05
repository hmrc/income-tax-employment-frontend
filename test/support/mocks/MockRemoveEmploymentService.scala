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

import models.employment.AllEmploymentData
import models.{APIErrorModel, User}
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import services.employment.RemoveEmploymentService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockRemoveEmploymentService extends MockFactory { _: TestSuite =>

  val mockRemoveEmploymentService: RemoveEmploymentService = mock[RemoveEmploymentService]

  def mockDeleteOrIgnore(employmentData: AllEmploymentData, taxYear: Int, employmentId: String):
  CallHandler5[AllEmploymentData, Int, String, User, HeaderCarrier, Future[Either[APIErrorModel, Unit]]] = {
      (mockRemoveEmploymentService.deleteOrIgnoreEmployment(_: AllEmploymentData, _: Int, _: String, _: User)(_: HeaderCarrier))
        .expects(employmentData, taxYear, employmentId, *, *)
        .returns(Future.successful(Right(())))
        .anyNumberOfTimes()
  }
}
