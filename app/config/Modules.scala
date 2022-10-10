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

package config

import com.google.inject.AbstractModule
import common.UUID
import repositories.{EmploymentUserDataRepository, EmploymentUserDataRepositoryImpl, ExpensesUserDataRepository, ExpensesUserDataRepositoryImpl, GatewayUserDataRepository, GatewayUserDataRepositoryImpl}
import services.{DefaultRedirectService, RedirectService}
import utils.Clock

class Modules extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AppConfig]).asEagerSingleton()
    bind(classOf[UUID]).toInstance(UUID)
    bind(classOf[Clock]).toInstance(Clock)
    bind(classOf[RedirectService]).to(classOf[DefaultRedirectService]).asEagerSingleton()
    bind(classOf[GatewayUserDataRepository]).to(classOf[GatewayUserDataRepositoryImpl]).asEagerSingleton()
    bind(classOf[EmploymentUserDataRepository]).to(classOf[EmploymentUserDataRepositoryImpl]).asEagerSingleton()
    bind(classOf[ExpensesUserDataRepository]).to(classOf[ExpensesUserDataRepositoryImpl]).asEagerSingleton()
  }
}
