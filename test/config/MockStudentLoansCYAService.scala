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

import models.mongo.EmploymentUserData
import models.{AuthorisationRequest, User}
import org.scalamock.handlers.CallHandler6
import org.scalamock.scalatest.MockFactory
import play.api.mvc.Result
import services.studentLoans.StudentLoansCYAService

import scala.concurrent.Future

trait MockStudentLoansCYAService extends MockFactory {

  val mockStudentLoansCYAService: StudentLoansCYAService = mock[StudentLoansCYAService]

  def mockCreateOrUpdateSessionData(result: Result): CallHandler6[String, EmploymentUserData, Int, User, Result, AuthorisationRequest[_], Future[Result]] = {
    (mockStudentLoansCYAService.createOrUpdateSessionData(_: String,
      _: EmploymentUserData,
      _: Int,
      _: User)(_: Result)(_: AuthorisationRequest[_]))
      .expects(*, *, *, *, *, *)
      .returns(Future.successful(result))
      .once()
  }
}



