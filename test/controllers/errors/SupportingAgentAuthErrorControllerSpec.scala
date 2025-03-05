/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.ControllerUnitTest
import views.html.errors.SupportingAgentAuthErrorView

class SupportingAgentAuthErrorControllerSpec extends ControllerUnitTest {

  private val pageView: SupportingAgentAuthErrorView = app.injector.instanceOf[SupportingAgentAuthErrorView]

  lazy val underTest = new SupportingAgentAuthErrorController(stubMessagesControllerComponents(), appConfig, pageView)

  "The show method" should {
    "return an UNAUTHORIZED response when .show() is called" in {
      val fakeRequest = FakeRequest(GET, routes.SupportingAgentAuthErrorController.show.url)
      val result = underTest.show()(fakeRequest)

      status(result) shouldBe UNAUTHORIZED
      contentAsString(result) shouldBe pageView()(fakeRequest, stubMessages(), appConfig).toString
    }
  }
}
