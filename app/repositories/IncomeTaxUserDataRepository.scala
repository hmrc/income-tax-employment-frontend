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
import javax.inject.{Inject, Singleton}
import models.{User, UserData}
import org.joda.time.LocalDateTime
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.FindOneAndUpdateOptions
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeTaxUserDataRepository @Inject()(mongo: MongoComponent)(implicit ec: ExecutionContext
) extends PlayMongoRepository[UserData](
  mongoComponent = mongo,
  collectionName = "userData",
  domainFormat   = UserData.formats,
  indexes        = Seq(),
  replaceIndexes = false
){

  private def filter(sessionId: String, mtdItId: String, nino: String, taxYear: Int): Bson = and(
    equal("sessionId", toBson(sessionId)),
    equal("mtdItId", toBson(mtdItId)),
    equal("nino", toBson(nino)),
    equal("taxYear", toBson(taxYear))
  )

  def find[T](user: User[T], taxYear: Int): Future[Option[UserData]] = {
    collection.findOneAndUpdate(
      filter = filter(user.sessionId,user.mtditid,user.nino,taxYear),
      update = set("lastUpdated", toBson(LocalDateTime.now())(MongoJodaFormats.localDateTimeWrites)),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFutureOption()
  }
}
