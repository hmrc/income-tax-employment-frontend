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

import common.SessionValues
import controllers.employment.EmploymentDetailsController
import models.{EmployerModel, EmploymentModel, GetEmploymentDataModel, PayModel}
import play.api.http.Status._
import play.api.mvc.Result
import utils.UnitTestWithApp
import views.html.employment.EmploymentDetailsView


import scala.concurrent.Future

class EmploymentDetailsControllerSpec extends UnitTestWithApp {

  object FullModel {
    val payModel: PayModel = PayModel(111.4, 1000.00, Some(10000000), "Monthly", "14/83/2022", None, None)
    val employerModel: EmployerModel = EmployerModel(Some("#Lon"), "Londis LTD 2020 PLC Company")
    val employmentModel: EmploymentModel = EmploymentModel(None, None, Some(true), Some(false), Some("1990-07-14"), None, None, None, None, employerModel, payModel)
    val getEmploymentDataModel: GetEmploymentDataModel = GetEmploymentDataModel("Today", None, None, None, employmentModel)
  }

  lazy val controller = new EmploymentDetailsController()(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[EmploymentDetailsView],
    mockAppConfig
  )

  val taxYear = mockAppConfig.defaultTaxYear

  ".show" should {

    "return a result when GetEmploymentDataModel is in Session" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.EMPLOYMENT_DATA -> FullModel.getEmploymentDataModel.asJsonString))

        status(result) shouldBe OK
      }
    }

    "redirect the User to the Overview page no data in session" which {

      s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth{
        val result: Future[Result] = controller.show(taxYear)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe "/overview"
      }
    }
  }

}
