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
import models.mongo.{DatabaseError, EncryptedExpensesUserData, EncryptionDecryptionError, ExpensesCYAModel, ExpensesUserData, MongoError}
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.{MongoException, MongoInternalException, MongoWriteException}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.AnyContent
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import services.EncryptionService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.mongo.MongoUtils
import utils.IntegrationTest
import utils.PagerDutyHelper.PagerDutyKeys.FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA

import scala.concurrent.Future

class ExpensesUserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  private val repo: ExpensesUserDataRepositoryImpl = app.injector.instanceOf[ExpensesUserDataRepositoryImpl]
  private val encryptionService = app.injector.instanceOf[EncryptionService]

  private def count = await(repo.collection.countDocuments().toFuture())

  private def find(expensesUserData: ExpensesUserData)(implicit user: User[_]): Future[Option[EncryptedExpensesUserData]] = {
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

  private def countFromOtherDatabase = await(repo.collection.countDocuments().toFuture())

  implicit val request: FakeRequest[AnyContent] = fakeRequest

  val userOne = User(expensesUserDataOne.mtdItId, None, expensesUserDataOne.nino, expensesUserDataOne.sessionId, AffinityGroup.Individual.toString)
  val userTwo = User(expensesUserDataTwo.mtdItId, None, expensesUserDataTwo.nino, expensesUserDataTwo.sessionId, AffinityGroup.Individual.toString)

  val repoWithInvalidEncryption = appWithInvalidEncryptionKey.injector.instanceOf[ExpensesUserDataRepositoryImpl]
  val serviceWithInvalidEncryption: EncryptionService = appWithInvalidEncryptionKey.injector.instanceOf[EncryptionService]

  "update with invalid encryption" should {
    "fail to add data" in new EmptyDatabase {
      countFromOtherDatabase mustBe 0
      val res: Either[DatabaseError, Unit] = await(repoWithInvalidEncryption.createOrUpdate(expensesUserDataOne)(userOne))
      res mustBe Left(EncryptionDecryptionError(
        "Key being used is not valid. It could be due to invalid encoding, wrong length or uninitialized for encrypt Invalid AES key length: 2 bytes"))
    }
  }

  "find with invalid encryption" should {
    "fail to find data" in new EmptyDatabase {
      countFromOtherDatabase mustBe 0
      await(repoWithInvalidEncryption.collection.insertOne(encryptionService.encryptExpenses(expensesUserDataOne)).toFuture())
      countFromOtherDatabase mustBe 1
      val res = await(repoWithInvalidEncryption.find(expensesUserDataOne.taxYear)(userOne))
      res mustBe Left(EncryptionDecryptionError(
        "Key being used is not valid. It could be due to invalid encoding, wrong length or uninitialized for decrypt Invalid AES key length: 2 bytes"))
    }
  }

  "handleEncryptionDecryptionException" should {
    "handle an exception" in {
      val res = repoWithInvalidEncryption.handleEncryptionDecryptionException(new Exception("fail"), "")
      res mustBe Left(EncryptionDecryptionError("fail"))
    }
  }


  "clear" should {
    "remove a record" in new EmptyDatabase {
      count mustBe 0
      await(repo.createOrUpdate(expensesUserDataOne)(userOne)) mustBe Right()
      count mustBe 1

      val clearAttempt: Boolean = await(repo.clear(taxYear)(userOne))
      clearAttempt mustBe true
      count mustBe 0
    }
  }

  "createOrUpdate" should {
    "fail to add a document to the collection when a mongo error occurs" in new EmptyDatabase {

      import org.mongodb.scala.model.{IndexModel, IndexOptions}

      def ensureIndexes: Future[Seq[String]] = {
        val indexes = Seq(IndexModel(ascending("taxYear"), IndexOptions().unique(true).name("fakeIndex")))
        MongoUtils.ensureIndexes(repo.collection, indexes, true)
      }

      ensureIndexes
      count mustBe 0

      val res = await(repo.createOrUpdate(expensesUserDataOne)(userOne))
      res mustBe Right()
      count mustBe 1

      val res2 = await(repo.createOrUpdate(expensesUserDataOne.copy(sessionId = "1234567890"))(userOne))
      res2 mustBe Left(MongoError("""Command failed with error 11000 (DuplicateKey): 'E11000 duplicate key error collection: income-tax-employment-frontend.expensesUserData index: fakeIndex dup key: { : 2022 }' on server localhost:27017. The full response is {"ok": 0.0, "errmsg": "E11000 duplicate key error collection: income-tax-employment-frontend.expensesUserData index: fakeIndex dup key: { : 2022 }", "code": 11000, "codeName": "DuplicateKey"}"""))
      count mustBe 1
    }

    "create a document in collection when one does not exist" in new EmptyDatabase {
      await(repo.createOrUpdate(expensesUserDataOne)(userOne)) mustBe Right()
      count mustBe 1
    }

    "update a document in collection when one already exists" in new EmptyDatabase {
      val createAttempt = await(repo.createOrUpdate(expensesUserDataOne)(userOne))
      createAttempt mustBe Right()
      count mustBe 1

      val updatedEmploymentDetails = expensesUserDataOne.expensesCya.expenses.copy(jobExpenses = Some(34234))
      val updatedEmploymentCyaModel = expensesUserDataOne.expensesCya.copy(expenses = updatedEmploymentDetails)
      val updatedEmploymentUserData = expensesUserDataOne.copy(expensesCya = updatedEmploymentCyaModel)

      await(repo.createOrUpdate(updatedEmploymentUserData)(userOne)) mustBe Right()
      count mustBe 1
    }
  }

  "find" should {
    "get a document and update the TTL" in new EmptyDatabase {
      val now = DateTime.now(DateTimeZone.UTC)
      val data = expensesUserDataOne.copy(lastUpdated = now)

      await(repo.createOrUpdate(data)(userOne)) mustBe Right()
      count mustBe 1

      val findResult = await(repo.find(data.taxYear)(userOne))

      findResult.right.get.map(_.copy(lastUpdated = data.lastUpdated)) mustBe Some(data)
      findResult.right.get.map(_.lastUpdated.isAfter(data.lastUpdated)) mustBe Some(true)
    }

    "return None when find operation succeeds but no data is found for the given inputs" in new EmptyDatabase {
      val taxYear = 2021
      await(repo.find(taxYear)(userOne)) mustBe Right(None)
    }
  }

  "the set indexes" should {
    "enforce uniqueness" in new EmptyDatabase {
      await(repo.createOrUpdate(expensesUserDataOne)(userOne)) mustBe Right()
      count mustBe 1

      private val encryptedExpensesUserData: EncryptedExpensesUserData = encryptionService.encryptExpenses(expensesUserDataOne)

      val caught = intercept[MongoWriteException](await(repo.collection.insertOne(encryptedExpensesUserData).toFuture()))

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
