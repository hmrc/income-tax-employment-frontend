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
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{Request, Result}
import services.EmploymentSessionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockEmploymentSessionService extends MockFactory {

  val mockIncomeTaxUserDataService: EmploymentSessionService = mock[EmploymentSessionService]

  def mockFind(taxYear: Int, result: Result): CallHandler5[User[_], Int, AllEmploymentData => Result, Request[_], HeaderCarrier, Future[Result]] = {
    (mockIncomeTaxUserDataService.findPreviousEmploymentUserData(_: User[_],_: Int)
    (_: AllEmploymentData => Result)(_: Request[_], _: HeaderCarrier))
      .expects(*, taxYear, *, *, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()
  }
}
