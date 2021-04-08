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

package controllers.errors

import play.api.http.Status.UNAUTHORIZED
import play.api.test.Helpers.contentType
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import utils.UnitTestWithApp
import views.html.authErrorPages.YouNeedAgentServicesView

class YouNeedAgentServicesControllerSpec extends UnitTestWithApp with DefaultAwaitTimeout {

  lazy val controller = new YouNeedAgentServicesController(mockMessagesControllerComponents, app.injector.instanceOf[YouNeedAgentServicesView], mockAppConfig)

  "The show method" should {

    "return an OK response when .show() is called" in {

      val fakeRequest = FakeRequest("GET", "/error/you-need-agent-services-account")
      val result = controller.show()(fakeRequest)

      status(result) shouldBe UNAUTHORIZED
      contentType(result) shouldBe Some("text/html")
    }
  }
}
