/*
 * Copyright 2022 HM Revenue & Customs
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
import models.mongo.{DataNotUpdatedError, DatabaseError, MongoError, UserDataGateway}
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.MongoException
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.PagerDutyHelper.PagerDutyKeys.{FAILED_TO_CREATE_UPDATE_GATEWAY_DATA, FAILED_TO_ClEAR_GATEWAY_DATA, FAILED_TO_FIND_GATEWAY_DATA}
import utils.PagerDutyHelper.{PagerDutyKeys, pagerDutyLog}
import utils.SecureGCMCipher
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatewayUserDataRepositoryImpl()(mongo: MongoComponent, appConfig: AppConfig)
                                               (implicit ec: ExecutionContext, secureGCMCipher: SecureGCMCipher)
  extends PlayMongoRepository[UserDataGateway](
    mongoComponent = mongo,
    collectionName = "gatewayUserData",
    domainFormat = UserDataGateway.formats,
    indexes = GatewayUserDataIndexes.indexes(appConfig)
  ) with Repository with GatewayUserDataRepository with Logging {

  override def createOrUpdate(userData: UserDataGateway): Future[Either[DatabaseError, Unit]] = {
    lazy val start = "[GatewayUserDataRepositoryImpl][update]"

        val queryFilter = filterGateway(userData.sessionId, userData.mtdItId, userData.nino)
        val replacement = userData
        val options = FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)

        collection.findOneAndReplace(queryFilter, replacement, options).toFutureOption().map {
          case Some(_) => Right(())
          case None =>
            pagerDutyLog(FAILED_TO_CREATE_UPDATE_GATEWAY_DATA, s"$start Failed to update user gateway data.")
            Left(DataNotUpdatedError)
        }.recover {
          case exception: Exception =>
            pagerDutyLog(FAILED_TO_CREATE_UPDATE_GATEWAY_DATA, s"$start Failed to update user  gateway data. Exception: ${exception.getMessage}")
            Left(MongoError(exception.getMessage))
        }
  }

  override def find(user: User): Future[Either[DatabaseError, Option[UserDataGateway]]] = {

    lazy val start = "[GatewayUserDataRepositoryImpl][find]"

    val queryFilter = filterGateway(user.sessionId, user.mtditid, user.nino)
    val update = set("lastUpdated", toBson(DateTime.now(DateTimeZone.UTC))(MongoJodaFormats.dateTimeWrites))
    val options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)

    collection.findOneAndUpdate(queryFilter, update, options).toFutureOption().map {
      case Some(data) => Right(Some(data))
      case None =>
        logger.info(s"[GatewayUserDataRepositoryImpl][find] No employment gateway data found for user. SessionId: ${user.sessionId}")
        Right(None)
    }.recover {
      case exception: Exception =>
        pagerDutyLog(FAILED_TO_FIND_GATEWAY_DATA, s"$start Failed to find gateway user data. Exception: ${exception.getMessage}")
        Left(MongoError(exception.getMessage))
    }
  }

  override def clear(user: User): Future[Boolean] =
    collection.deleteOne(filterGateway(user.sessionId, user.mtditid, user.nino))
      .toFutureOption()
      .recover(mongoRecover("Clear", FAILED_TO_ClEAR_GATEWAY_DATA, user))
      .map(_.exists(_.wasAcknowledged()))

  def mongoRecover[T](operation: String,
                      pagerDutyKey: PagerDutyKeys.Value,
                      user: User): PartialFunction[Throwable, Option[T]] = new PartialFunction[Throwable, Option[T]] {

    override def isDefinedAt(x: Throwable): Boolean = x.isInstanceOf[MongoException]

    override def apply(e: Throwable): Option[T] = {
      pagerDutyLog(
        pagerDutyKey,
        s"[GatewayUserDataRepositoryImpl] Failed to [$operation] user gateway data. Error:${e.getMessage}. SessionId: ${user.sessionId}"
      )
      None
    }
  }
}

trait GatewayUserDataRepository {
  def createOrUpdate(userData: UserDataGateway): Future[Either[DatabaseError, Unit]]

  def find(user: User): Future[Either[DatabaseError, Option[UserDataGateway]]]

  def clear(user: User): Future[Boolean]
}
