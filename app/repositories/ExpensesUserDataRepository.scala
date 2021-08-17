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

import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.set
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.User
import models.mongo.ExpensesUserData
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.MongoException
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.PagerDutyHelper.PagerDutyKeys.{FAILED_TO_CREATE_UPDATE_EXPENSES_DATA, FAILED_TO_ClEAR_EXPENSES_DATA, FAILED_TO_FIND_EXPENSES_DATA}
import utils.PagerDutyHelper.{PagerDutyKeys, pagerDutyLog}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExpensesUserDataRepositoryImpl @Inject()(mongo: MongoComponent, appConfig: AppConfig)(implicit ec: ExecutionContext
) extends PlayMongoRepository[ExpensesUserData](
  mongoComponent = mongo,
  collectionName = "expensesUserData",
  domainFormat   = ExpensesUserData.format,
  indexes        = ExpensesUserDataIndexes.indexes(appConfig)
) with Repository with ExpensesUserDataRepository with Logging {

  def find[T](taxYear: Int)(implicit user: User[T]): Future[Option[ExpensesUserData]] = {
    val queryFilter = filterExpenses(user.sessionId, user.mtditid, user.nino, taxYear)
    val update = set("lastUpdated", toBson(DateTime.now(DateTimeZone.UTC))(MongoJodaFormats.dateTimeWrites))
    val options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)

    collection.findOneAndUpdate(queryFilter, update, options)
      .toFutureOption()
      .map{
        case Some(data) => Some(data)
        case None =>
          logger.info(s"[ExpensesUserDataRepositoryImpl][find] No employment CYA data found for user. SessionId: ${user.sessionId}")
          None
      }.recover(mongoRecover("Find", FAILED_TO_FIND_EXPENSES_DATA))
  }

  def createOrUpdate[T](expensesUserData: ExpensesUserData)(implicit user: User[T]): Future[Option[ExpensesUserData]] = {
    val queryFilter = filterExpenses(expensesUserData.sessionId, expensesUserData.mtdItId, expensesUserData.nino, expensesUserData.taxYear)
    val replacement = expensesUserData
    val options = FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)

    collection.findOneAndReplace(queryFilter, replacement, options)
      .toFutureOption()
      .recover(mongoRecover("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_EXPENSES_DATA))
  }

  def clear[T](taxYear: Int)(implicit user: User[T]): Future[Boolean] =
    collection.deleteOne(filterExpenses(user.sessionId, user.mtditid, user.nino, taxYear))
      .toFutureOption()
      .recover(mongoRecover("Clear", FAILED_TO_ClEAR_EXPENSES_DATA))
      .map(_.exists(_.wasAcknowledged()))

  def mongoRecover[T](operation: String, pagerDutyKey: PagerDutyKeys.Value)
                     (implicit user: User[_]): PartialFunction[Throwable, Option[T]] = new PartialFunction[Throwable, Option[T]] {

    override def isDefinedAt(x: Throwable): Boolean = x.isInstanceOf[MongoException]

    override def apply(e: Throwable): Option[T] = {
      pagerDutyLog(
        pagerDutyKey,
        s"[ExpensesUserDataRepositoryImpl][$operation] Failed to create employment expenses user data. Error:${e.getMessage}. SessionId: ${user.sessionId}"
      )
      None
    }
  }
}

trait ExpensesUserDataRepository {
  def createOrUpdate[T](expensesUserData: ExpensesUserData)(implicit user: User[T]): Future[Option[ExpensesUserData]]
  def find[T](taxYear: Int)(implicit user: User[T]): Future[Option[ExpensesUserData]]
  def clear[T](taxYear: Int)(implicit user: User[T]): Future[Boolean]
}
