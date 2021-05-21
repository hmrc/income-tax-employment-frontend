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

package config

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import javax.inject.{Inject, Provider}
import org.mongodb.scala.{ConnectionString, MongoClient, MongoDatabase}
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent

class IncomeTaxUserDataMongoModule extends AbstractModule {
  override def configure(): Unit =
    bind(classOf[MongoComponent])
      .annotatedWith(Names.named("incomeTaxUserData"))
      .toProvider(classOf[IncomeTaxUserDatabaseProvider])
}

private class IncomeTaxUserDatabaseProvider @Inject()(configuration: Configuration) extends Provider[MongoComponent] {

  override def get(): MongoComponent = new MongoComponent {

    lazy val uri = s"mongodb://${configuration.get[String]("incomeTaxUserDataMongo.uri")}"
    override def client: MongoClient = MongoClient(uri)
    override def database: MongoDatabase = client.getDatabase(new ConnectionString(uri).getDatabase)
  }
}
