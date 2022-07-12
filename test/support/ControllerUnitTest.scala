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

package support

import config.AppConfig
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.stubMessagesControllerComponents
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Injecting}
import support.mocks.MockAppConfig

import scala.concurrent.ExecutionContext

trait ControllerUnitTest extends UnitTest
  with FutureAwaits with DefaultAwaitTimeout
  with GuiceOneAppPerSuite
  with Injecting
  with TaxYearProvider
  with FakeRequestHelper {

  protected implicit val cc: MessagesControllerComponents = stubMessagesControllerComponents()
  protected implicit val appConfig: AppConfig = new MockAppConfig().config()
  protected implicit lazy val ec: ExecutionContext = ExecutionContext.Implicits.global
}
