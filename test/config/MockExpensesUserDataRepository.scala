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

import models.User
import models.mongo.{DatabaseError, ExpensesUserData}
import org.scalamock.handlers.CallHandler2
import org.scalamock.scalatest.MockFactory
import repositories.ExpensesUserDataRepository

import scala.concurrent.Future

trait MockExpensesUserDataRepository extends MockFactory {

  val mockExpensesUserDataRepository: ExpensesUserDataRepository = mock[ExpensesUserDataRepository]

  def mockFind(taxYear: Int,
               response: Either[DatabaseError,Option[ExpensesUserData]]): CallHandler2[Int, User[_], Future[Either[DatabaseError, Option[ExpensesUserData]]]] = {
    (mockExpensesUserDataRepository.find(_: Int)(_: User[_]))
      .expects(taxYear, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }

  def mockCreateOrUpdate(expenses: ExpensesUserData,
                         response: Option[ExpensesUserData]
                        ): CallHandler2[ExpensesUserData, User[_], Future[Either[DatabaseError, Unit]]] = {
    (mockExpensesUserDataRepository.createOrUpdate(_: ExpensesUserData)(_: User[_]))
      .expects(expenses, *)
      .returns(Future.successful(Right(response)))
      .anyNumberOfTimes()
  }

  def mockClear(taxYear: Int, response: Boolean): Unit = {
    (mockExpensesUserDataRepository.clear(_: Int)(_: User[_]))
      .expects(taxYear, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }


}
