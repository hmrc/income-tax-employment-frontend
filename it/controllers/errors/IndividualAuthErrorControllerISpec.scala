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

import config.AppConfig
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.UNAUTHORIZED
import utils.IntegrationTest
import views.html.authErrorPages.IndividualUnauthorisedView

class IndividualAuthErrorControllerISpec extends IntegrationTest {


  lazy val controller = new IndividualAuthErrorController(
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[AppConfig],
    app.injector.instanceOf[IndividualUnauthorisedView]
  )

  ".show" should {

    "return an Unauthorized status" in {
      val result: Result = await(controller.show()(FakeRequest()))
      result.header.status shouldBe UNAUTHORIZED
    }

  }

}
