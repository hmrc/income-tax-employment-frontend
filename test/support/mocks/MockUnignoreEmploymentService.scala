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

import models.employment.EmploymentSource
import models.{APIErrorModel, User}
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import services.UnignoreEmploymentService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockUnignoreEmploymentService extends MockFactory {

  val mockUnignoreEmploymentService: UnignoreEmploymentService = mock[UnignoreEmploymentService]

  def mockUnignore(user: User,
                   taxYear: Int,
                   hmrcEmploymentSource: EmploymentSource,
                   response: Either[APIErrorModel, Unit]): CallHandler4[User, Int, EmploymentSource, HeaderCarrier, Future[Either[APIErrorModel, Unit]]] = {
    (mockUnignoreEmploymentService.unignoreEmployment(_: User, _: Int, _: EmploymentSource)(_: HeaderCarrier))
      .expects(user, taxYear, hmrcEmploymentSource, *)
      .returns(Future.successful(response))
  }
}
