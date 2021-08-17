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

package repositories

import com.mongodb.MongoTimeoutException
import common.UUID
import models.User
import models.employment.Expenses
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.{MongoException, MongoInternalException, MongoWriteException}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.AnyContent
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.IntegrationTest
import utils.PagerDutyHelper.PagerDutyKeys.FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA

import scala.concurrent.Future

class ExpensesUserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  val repo: ExpensesUserDataRepositoryImpl = app.injector.instanceOf[ExpensesUserDataRepositoryImpl]

  private def count = await(repo.collection.countDocuments().toFuture())

  private def find(expensesUserData: ExpensesUserData)(implicit user: User[_]): Future[Option[ExpensesUserData]] = {
    repo.collection
      .find(filter = Repository.filterExpenses(user.sessionId, user.mtditid, user.nino, expensesUserData.taxYear))
      .toFuture()
      .map(_.headOption)
  }

  class EmptyDatabase {
    await(repo.collection.drop().toFuture())
    await(repo.ensureIndexes)
    count mustBe 0
  }

  private val sessionIdOne = UUID.randomUUID
  private val sessionIdTwo = UUID.randomUUID

  private val now = DateTime.now(DateTimeZone.UTC)

  val expensesUserDataOne: ExpensesUserData = ExpensesUserData(
    sessionIdOne,
    mtditid,
    nino,
    2022,
    isPriorSubmission = true,
    ExpensesCYAModel(
      expenses = Expenses(
        Some(100),
        Some(100),
        Some(100),
        Some(100),
        Some(100),
        Some(100),
        Some(100),
        Some(100)
      ),
      currentDataIsHmrcHeld = true
    ),
    lastUpdated = now
  )

  val expensesUserDataTwo: ExpensesUserData = ExpensesUserData(
    sessionIdTwo,
    mtditid,
    nino,
    2022,
    isPriorSubmission = true,
    ExpensesCYAModel(
      expenses = Expenses(),
      currentDataIsHmrcHeld = true
    ),
    lastUpdated = now
  )

  implicit val request: FakeRequest[AnyContent] = fakeRequest

  val userOne = User(expensesUserDataOne.mtdItId, None, expensesUserDataOne.nino, expensesUserDataOne.sessionId, AffinityGroup.Individual.toString)
  val userTwo = User(expensesUserDataTwo.mtdItId, None, expensesUserDataTwo.nino, expensesUserDataTwo.sessionId, AffinityGroup.Individual.toString)

  "clear" should {
    "remove a record" in new EmptyDatabase {
      count mustBe 0
      val createAttempt: Option[ExpensesUserData] = await(repo.createOrUpdate(expensesUserDataOne)(userOne))
      createAttempt mustBe Some(expensesUserDataOne)
      count mustBe 1

      val clearAttempt: Boolean = await(repo.clear(taxYear)(userOne))
      clearAttempt mustBe true
      count mustBe 0
    }
  }

  "createOrUpdate" should {

    "create a document in collection when one does not exist" in new EmptyDatabase {
      val createAttempt: Option[ExpensesUserData] = await(repo.createOrUpdate(expensesUserDataOne)(userOne))
      createAttempt mustBe Some(expensesUserDataOne)
      count mustBe 1
    }

    "update a document in collection when one already exists" in new EmptyDatabase {
      val createAttempt: Option[ExpensesUserData] = await(repo.createOrUpdate(expensesUserDataOne)(userOne))
      createAttempt.get mustBe expensesUserDataOne
      count mustBe 1

      val updatedEmploymentDetails = expensesUserDataOne.expensesCya.expenses.copy(jobExpenses = Some(34234))
      val updatedEmploymentCyaModel = expensesUserDataOne.expensesCya.copy(expenses = updatedEmploymentDetails)
      val updatedEmploymentUserData = expensesUserDataOne.copy(expensesCya = updatedEmploymentCyaModel)

      val updateAttempt: Option[ExpensesUserData] = await(repo.createOrUpdate(updatedEmploymentUserData)(userOne))
      updateAttempt mustBe Some(updatedEmploymentUserData)
      count mustBe 1
    }
  }

  "find" should {
    "get a document and update the TTL" in new EmptyDatabase {
      val now = DateTime.now(DateTimeZone.UTC)
      val data = expensesUserDataOne.copy(lastUpdated = now)

      val createResult: Option[ExpensesUserData] = await(repo.createOrUpdate(data)(userOne))
      createResult mustBe Some(data)
      count mustBe 1

      val findResult = await(repo.find(data.taxYear)(userOne))

      findResult.map(_.copy(lastUpdated = data.lastUpdated)) mustBe Some(data)
      findResult.map(_.lastUpdated.isAfter(data.lastUpdated)) mustBe Some(true)
    }

    "return None when find operation succeeds but no data is found for the given inputs" in new EmptyDatabase {
      val taxYear = 2021
      val findResult = await(repo.find(taxYear)(userOne))

      findResult mustBe None
    }
  }

  "the set indexes" should {
    "enforce uniqueness" in new EmptyDatabase {
      val createResult: Option[ExpensesUserData] = await(repo.createOrUpdate(expensesUserDataOne)(userOne))
      createResult mustBe Some(expensesUserDataOne)
      count mustBe 1

      val caught = intercept[MongoWriteException](await(repo.collection.insertOne(expensesUserDataOne).toFuture()))

      caught.getMessage must include("E11000 duplicate key error collection: income-tax-employment-frontend.expensesUserData index: UserDataLookupIndex dup key:")
    }
  }

  "mongoRecover" should {
    Seq(new MongoTimeoutException(""), new MongoInternalException(""), new MongoException("")).foreach { exception =>
      s"recover when the exception is a MongoException or a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result =
          Future.failed(exception)
            .recover(repo.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA)(userOne))

        await(result) mustBe None
      }
    }

    Seq(new NullPointerException(""), new RuntimeException("")).foreach { exception =>
      s"not recover when the exception is not a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result =
          Future.failed(exception)
            .recover(repo.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA)(userOne))

        assertThrows[RuntimeException] {
          await(result)
        }
      }
    }
  }
}
