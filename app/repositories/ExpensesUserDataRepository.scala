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

import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.set
import config.AppConfig
import models.User
import models.mongo._
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.MongoException
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.AesGcmAdCrypto
import utils.PagerDutyHelper.PagerDutyKeys.{FAILED_TO_CREATE_UPDATE_EXPENSES_DATA, FAILED_TO_ClEAR_EXPENSES_DATA, FAILED_TO_FIND_EXPENSES_DATA}
import utils.PagerDutyHelper.{PagerDutyKeys, pagerDutyLog}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ExpensesUserDataRepositoryImpl @Inject()(mongo: MongoComponent, appConfig: AppConfig)
                                              (implicit ec: ExecutionContext, aesGcmAdCrypto: AesGcmAdCrypto)
  extends PlayMongoRepository[EncryptedExpensesUserData](
    mongoComponent = mongo,
    collectionName = "expensesUserData",
    domainFormat = EncryptedExpensesUserData.format,
    indexes = ExpensesUserDataIndexes.indexes(appConfig)
  ) with Repository with ExpensesUserDataRepository with Logging {

  def find(taxYear: Int, user: User): Future[Either[DatabaseError, Option[ExpensesUserData]]] = {

    lazy val start = "[ExpensesUserDataRepositoryImpl][find]"

    val queryFilter = filterExpenses(user.sessionId, user.mtditid, user.nino, taxYear)
    val update = set("lastUpdated", toBson(DateTime.now(DateTimeZone.UTC))(MongoJodaFormats.dateTimeWrites))
    val options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)

    val findResult = collection.findOneAndUpdate(queryFilter, update, options).toFutureOption().map {
      case Some(data) => Right(Some(data))
      case None =>
        logger.info(s"[ExpensesUserDataRepositoryImpl][find] No employment CYA data found for user. SessionId: ${user.sessionId}")
        Right(None)
    }.recover {
      case exception: Exception =>
        pagerDutyLog(FAILED_TO_FIND_EXPENSES_DATA, s"$start Failed to find expenses user data. Exception: ${exception.getMessage}")
        Left(MongoError(exception.getMessage))
    }

    findResult.map {
      case Left(error) => Left(error)
      case Right(encryptedData) =>
        Try {
          encryptedData.map { encryptedExpensesUserData: EncryptedExpensesUserData =>
            implicit val associatedText: String = encryptedExpensesUserData.mtdItId
            encryptedExpensesUserData.decrypted
          }
        }.toEither match {
          case Left(t: Throwable) => handleEncryptionDecryptionException(t.asInstanceOf[Exception], start)
          case Right(decryptedData) => Right(decryptedData)
        }
    }
  }

  def createOrUpdate(expensesUserData: ExpensesUserData): Future[Either[DatabaseError, Unit]] = {
    lazy val start = "[ExpensesUserDataRepositoryImpl][update]"

    Try {
      implicit val associatedText: String = expensesUserData.mtdItId
      expensesUserData.encrypted
    }.toEither match {
      case Left(t: Throwable) => Future.successful(handleEncryptionDecryptionException(t.asInstanceOf[Exception], start))
      case Right(encryptedData) =>

        val queryFilter = filterExpenses(encryptedData.sessionId, encryptedData.mtdItId, encryptedData.nino, encryptedData.taxYear)
        val replacement = encryptedData
        val options = FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)

        collection.findOneAndReplace(queryFilter, replacement, options).toFutureOption().map {
          case Some(_) => Right(())
          case None =>
            pagerDutyLog(FAILED_TO_CREATE_UPDATE_EXPENSES_DATA, s"$start Failed to update user data.")
            Left(DataNotUpdatedError)
        }.recover {
          case exception: Exception =>
            pagerDutyLog(FAILED_TO_CREATE_UPDATE_EXPENSES_DATA, s"$start Failed to update expenses user data. Exception: ${exception.getMessage}")
            Left(MongoError(exception.getMessage))
        }
    }
  }

  def clear(taxYear: Int, user: User): Future[Boolean] =
    collection.deleteOne(filterExpenses(user.sessionId, user.mtditid, user.nino, taxYear))
      .toFutureOption()
      .recover(mongoRecover("Clear", FAILED_TO_ClEAR_EXPENSES_DATA, user))
      .map(_.exists(_.wasAcknowledged()))

  def mongoRecover[T](operation: String,
                      pagerDutyKey: PagerDutyKeys.Value,
                      user: User): PartialFunction[Throwable, Option[T]] = new PartialFunction[Throwable, Option[T]] {

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
  def createOrUpdate(expensesUserData: ExpensesUserData): Future[Either[DatabaseError, Unit]]

  def find(taxYear: Int, user: User): Future[Either[DatabaseError, Option[ExpensesUserData]]]

  def clear(taxYear: Int, user: User): Future[Boolean]
}
