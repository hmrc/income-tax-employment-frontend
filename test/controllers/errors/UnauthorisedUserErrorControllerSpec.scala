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

package controllers.errors

import play.api.http.Status.UNAUTHORIZED
import play.api.test.Helpers.contentType
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import utils.{UnitTest, ViewTest}
import views.html.errors.UnauthorisedUserErrorPageView

class UnauthorisedUserErrorControllerSpec extends UnitTest with DefaultAwaitTimeout with ViewTest {

  private val pageView: UnauthorisedUserErrorPageView = app.injector.instanceOf[UnauthorisedUserErrorPageView]

  lazy val underTest = new UnauthorisedUserErrorController(pageView)(mockMessagesControllerComponents, mockAppConfig)

  "The show method" should {
    "return an OK response when .show() is called" in {
      val fakeRequest = FakeRequest("GET", "/error/not-authorised-to-use-service")
      val result = underTest.show()(fakeRequest)

      status(result) shouldBe UNAUTHORIZED
      contentType(result) shouldBe Some("text/html")
    }
  }
}
