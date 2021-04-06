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

package controllers

import config.AppConfig
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UnitTest

class LanguageSwitchControllerSpec extends UnitTest with GuiceOneAppPerSuite {

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val mockFrontendAppConfig = new AppConfig(serviceConfig)

  private val controller = new LanguageSwitchController(appConfig = mockFrontendAppConfig, controllerComponents = stubMessagesControllerComponents(),
    messagesApi = stubMessagesApi())

  "calling the SwitchToLanguage method" when {
    "return a redirect with the referer url" in {
      val result = controller.switchToLanguage("en")(fakeRequest.withHeaders("Referer" -> "/referrer-url"))
      status(result) shouldBe Status.SEE_OTHER
    }
    "return a redirect to the fallback url with a default taxYear" in {
      val result = controller.switchToLanguage("en")(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
    "return a redirect to the fallback url with a specific taxYear" in {
      val result = controller.switchToLanguage("en")(fakeRequest.withSession("TAX_YEAR" -> "2010"))
      status(result) shouldBe Status.SEE_OTHER
    }
  }
}
