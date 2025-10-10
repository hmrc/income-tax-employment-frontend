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
import models.mongo.{DatabaseError, EmploymentUserData}
import org.scalamock.handlers.{CallHandler1, CallHandler3}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import repositories.EmploymentUserDataRepository

import scala.concurrent.Future

trait MockEmploymentUserDataRepository extends MockFactory { _: TestSuite =>

  val mockEmploymentUserDataRepository: EmploymentUserDataRepository = mock[EmploymentUserDataRepository]


  def mockFind(taxYear: Int,
               id: String,
               user: User,
               repositoryResponse: Either[DatabaseError, Option[EmploymentUserData]]
              ): CallHandler3[Int, String, User, Future[Either[DatabaseError, Option[EmploymentUserData]]]] = {
    (mockEmploymentUserDataRepository.find(_: Int, _: String, _: User))
      .expects(taxYear, id, user)
      .returns(Future.successful(repositoryResponse))
      .anyNumberOfTimes()
  }

  @deprecated("mockFind with the full parameter list should be used nod wild card")
  def mockFind(taxYear: Int,
               id: String,
               repositoryResponse: Either[DatabaseError, Option[EmploymentUserData]]
              ): CallHandler3[Int, String, User, Future[Either[DatabaseError, Option[EmploymentUserData]]]] = {
    (mockEmploymentUserDataRepository.find(_: Int, _: String, _: User))
      .expects(taxYear, id, *)
      .returns(Future.successful(repositoryResponse))
      .anyNumberOfTimes()
  }

  def mockCreateOrUpdate(employmentUserData: EmploymentUserData,
                         response: Either[DatabaseError, Unit]): CallHandler1[EmploymentUserData, Future[Either[DatabaseError, Unit]]] = {
    (mockEmploymentUserDataRepository.createOrUpdate(_: EmploymentUserData))
      .expects(employmentUserData)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }

  def mockClear(taxYear: Int, employmentId: String, response: Boolean): Unit = {
    (mockEmploymentUserDataRepository.clear(_: Int, _: String, _: User))
      .expects(taxYear, employmentId, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }
}
