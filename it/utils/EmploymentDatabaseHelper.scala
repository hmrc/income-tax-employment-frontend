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

package utils

import models.User
import models.mongo.{EmploymentUserData, ExpensesUserData}
import org.mongodb.scala.bson.collection.immutable.Document
import repositories.{EmploymentUserDataRepositoryImpl, ExpensesUserDataRepositoryImpl}

trait EmploymentDatabaseHelper {
  self: IntegrationTest =>

  lazy val employmentDatabase: EmploymentUserDataRepositoryImpl = app.injector.instanceOf[EmploymentUserDataRepositoryImpl]
  lazy val expensesDatabase: ExpensesUserDataRepositoryImpl = app.injector.instanceOf[ExpensesUserDataRepositoryImpl]

  def dropEmploymentDB(): Unit = {
    await(employmentDatabase.collection.deleteMany(filter = Document()).toFuture())
    await(employmentDatabase.ensureIndexes)
  }

  def dropExpensesDB(): Unit = {
    await(expensesDatabase.collection.deleteMany(filter = Document()).toFuture())
    await(expensesDatabase.ensureIndexes)
  }

  def insertCyaData(cya: EmploymentUserData, user: User[_]): Unit = {
    await(employmentDatabase.createOrUpdate(cya)(user))
  }

  def insertExpensesCyaData(cya: ExpensesUserData, user: User[_]): Unit = {
    await(expensesDatabase.createOrUpdate(cya)(user))
  }

  def findCyaData(taxYear: Int, employmentId: String, user: User[_]): Option[EmploymentUserData] = {
    await(employmentDatabase.find(taxYear, employmentId)(user).map {
      case Left(_) => None
      case Right(value) => value
    })
  }

  def findExpensesCyaData(taxYear: Int, user: User[_]): Option[ExpensesUserData] = {
    await(expensesDatabase.find(taxYear)(user).map {
      case Left(_) => None
      case Right(value) => value
    })
  }
}
