/*
 * Copyright 2021 HM Revenue & Customs
 *
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

    val uri = s"mongodb://${configuration.get[String]("incomeTaxUserDataMongo.uri")}"
    override def client: MongoClient = MongoClient(uri)
    override def database: MongoDatabase = client.getDatabase(new ConnectionString(uri).getDatabase)
  }
}
