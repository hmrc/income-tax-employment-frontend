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

import java.util.concurrent.TimeUnit

import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.set
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.User
import models.mongo.EmploymentUserData
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Indexes.{ascending, compoundIndex}
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions, IndexModel, IndexOptions}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmploymentUserDataRepositoryImpl @Inject()(mongo: MongoComponent, appConfig: AppConfig)(implicit ec: ExecutionContext
) extends PlayMongoRepository[EmploymentUserData](
  mongoComponent = mongo,
  collectionName = "employmentUserData",
  domainFormat   = EmploymentUserData.formats,
  indexes        = EmploymentUserDataIndexes.indexes(appConfig)
) with Repository with EmploymentUserDataRepository {

  def create[T](userData: EmploymentUserData)(implicit user: User[T]): Future[Boolean] = collection.insertOne(userData).toFutureOption().map(_.isDefined)

  def find[T](taxYear: Int, employmentId: String)(implicit user: User[T]): Future[Option[EmploymentUserData]] = {
    collection.findOneAndUpdate(
      filter = filter(user.sessionId,user.mtditid,user.nino,taxYear,employmentId),
      update = set("lastUpdated", toBson(DateTime.now(DateTimeZone.UTC))(MongoJodaFormats.dateTimeWrites)),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFutureOption()
  }

  def update(employmentUserData: EmploymentUserData): Future[Boolean] = {
    collection.findOneAndReplace(
      filter = filter(employmentUserData.sessionId,employmentUserData.mtdItId,
        employmentUserData.nino,employmentUserData.taxYear, employmentUserData.employmentId),
      replacement = employmentUserData,
      options = FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFutureOption().map(_.isDefined)
  }

  def clear(taxYear: Int, employmentId: String)(implicit user: User[_]): Future[Boolean] = collection.deleteOne(
    filter = filter(user.sessionId, user.mtditid, user.nino, taxYear, employmentId)
  ).toFutureOption().map(_.isDefined)
}

private object EmploymentUserDataIndexes {

  private val lookUpIndex: Bson = compoundIndex(
    ascending("sessionId"),
    ascending("mtdItId"),
    ascending("nino"),
    ascending("taxYear"),
    ascending("employmentId")
  )

  def indexes(appConfig: AppConfig): Seq[IndexModel] = {
    Seq(
      IndexModel(lookUpIndex, IndexOptions().unique(true).name("UserDataLookupIndex")),
      IndexModel(ascending("lastUpdated"), IndexOptions().expireAfter(appConfig.mongoTTL, TimeUnit.MINUTES).name("UserDataTTL"))
    )
  }
}

trait EmploymentUserDataRepository {

  def create[T](userData: EmploymentUserData)(implicit user: User[T]): Future[Boolean]
  def find[T](taxYear: Int, employmentId: String)(implicit user: User[T]): Future[Option[EmploymentUserData]]
  def update(employmentUserData: EmploymentUserData): Future[Boolean]
  def clear(taxYear: Int, employmentId: String)(implicit user: User[_]): Future[Boolean]
}
