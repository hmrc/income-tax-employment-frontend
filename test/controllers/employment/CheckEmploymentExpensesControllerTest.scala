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

package controllers.employment

import common.SessionValues
import controllers.Assets._
import models.{ExpensesType, GetEmploymentExpensesModel}
import play.api.http.HeaderNames.LOCATION
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.header
import utils.UnitTestWithApp
import views.html.expenses.CheckEmploymentExpensesView

class CheckEmploymentExpensesControllerTest extends UnitTestWithApp with DefaultAwaitTimeout {

  private val view = app.injector.instanceOf[CheckEmploymentExpensesView]

  val controller = new CheckEmploymentExpensesController(authorisedAction, view, mockAppConfig, mockMessagesControllerComponents)

  val taxYear = 2022

  "calling show() as an individual" should {

    "return status code 303 with correct Location header" when {
      "there is no expenses data in session" in new TestWithAuth {
        val responseF = controller.show(taxYear)(fakeRequest)

        status(responseF) shouldBe SEE_OTHER
        header(LOCATION, responseF) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return status code 200 with correct content" when {
      "there is expenses data in session" in new TestWithAuth {
        val expensesType = Some(ExpensesType(Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)))

        val request =
          fakeRequest.withSession(
            SessionValues.EXPENSES_CYA -> Json.prettyPrint(
              Json.toJson(GetEmploymentExpensesModel(None, None, None, None, expensesType))
            )
          )

        val responseF = controller.show(taxYear)(request)

        status(responseF) shouldBe OK
      }
    }

  }

  "calling show() as an agent" should {

    "return status code 303 with correct Location header" when {
      "there is no expenses data in session" in new TestWithAuth(isAgent = true) {
        val responseF = controller.show(taxYear)(fakeRequestWithMtditidAndNino)

        status(responseF) shouldBe SEE_OTHER
        header(LOCATION, responseF) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return status code 200 with correct content" when {
      "there is expenses data in session" in new TestWithAuth(isAgent = true) {
        val expensesType = Some(ExpensesType(Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)))

        val request =
          fakeRequestWithMtditidAndNino.withSession(
            SessionValues.EXPENSES_CYA -> Json.prettyPrint(
              Json.toJson(GetEmploymentExpensesModel(None, None, None, None, expensesType))
            )
          )

        val responseF = controller.show(taxYear)(request)

        status(responseF) shouldBe OK
      }
    }

  }

}
