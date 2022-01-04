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

package controllers

import play.api.http.Status.{NO_CONTENT, OK}
import play.api.test.Helpers.{charset, contentType}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import utils.UnitTestWithApp
import views.html.templates.TimeoutPage

class SessionExpiredControllerSpec extends UnitTestWithApp with DefaultAwaitTimeout {

  lazy val controller = new SessionExpiredController(mockMessagesControllerComponents, mockAppConfig, app.injector.instanceOf[TimeoutPage])

  ".KeepAlive" should {
    "return no Content" in {
      val request = FakeRequest("GET", "/timeout")
      val result = controller.keepAlive()(request)

      status(result) shouldBe NO_CONTENT
      }
    }

  ".timeout" should {
    "return OK with HTML" in {
      val request = FakeRequest("GET", "/keep-alive")
      val result = controller.timeout()(request)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "timeout() is called with a tax year key it" should {

      val request = FakeRequest("GET", "/sign-out")

      val responseF = controller.timeout()(request.withSession("TAX_YEAR" -> "2022"))

      "return status code OK" in {
        status(responseF) shouldBe OK
      }

      "return HTML" in {
        contentType(responseF) shouldBe Some("text/html")
        charset(responseF) shouldBe Some("utf-8")
      }
    }

  }
}
