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
import models.mongo.EmploymentUserData
import org.scalamock.handlers.{CallHandler1, CallHandler2, CallHandler3}
import org.scalamock.scalatest.MockFactory
import repositories.EmploymentUserDataRepository

import scala.concurrent.Future

trait MockEmploymentUserDataRepository extends MockFactory {

  val mockEmploymentUserDataRepository: EmploymentUserDataRepository = mock[EmploymentUserDataRepository]

  def mockFind(taxYear: Int, id: String,
               employmentUserData: Option[EmploymentUserData]): CallHandler3[Int, String, User[_], Future[Option[EmploymentUserData]]] = {
    (mockEmploymentUserDataRepository.find(_: Int, _:String)(_: User[_]))
      .expects(taxYear, id, *)
      .returns(Future.successful(employmentUserData))
      .anyNumberOfTimes()
  }
  def mockCreate(employmentUserData: EmploymentUserData, response: Boolean): CallHandler2[EmploymentUserData, User[_], Future[Boolean]] = {
    (mockEmploymentUserDataRepository.create(_: EmploymentUserData)(_: User[_]))
      .expects(employmentUserData,*)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }
  def mockUpdate(employmentUserData: EmploymentUserData, response: Boolean): CallHandler1[EmploymentUserData, Future[Boolean]] = {
    (mockEmploymentUserDataRepository.update(_: EmploymentUserData))
      .expects(employmentUserData)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }


}
