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

package repositories

import com.mongodb.MongoTimeoutException
import common.UUID
import models.benefits._
import models.details.EmploymentDetails
import models.mongo._
import models.{AuthorisationRequest, User}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.{MongoException, MongoInternalException, MongoWriteException}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.AnyContent
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.mongo.MongoUtils
import utils.PagerDutyHelper.PagerDutyKeys.FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA
import utils.{AesGcmAdCrypto, IntegrationTest}

import java.time.{Clock, Duration, LocalDateTime, ZoneId}
import scala.concurrent.Future

class EmploymentUserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  private val employmentRepo: EmploymentUserDataRepositoryImpl = app.injector.instanceOf[EmploymentUserDataRepositoryImpl]
  private implicit val secureGCMCipher: AesGcmAdCrypto = app.injector.instanceOf[AesGcmAdCrypto]

  private def count: Long = await(employmentRepo.collection.countDocuments().toFuture())

  private def find(employmentUserData: EmploymentUserData)
                  (implicit authorisationRequest: AuthorisationRequest[_]): Future[Option[EncryptedEmploymentUserData]] = {
    employmentRepo.collection
      .find(filter = Repository.filter(authorisationRequest.user.sessionId, authorisationRequest.user.mtditid, authorisationRequest.user.nino,
        employmentUserData.taxYear, employmentUserData.employmentId))
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

  private val millisecondClock = Clock.tick(Clock.systemDefaultZone, Duration.ofNanos(1000000))
  private val now = LocalDateTime.now(millisecondClock)
  private val employerName = "some-employer-name"

  val employmentUserDataOne: EmploymentUserData = EmploymentUserData(
    sessionIdOne,
    mtditid,
    nino,
    taxYear,
    employmentIdOne,
    isPriorSubmission = true,
    hasPriorBenefits = true, hasPriorStudentLoans = true,
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = true), None),
    lastUpdated = now
  )

  val amount = 66
  val benefitsViewModel: BenefitsViewModel = BenefitsViewModel(
    Some(CarVanFuelModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount),
      Some(true), Some(amount), Some(true), Some(amount))),
    Some(AccommodationRelocationModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount))),
    Some(TravelEntertainmentModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount))),
    Some(UtilitiesAndServicesModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount))),
    Some(MedicalChildcareEducationModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount))),
    Some(IncomeTaxAndCostsModel(Some(true), Some(true), Some(amount), Some(true), Some(amount))),
    Some(ReimbursedCostsVouchersAndNonCashModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount))),
    Some(AssetsModel(Some(true), Some(true), Some(amount), Some(true), Some(amount))),
    Some(s"${taxYearEOY - 1}-10-10"), isUsingCustomerData = true, isBenefitsReceived = true)

  val employmentUserDataFull: EmploymentUserData = EmploymentUserData(
    sessionIdOne,
    mtditid,
    nino,
    taxYear,
    employmentIdOne,
    isPriorSubmission = true,
    hasPriorBenefits = true, hasPriorStudentLoans = true,
    EmploymentCYAModel(
      EmploymentDetails(
        employerName = "Name",
        employerRef = Some("Ref"),
        startDate = Some(s"${taxYearEOY - 1}-01-10"),
        payrollId = Some("12345"),
        didYouLeaveQuestion = Some(false),
        cessationDate = Some(s"$taxYearEOY-01-01"),
        dateIgnored = Some(s"$taxYearEOY-02-02"),
        employmentSubmittedOn = Some(s"$taxYearEOY-02-02"),
        employmentDetailsSubmittedOn = Some(s"$taxYearEOY-02-02"),
        taxablePayToDate = Some(55.00),
        totalTaxToDate = Some(55.00),
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
    taxYear,
    employmentIdTwo,
    isPriorSubmission = true,
    hasPriorBenefits = true, hasPriorStudentLoans = true,
    EmploymentCYAModel(
      EmploymentDetails("Argos", currentDataIsHmrcHeld = true),
      None
    ),
    lastUpdated = now
  )

  implicit val request: FakeRequest[AnyContent] = FakeRequest()

  private val authRequestOne = AuthorisationRequest(User(employmentUserDataOne.mtdItId, None, employmentUserDataOne.nino, employmentUserDataOne.sessionId, Individual.toString), request)
  private val authRequestTwo = AuthorisationRequest(User(employmentUserDataTwo.mtdItId, None, employmentUserDataTwo.nino, employmentUserDataTwo.sessionId, Individual.toString), request)

  private val repoWithInvalidEncryption = appWithInvalidEncryptionKey.injector.instanceOf[EmploymentUserDataRepositoryImpl]

  "update with invalid encryption" should {
    "fail to add data" in new EmptyDatabase {
      countFromOtherDatabase mustBe 0
      val res: Either[DatabaseError, Unit] = await(repoWithInvalidEncryption.createOrUpdate(employmentUserDataOne))
      res mustBe Left(EncryptionDecryptionError("Failed encrypting data"))
    }
  }

  "find with invalid encryption" should {
    "fail to find data" in new EmptyDatabase {
      implicit val associatedText: String = employmentUserDataOne.mtdItId
      countFromOtherDatabase mustBe 0
      await(repoWithInvalidEncryption.collection.insertOne(employmentUserDataOne.encrypted).toFuture())
      countFromOtherDatabase mustBe 1
      private val res = await(repoWithInvalidEncryption.find(employmentUserDataOne.taxYear, employmentIdOne, authRequestOne.user))
      res mustBe Left(EncryptionDecryptionError("Failed encrypting data"))
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
      await(employmentRepo.createOrUpdate(employmentUserDataOne)) mustBe Right(())
      count mustBe 1

      await(employmentRepo.clear(taxYear, employmentUserDataOne.employmentId, authRequestOne.user)) mustBe true
      count mustBe 0
    }
  }

  "createOrUpdate" should {
    "fail to add a document to the collection when a mongo error occurs" in new EmptyDatabase {

      def ensureIndexes: Future[Seq[String]] = {
        val indexes = Seq(IndexModel(ascending("taxYear"), IndexOptions().unique(true).name("fakeIndex")))
        MongoUtils.ensureIndexes(employmentRepo.collection, indexes, replaceIndexes = true)
      }

      await(ensureIndexes)
      count mustBe 0

      private val res = await(employmentRepo.createOrUpdate(employmentUserDataOne))
      res mustBe Right(())
      count mustBe 1

      private val res2 = await(employmentRepo.createOrUpdate(employmentUserDataOne.copy(sessionId = "1234567890")))
      res2.left.toOption.get.message must include("Command failed with error 11000 (DuplicateKey)")
      count mustBe 1
    }

    "create a document in collection when one does not exist" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataOne)) mustBe Right(())
      count mustBe 1
    }

    "create a document in collection with all fields present" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataFull)) mustBe Right(())
      count mustBe 1
    }

    "update a document in collection when one already exists" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataOne)) mustBe Right(())
      count mustBe 1

      private val updatedEmploymentDetails = employmentUserDataOne.employment.employmentDetails.copy(employerName = "Different_Employer_Name")
      private val updatedEmploymentCyaModel = employmentUserDataOne.employment.copy(employmentDetails = updatedEmploymentDetails)
      private val updatedEmploymentUserData = employmentUserDataOne.copy(employment = updatedEmploymentCyaModel)

      await(employmentRepo.createOrUpdate(updatedEmploymentUserData)) mustBe Right(())
      count mustBe 1
    }

    "update a single document when one already exists and collection has multiple documents" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataOne)) mustBe Right(())
      await(employmentRepo.createOrUpdate(employmentUserDataTwo)) mustBe Right(())
      count mustBe 2

      private val updatedEmploymentDetails = employmentUserDataOne.employment.employmentDetails.copy(employerName = "Different_Employer_Name")
      private val updatedEmploymentCyaModel = employmentUserDataOne.employment.copy(employmentDetails = updatedEmploymentDetails)
      private val updatedEmploymentUserDataOne = employmentUserDataOne.copy(employment = updatedEmploymentCyaModel)

      await(employmentRepo.createOrUpdate(updatedEmploymentUserDataOne)) mustBe Right(())

      count mustBe 2
      private val maybeData: Option[EncryptedEmploymentUserData] = await(find(employmentUserDataTwo)(authRequestTwo))
      implicit val associatedText: String = maybeData.get.mtdItId
      maybeData.get.decrypted mustBe employmentUserDataTwo
    }

    "create a new document when the same documents exists but the sessionId is different" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataOne)) mustBe Right(())
      count mustBe 1

      private val newUserData = employmentUserDataOne.copy(sessionId = UUID.randomUUID)

      await(employmentRepo.createOrUpdate(newUserData)) mustBe Right(())
      count mustBe 2
    }
  }

  "find" should {
    "get a document and update the TTL" in new EmptyDatabase {
      private val now = LocalDateTime.now(ZoneId.of("UTC"))
      private val data = employmentUserDataOne.copy(lastUpdated = now)

      await(employmentRepo.createOrUpdate(data)) mustBe Right(())
      count mustBe 1

      private val findResult = await(employmentRepo.find(data.taxYear, data.employmentId, authRequestOne.user))

      findResult.toOption.get.map(_.copy(lastUpdated = data.lastUpdated)) mustBe Some(data)
      findResult.toOption.get.map(_.lastUpdated.isAfter(data.lastUpdated)) mustBe Some(true)
    }

    "find a document in collection with all fields present" in new EmptyDatabase {
      await(employmentRepo.createOrUpdate(employmentUserDataFull)) mustBe Right(())
      count mustBe 1

      val findResult: Either[DatabaseError, Option[EmploymentUserData]] = {
        await(employmentRepo.find(employmentUserDataFull.taxYear, employmentUserDataFull.employmentId, authRequestOne.user))
      }

      findResult mustBe Right(Some(employmentUserDataFull.copy(lastUpdated = findResult.toOption.get.get.lastUpdated)))
    }

    "return None when find operation succeeds but no data is found for the given inputs" in new EmptyDatabase {
      val taxYear: Int = taxYearEOY
      await(employmentRepo.find(taxYear, "employmentId", authRequestOne.user)) mustBe Right(None)
    }
  }

  "the set indexes" should {
    "enforce uniqueness" in new EmptyDatabase {
      implicit val associatedText: String = employmentUserDataOne.mtdItId
      await(employmentRepo.createOrUpdate(employmentUserDataOne)) mustBe Right(())
      count mustBe 1

      private val encryptedEmploymentUserData: EncryptedEmploymentUserData = employmentUserDataOne.encrypted

      private val caught = intercept[MongoWriteException](await(employmentRepo.collection.insertOne(encryptedEmploymentUserData).toFuture()))

      caught.getMessage must
        include("E11000 duplicate key error collection: income-tax-employment-frontend.employmentUserData index: UserDataLookupIndex dup key:")
    }
  }

  "mongoRecover" should {
    Seq(new MongoTimeoutException(""), new MongoInternalException(""), new MongoException("")).foreach { exception =>
      s"recover when the exception is a MongoException or a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result =
          Future.failed(exception)
            .recover(employmentRepo.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA, authRequestOne.user))

        await(result) mustBe None
      }
    }

    Seq(new NullPointerException(""), new RuntimeException("")).foreach { exception =>
      s"not recover when the exception is not a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result =
          Future.failed(exception)
            .recover(employmentRepo.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_EMPLOYMENT_DATA, authRequestOne.user))

        assertThrows[RuntimeException] {
          await(result)
        }
      }
    }
  }
}
