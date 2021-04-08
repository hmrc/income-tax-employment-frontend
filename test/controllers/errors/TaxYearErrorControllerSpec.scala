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

import play.api.http.Status.OK
import play.api.test.Helpers.contentType
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import utils.UnitTestWithApp
import views.html.templates.TaxYearErrorTemplate

class TaxYearErrorControllerSpec extends UnitTestWithApp with DefaultAwaitTimeout {

  lazy val taxYearErrorTemplate: TaxYearErrorTemplate = app.injector.instanceOf[TaxYearErrorTemplate]
  lazy val controller = new TaxYearErrorController(mockMessagesControllerComponents, mockAppConfig, taxYearErrorTemplate)

  ".show()" should {

    "return an OK response .show() is called" in {

      val fakeRequest = FakeRequest("GET", "/error/wrong-tax-year")
      val result = controller.show()(fakeRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }

  }

}
