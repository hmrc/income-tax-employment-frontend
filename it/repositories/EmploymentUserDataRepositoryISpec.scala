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

import models.User
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.joda.time.DateTime
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.AnyContent
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import utils.IntegrationTest

class EmploymentUserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  val employmentRepo: EmploymentUserDataRepositoryImpl = app.injector.instanceOf[EmploymentUserDataRepositoryImpl]

  private def count = await(employmentRepo.collection.countDocuments().toFuture())

  class EmptyDatabase {
    await(employmentRepo.collection.drop().toFuture())
    await(employmentRepo.ensureIndexes)
  }

  val empUserData: EmploymentUserData = EmploymentUserData(
    "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
    "1234567890",
    "AA123456A",
    2022,
    "employmentId",
    isPriorSubmission = true,
    EmploymentCYAModel(
      EmploymentDetails("Employer Name",currentDataIsHmrcHeld = true),
      None
    )
  )

  implicit val request: FakeRequest[AnyContent] = fakeRequest

  "clear" should {
    "remove a record" in new EmptyDatabase {
      count mustBe 0
      val res: Boolean = await(employmentRepo.update(employmentUserData))
      res mustBe true
      count mustBe 1
      val clear: Boolean = await(employmentRepo.clear(taxYear,employmentUserData.employmentId)(User(
          employmentUserData.mtdItId,None,employmentUserData.nino,employmentUserData.sessionId, AffinityGroup.Individual.toString)))
      clear mustBe true
      count mustBe 0
    }
  }

  "create" should {
    "create a record" in new EmptyDatabase {
      count mustBe 0
      val res: Boolean = await(employmentRepo.create(employmentUserData)(User(
        employmentUserData.mtdItId,None,employmentUserData.nino,employmentUserData.sessionId, AffinityGroup.Individual.toString)))
      res mustBe true
      count mustBe 1
      val duplicateAttempt: Boolean = await(employmentRepo.create(employmentUserData)(User(
        employmentUserData.mtdItId,None,employmentUserData.nino,employmentUserData.sessionId, AffinityGroup.Individual.toString)))
      duplicateAttempt mustBe false
      count mustBe 1
    }
  }

  "update" should {
    "add a document to the collection" in new EmptyDatabase {
      count mustBe 0
      val res: Boolean = await(employmentRepo.update(empUserData))
      res mustBe true
      count mustBe 1
      val data: Option[EmploymentUserData] = await(employmentRepo.find(taxYear,employmentId = "employmentId")(User(
        empUserData.mtdItId,None,empUserData.nino,empUserData.sessionId, AffinityGroup.Individual.toString)))
      data.map(_.copy(lastUpdated = DateTime.parse("2021-05-17T14:01:52.634Z"))) mustBe Some(
        empUserData.copy(lastUpdated = DateTime.parse("2021-05-17T14:01:52.634Z"))
      )
    }
    "update a document in the collection" in {
      val newUserData = empUserData.copy(employment = empUserData.employment.copy(
        employmentDetails = empUserData.employment.employmentDetails.copy(employerName = "Name 2")))
      count mustBe 1
      val res: Boolean = await(employmentRepo.update(newUserData))
      res mustBe true
      count mustBe 1
      val data: Option[EmploymentUserData] = await(employmentRepo.find(taxYear,employmentId = "employmentId")(User(
        empUserData.mtdItId,None,empUserData.nino,empUserData.sessionId, AffinityGroup.Individual.toString)))
      data.map(_.copy(lastUpdated = DateTime.parse("2021-05-17T14:01:52.634Z"))) mustBe Some(
        newUserData.copy(lastUpdated = DateTime.parse("2021-05-17T14:01:52.634Z"))
      )
    }
    "insert a new document to the collection if the sessionId is different" in {
      val newUserData = empUserData.copy(sessionId = "sessionId-000001")
      count mustBe 1
      val res: Boolean = await(employmentRepo.update(newUserData))
      res mustBe true
      count mustBe 2
    }
  }

  "find" should {

    def filter(sessionId: String, mtdItId: String, nino: String, taxYear: Int): Bson = org.mongodb.scala.model.Filters.and(
      org.mongodb.scala.model.Filters.equal("sessionId", toBson(sessionId)),
      org.mongodb.scala.model.Filters.equal("mtdItId", toBson(mtdItId)),
      org.mongodb.scala.model.Filters.equal("nino", toBson(nino)),
      org.mongodb.scala.model.Filters.equal("taxYear", toBson(taxYear))
    )

    "get a document and update the TTL" in {
      count mustBe 2
      val dataBefore: EmploymentUserData = await(employmentRepo.collection.find(
        filter(empUserData.sessionId,empUserData.mtdItId,empUserData.nino,empUserData.taxYear)
      ).toFuture()).head
      val dataAfter: Option[EmploymentUserData] = await(employmentRepo.find(taxYear,employmentId = "employmentId")(User(
        empUserData.mtdItId,None,empUserData.nino,empUserData.sessionId
        , AffinityGroup.Individual.toString)))

      dataAfter.map(_.copy(lastUpdated = dataBefore.lastUpdated)) mustBe Some(dataBefore)
      dataAfter.map(_.lastUpdated.isAfter(dataBefore.lastUpdated)) mustBe Some(true)
    }
  }

  "the set indexes" should {
    "enforce uniqueness" in {
      val result: Either[Exception,InsertOneResult] = try {
        Right(await(employmentRepo.collection.insertOne(empUserData).toFuture()))
      } catch {
        case e: Exception => Left(e)
      }
      result.isLeft mustBe true
      result.left.get.getMessage must include(
        "E11000 duplicate key error collection: income-tax-employment-frontend.employmentUserData index: UserDataLookupIndex dup key:")
    }
  }
}
