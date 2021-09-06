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
import models.employment.BenefitsViewModel
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.{MongoException, MongoInternalException, MongoWriteException}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.AnyContent
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import utils.IntegrationTest
import utils.PagerDutyHelper.PagerDutyKeys.FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA

import scala.concurrent.Future

class EmploymentUserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  val employmentRepo: EmploymentUserDataRepositoryImpl = app.injector.instanceOf[EmploymentUserDataRepositoryImpl]

  private def count = await(employmentRepo.collection.countDocuments().toFuture())

  private def find(employmentUserData: EmploymentUserData)(implicit user: User[_]): Future[Option[EmploymentUserData]] = {
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
      BenefitsViewModel(isUsingCustomerData = false)
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
      BenefitsViewModel(isUsingCustomerData = false)
    ),
    lastUpdated = now
  )

  implicit val request: FakeRequest[AnyContent] = fakeRequest

  val userOne = User(employmentUserDataOne.mtdItId, None, employmentUserDataOne.nino, employmentUserDataOne.sessionId, AffinityGroup.Individual.toString)
  val userTwo = User(employmentUserDataTwo.mtdItId, None, employmentUserDataTwo.nino, employmentUserDataTwo.sessionId, AffinityGroup.Individual.toString)

  "clear" should {
    "remove a record" in new EmptyDatabase {
      count mustBe 0
      val createAttempt: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne))
      createAttempt mustBe Some(employmentUserDataOne)
      count mustBe 1

      val clearAttempt: Boolean = await(employmentRepo.clear(taxYear, employmentUserDataOne.employmentId)(userOne))
      clearAttempt mustBe true
      count mustBe 0
    }
  }

  "createOrUpdate" should {

    "create a document in collection when one does not exist" in new EmptyDatabase {
      val createAttempt: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne))
      createAttempt mustBe Some(employmentUserDataOne)
      count mustBe 1
    }

    "update a document in collection when one already exists" in new EmptyDatabase {
      val createAttempt: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne))
      createAttempt.get mustBe employmentUserDataOne
      count mustBe 1

      val updatedEmploymentDetails = employmentUserDataOne.employment.employmentDetails.copy(employerName = "Different_Employer_Name")
      val updatedEmploymentCyaModel = employmentUserDataOne.employment.copy(employmentDetails = updatedEmploymentDetails)
      val updatedEmploymentUserData = employmentUserDataOne.copy(employment = updatedEmploymentCyaModel)

      val updateAttempt: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(updatedEmploymentUserData)(userOne))
      updateAttempt mustBe Some(updatedEmploymentUserData)
      count mustBe 1
    }

    "update a single document when one already exists and collection has multiple documents" in new EmptyDatabase {
      val createOne: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne))
      val createTwo: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(employmentUserDataTwo)(userTwo))

      createOne mustBe Some(employmentUserDataOne)
      createTwo mustBe Some(employmentUserDataTwo)
      count mustBe 2

      val updatedEmploymentDetails = employmentUserDataOne.employment.employmentDetails.copy(employerName = "Different_Employer_Name")
      val updatedEmploymentCyaModel = employmentUserDataOne.employment.copy(employmentDetails = updatedEmploymentDetails)
      val updatedEmploymentUserDataOne = employmentUserDataOne.copy(employment = updatedEmploymentCyaModel)

      val updateAttempt: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(updatedEmploymentUserDataOne)(userOne))
      updateAttempt mustBe Some(updatedEmploymentUserDataOne)

      count mustBe 2
      await(find(employmentUserDataTwo)(userTwo)) mustBe createTwo
    }

    "create a new document when the same documents exists but the sessionId is different" in new EmptyDatabase {
      val createOne: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne))
      createOne mustBe Some(employmentUserDataOne)
      count mustBe 1

      val newUserData = employmentUserDataOne.copy(sessionId = UUID.randomUUID)
      val createSameWithDifferentSessionId: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(newUserData)(userOne))

      createSameWithDifferentSessionId mustBe Some(newUserData)
      count mustBe 2
    }
  }

  "find" should {
    "get a document and update the TTL" in new EmptyDatabase {
      val now = DateTime.now(DateTimeZone.UTC)
      val data = employmentUserDataOne.copy(lastUpdated = now)

      val createResult: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(data)(userOne))
      createResult mustBe Some(data)
      count mustBe 1

      val findResult = await(employmentRepo.find(data.taxYear, data.employmentId)(userOne))

      findResult.map(_.copy(lastUpdated = data.lastUpdated)) mustBe Some(data)
      findResult.map(_.lastUpdated.isAfter(data.lastUpdated)) mustBe Some(true)
    }

    "return None when find operation succeeds but no data is found for the given inputs" in new EmptyDatabase {
      val taxYear = 2021
      val findResult = await(employmentRepo.find(taxYear, "employmentId")(userOne))

      findResult mustBe None
    }
  }

  "the set indexes" should {
    "enforce uniqueness" in new EmptyDatabase {
      val createResult: Option[EmploymentUserData] = await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne))
      createResult mustBe Some(employmentUserDataOne)
      count mustBe 1

      val caught = intercept[MongoWriteException](await(employmentRepo.collection.insertOne(employmentUserDataOne).toFuture()))

      caught.getMessage must include("E11000 duplicate key error collection: income-tax-employment-frontend.employmentUserData index: UserDataLookupIndex dup key:")
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
