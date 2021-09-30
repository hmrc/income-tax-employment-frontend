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
import models.employment.{BenefitsViewModel, CarVanFuelModel}
import models.mongo._
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.{MongoException, MongoInternalException, MongoWriteException}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.AnyContent
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import services.EncryptionService
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.mongo.MongoUtils
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

  private def countFromOtherDatabase = await(employmentRepo.collection.countDocuments().toFuture())

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

  val amount = 66
  val benefitsViewModel: BenefitsViewModel = BenefitsViewModel(
    Some(CarVanFuelModel(
      Some(true), Some(true), Some(100), Some(true), Some(100), Some(true), Some(100), Some(true), Some(100), Some(true), Some(100)
    )
    ), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount),
    Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount),
    Some(amount), Some(amount), Some(amount), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true),
    Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true),
    Some(true), Some(true), Some(true), Some("2020-10-10"), isUsingCustomerData = true, true)

  val employmentUserDataFull: EmploymentUserData = EmploymentUserData(
    sessionIdOne,
    mtditid,
    nino,
    2022,
    employmentIdOne,
    isPriorSubmission = true,
    EmploymentCYAModel(
      EmploymentDetails(
        employerName = "Name",
        employerRef = Some("Ref"),
        startDate = Some("2020-01-10"),
        payrollId = Some("12345"),
        cessationDateQuestion = Some(false),
        cessationDate = Some("2021-01-01"),
        dateIgnored = Some("2021-02-02"),
        employmentSubmittedOn = Some("2021-02-02"),
        employmentDetailsSubmittedOn = Some("2021-02-02"),
        taxablePayToDate = Some(55),
        totalTaxToDate = Some(55),
        currentDataIsHmrcHeld = false
      ),
      Some(
        benefitsViewModel
      )
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

  val repoWithInvalidEncryption = appWithInvalidEncryptionKey.injector.instanceOf[EmploymentUserDataRepositoryImpl]
  val serviceWithInvalidEncryption: EncryptionService = appWithInvalidEncryptionKey.injector.instanceOf[EncryptionService]

  "update with invalid encryption" should {
    "fail to add data" in new EmptyDatabase {
      countFromOtherDatabase mustBe 0
      val res: Either[DatabaseError, Unit] = await(repoWithInvalidEncryption.createOrUpdate(employmentUserDataOne)(userOne))
      res mustBe Left(EncryptionDecryptionError(
        "Key being used is not valid. It could be due to invalid encoding, wrong length or uninitialized for encrypt Invalid AES key length: 2 bytes"))
    }
  }

  "find with invalid encryption" should {
    "fail to find data" in new EmptyDatabase {
      countFromOtherDatabase mustBe 0
      await(repoWithInvalidEncryption.collection.insertOne(encryptionService.encryptUserData(employmentUserDataOne)).toFuture())
      countFromOtherDatabase mustBe 1
      val res = await(repoWithInvalidEncryption.find(employmentUserDataOne.taxYear,employmentIdOne)(userOne))
      res mustBe Left(EncryptionDecryptionError(
        "Key being used is not valid. It could be due to invalid encoding, wrong length or uninitialized for decrypt Invalid AES key length: 2 bytes"))
    }
  }

  "handleEncryptionDecryptionException" should {
    "handle an exception" in {
      val res = repoWithInvalidEncryption.handleEncryptionDecryptionException(new Exception("fail"),"")
      res mustBe Left(EncryptionDecryptionError("fail"))
    }
  }

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
    "fail to add a document to the collection when a mongo error occurs" in new EmptyDatabase {

      def ensureIndexes: Future[Seq[String]] = {
        val indexes = Seq(IndexModel(ascending("taxYear"), IndexOptions().unique(true).name("fakeIndex")))
        MongoUtils.ensureIndexes(employmentRepo.collection, indexes, true)
      }

      await(ensureIndexes)
      count mustBe 0

      val res = await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne))
      res mustBe Right()
      count mustBe 1

      val res2 = await(employmentRepo.createOrUpdate(employmentUserDataOne.copy(sessionId = "1234567890"))(userOne))
      res2.left.get.message must include("Command failed with error 11000 (DuplicateKey)")
      count mustBe 1
    }

    "create a document in collection when one does not exist" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataOne)(userOne)) mustBe Right()
      count mustBe 1
    }

    "create a document in collection with all fields present" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataFull)(userOne)) mustBe Right()
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

    "find a document in collection with all fields present" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataFull)(userOne)) mustBe Right()
      count mustBe 1

      val findResult: Either[DatabaseError, Option[EmploymentUserData]] = {
        await(employmentRepo.find(employmentUserDataFull.taxYear, employmentUserDataFull.employmentId)(userOne))
      }

      findResult mustBe Right(Some(employmentUserDataFull.copy(lastUpdated = findResult.right.get.get.lastUpdated)))
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
