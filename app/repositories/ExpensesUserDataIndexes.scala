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

import config.AppConfig
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.model.Indexes.{ascending, compoundIndex}

import java.util.concurrent.TimeUnit

private[repositories] object ExpensesUserDataIndexes {

  private val lookUpIndex: Bson = compoundIndex(
    ascending("sessionId"),
    ascending("mtdItId"),
    ascending("nino"),
    ascending("taxYear")
  )

  def indexes(appConfig: AppConfig): Seq[IndexModel] = {
    Seq(
      IndexModel(lookUpIndex, IndexOptions().unique(true).name("UserDataLookupIndex")),
      IndexModel(ascending("lastUpdated"), IndexOptions().expireAfter(appConfig.mongoTTL, TimeUnit.MINUTES).name("UserDataTTL"))
    )
  }
}
