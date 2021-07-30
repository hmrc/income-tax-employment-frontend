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

import models.User
import models.employment.AllEmploymentData
import org.scalamock.handlers.CallHandler7
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{Request, Result}
import services.DeleteOrIgnoreEmploymentService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockDeleteOrIgnoreEmploymentService extends MockFactory {

  val mockDeleteOrIgnoreEmploymentService: DeleteOrIgnoreEmploymentService = mock[DeleteOrIgnoreEmploymentService]

  def mockDeleteOrIgnore(user: User[_], employmentData: AllEmploymentData, taxYear: Int, employmentId: String)(result: Result): CallHandler7[User[_], AllEmploymentData, Int, String, Result, Request[_], HeaderCarrier, Future[Result]] = {
    (mockDeleteOrIgnoreEmploymentService.deleteOrIgnoreEmployment(_: User[_], _: AllEmploymentData, _: Int, _: String)(_: Result)(_: Request[_], _:HeaderCarrier))
      .expects(user, employmentData, taxYear, employmentId, result, *, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()
  }


}
