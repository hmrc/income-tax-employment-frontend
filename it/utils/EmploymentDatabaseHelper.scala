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

package utils

import models.User
import models.mongo.{EmploymentUserData, ExpensesUserData}
import repositories.{EmploymentUserDataRepositoryImpl, ExpensesUserDataRepositoryImpl}

trait EmploymentDatabaseHelper { self: IntegrationTest =>

  lazy val employmentDatabase: EmploymentUserDataRepositoryImpl = app.injector.instanceOf[EmploymentUserDataRepositoryImpl]
  lazy val expensesDatabase: ExpensesUserDataRepositoryImpl = app.injector.instanceOf[ExpensesUserDataRepositoryImpl]

  //noinspection ScalaStyle
  def dropEmploymentDB(): Unit = {
    await(employmentDatabase.collection.drop().toFutureOption())
    await(employmentDatabase.ensureIndexes)
  }

  //noinspection ScalaStyle
  def dropExpensesDB(): Unit = {
    await(expensesDatabase.collection.drop().toFutureOption())
    await(expensesDatabase.ensureIndexes)
  }

  //noinspection ScalaStyle
  def insertCyaData(cya: EmploymentUserData, user: User[_]): Option[EmploymentUserData] = {
    await(employmentDatabase.createOrUpdate(cya)(user))
  }

  //noinspection ScalaStyle
  def insertExpensesCyaData(cya: ExpensesUserData, user: User[_]): Option[ExpensesUserData] = {
    await(expensesDatabase.createOrUpdate(cya)(user))
  }

  //noinspection ScalaStyle
  def findCyaData(taxYear: Int, employmentId: String, user: User[_]): Option[EmploymentUserData] = {
    await(employmentDatabase.find(taxYear, employmentId)(user))
  }

  //noinspection ScalaStyle
  def findExpensesCyaData(taxYear: Int, user: User[_]): Option[ExpensesUserData] = {
    await(expensesDatabase.find(taxYear)(user))
  }
}
