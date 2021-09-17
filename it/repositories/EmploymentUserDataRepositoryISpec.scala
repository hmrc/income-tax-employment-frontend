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
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData, EncryptedEmploymentUserData}
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.{MongoException, MongoInternalException, MongoWriteException}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.AnyContent
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import services.EncryptionService
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import utils.IntegrationTest
import utils.PagerDutyHelper.PagerDutyKeys.FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA

import scala.concurrent.Future

class EmploymentUserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  private val employmentRepo: EmploymentUserDataRepositoryImpl = app.injector.instanceOf[EmploymentUserDataRepositoryImpl]
  private val encryptionService = app.injector.instanceOf[EncryptionService]

  private def count = await(employmentRepo.collection.countDocuments().toFuture())

  private def find(employmentUserData: EmploymentUserData)(implicit user: User[_]): Future[Option[EncryptedEmploymentUserData]] = {
    employmentRepo.collection
      .find(filter = Repository.filter(user.sessionId, user.mtditid, user.nino, employmentUserData.taxYear, employmentUserData.employmentId))
      .toFuture()
      .map(_.headOption)
  }

  class EmptyDatabase {
    await(employmentRepo.collection.drop().toFuture())
    await(employmentRepo.ensureIndexes)
    count mustBe 0
  }

  private val employmentIdOne = UUID.randomUUID
  private val sessionIdOne = UUID.randomUUID

  private val employmentIdTwo = UUID.randomUUID
  private val sessionIdTwo = UUID.randomUUID

  private val now = DateTime.now(DateTimeZone.UTC)

  val employmentUserDataOne: EmploymentUserData = EmploymentUserData(
    sessionIdOne,
    mtditid,
    nino,
    2022,
    employmentIdOne,
    isPriorSubmission = true,
    EmploymentCYAModel(
      EmploymentDetails("Tesco", currentDataIsHmrcHeld = true),
      None
    ),
    lastUpdated = now
  )

  val employmentUserDataTwo: EmploymentUserData = EmploymentUserData(
    sessionIdTwo,
    mtditid,
    nino,
    2022,
    employmentIdTwo,
    isPriorSubmission = true,
    EmploymentCYAModel(
      EmploymentDetails("Argos", currentDataIsHmrcHeld = true),
      None
    ),
    lastUpdated = now
  )

  implicit val request: FakeRequest[AnyContent] = fakeRequest

  private val userOne = User(employmentUserDataOne.mtdItId, None, employmentUserDataOne.nino, employmentUserDataOne.sessionId, Individual.toString)
  private val userTwo = User(employmentUserDataTwo.mtdItId, None, employmentUserDataTwo.nino, employmentUserDataTwo.sessionId, Individual.toString)

  "clear" should {
    "remove a record" in new EmptyDatabase {
      count mustBe 0
      await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne)) mustBe Right()
      count mustBe 1

      await(employmentRepo.clear(taxYear, employmentUserDataOne.employmentId)(userOne)) mustBe true
      count mustBe 0
    }
  }

  "createOrUpdate" should {

    "create a document in collection when one does not exist" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne)) mustBe Right()
      count mustBe 1
    }

    "update a document in collection when one already exists" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne)) mustBe Right()
      count mustBe 1

      private val updatedEmploymentDetails = employmentUserDataOne.employment.employmentDetails.copy(employerName = "Different_Employer_Name")
      private val updatedEmploymentCyaModel = employmentUserDataOne.employment.copy(employmentDetails = updatedEmploymentDetails)
      private val updatedEmploymentUserData = employmentUserDataOne.copy(employment = updatedEmploymentCyaModel)

      await(employmentRepo.createOrUpdate(updatedEmploymentUserData)(userOne)) mustBe Right()
      count mustBe 1
    }

    "update a single document when one already exists and collection has multiple documents" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne)) mustBe Right()
      await(employmentRepo.createOrUpdate(employmentUserDataTwo)(userTwo)) mustBe Right()
      count mustBe 2

      private val updatedEmploymentDetails = employmentUserDataOne.employment.employmentDetails.copy(employerName = "Different_Employer_Name")
      private val updatedEmploymentCyaModel = employmentUserDataOne.employment.copy(employmentDetails = updatedEmploymentDetails)
      private val updatedEmploymentUserDataOne = employmentUserDataOne.copy(employment = updatedEmploymentCyaModel)

      await(employmentRepo.createOrUpdate(updatedEmploymentUserDataOne)(userOne)) mustBe Right()

      count mustBe 2
      private val maybeData: Option[EncryptedEmploymentUserData] = await(find(employmentUserDataTwo)(userTwo))
      encryptionService.decryptUserData(maybeData.get) mustBe employmentUserDataTwo
    }

    "create a new document when the same documents exists but the sessionId is different" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne)) mustBe Right()
      count mustBe 1

      val newUserData = employmentUserDataOne.copy(sessionId = UUID.randomUUID)

      await(employmentRepo.createOrUpdate(newUserData)(userOne)) mustBe Right()
      count mustBe 2
    }
  }

  "find" should {
    "get a document and update the TTL" in new EmptyDatabase {
      val now = DateTime.now(DateTimeZone.UTC)
      val data = employmentUserDataOne.copy(lastUpdated = now)

      await(employmentRepo.createOrUpdate(data)(userOne)) mustBe Right()
      count mustBe 1

      val findResult = await(employmentRepo.find(data.taxYear, data.employmentId)(userOne))

      findResult.right.get.map(_.copy(lastUpdated = data.lastUpdated)) mustBe Some(data)
      findResult.right.get.map(_.lastUpdated.isAfter(data.lastUpdated)) mustBe Some(true)
    }

    "return None when find operation succeeds but no data is found for the given inputs" in new EmptyDatabase {
      val taxYear = 2021
      await(employmentRepo.find(taxYear, "employmentId")(userOne)) mustBe Right(None)
    }
  }

  "the set indexes" should {
    "enforce uniqueness" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne)) mustBe Right()
      count mustBe 1

      private val encryptedEmploymentUserData: EncryptedEmploymentUserData = encryptionService.encryptUserData(employmentUserDataOne)

      val caught = intercept[MongoWriteException](await(employmentRepo.collection.insertOne(encryptedEmploymentUserData).toFuture()))

      caught.getMessage must
        include("E11000 duplicate key error collection: income-tax-employment-frontend.employmentUserData index: UserDataLookupIndex dup key:")
    }
  }

  "mongoRecover" should {
    Seq(new MongoTimeoutException(""), new MongoInternalException(""), new MongoException("")).foreach { exception =>
      s"recover when the exception is a MongoException or a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result =
          Future.failed(exception)
            .recover(employmentRepo.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA)(userOne))

        await(result) mustBe None
      }
    }

    Seq(new NullPointerException(""), new RuntimeException("")).foreach { exception =>
      s"not recover when the exception is not a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result =
          Future.failed(exception)
            .recover(employmentRepo.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA)(userOne))

        assertThrows[RuntimeException] {
          await(result)
        }
      }
    }
  }
}
