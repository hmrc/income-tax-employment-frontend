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
    val payModel: PayModel = PayModel(34234.15, 6782.92, Some(67676), "CALENDAR MONTHLY", "2020-04-23", Some(32), Some(2))
    val employerModel: EmployerModel = EmployerModel(Some("223/AB12399"), "maggie")
    val employmentModel: EmploymentModel = EmploymentModel(Some("1002"), Some("123456789999"), Some(true), Some(false), Some("2020-02-12"),
      Some("2019-04-21"), Some("2020-03-11"), Some(false), Some(false), employerModel, payModel)
    val getEmploymentDataModel: GetEmploymentDataModel = GetEmploymentDataModel("2020-01-04T05:01:01Z", Some("CUSTOMER"),
      Some("2020-04-04T01:01:01Z"), Some("2020-04-04T01:01:01Z"), employmentModel)
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
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }
  }

}
