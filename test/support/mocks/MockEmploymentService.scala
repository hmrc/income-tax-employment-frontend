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

import models.User
import models.mongo.EmploymentUserData
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import services.employment.EmploymentService

import java.time.LocalDate
import scala.concurrent.Future

trait MockEmploymentService extends MockFactory {

  protected val mockEmploymentService: EmploymentService = mock[EmploymentService]

  def mockUpdateEndDate(user: User,
                        taxYear: Int,
                        employmentId: String,
                        originalEmploymentUserData: EmploymentUserData,
                        endDate: LocalDate,
                        result: Either[Unit, EmploymentUserData]
                       ): CallHandler5[User, Int, String, EmploymentUserData, LocalDate, Future[Either[Unit, EmploymentUserData]]] = {
    (mockEmploymentService.updateEndDate(_: User, _: Int, _: String, _: EmploymentUserData, _: LocalDate))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, endDate)
      .returns(Future.successful(result))
  }

  def mockUpdatePayrollId(user: User,
                          taxYear: Int,
                          employmentId: String,
                          originalEmploymentUserData: EmploymentUserData,
                          payrollId: Option[String],
                          result: Either[Unit, EmploymentUserData]
                         ): CallHandler5[User, Int, String, EmploymentUserData, Option[String], Future[Either[Unit, EmploymentUserData]]] = {
    (mockEmploymentService.updatePayrollId(_: User, _: Int, _: String, _: EmploymentUserData, _: Option[String]))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, payrollId)
      .returns(Future.successful(result))
  }
}
