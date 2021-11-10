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

package controllers.benefits;

import common.SessionValues
import config.{MockAuditService, MockEmploymentSessionService}
import controllers.benefits.fuel.MileageBenefitAmountController
import forms.AmountForm
import play.api.data.Form
import play.api.http.Status._
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{Result, Results}
import utils.UnitTestWithApp
import views.html.benefits.MileageBenefitAmountView

import scala.concurrent.Future

class MileageBenefitAmountControllerSpec extends UnitTestWithApp with MockEmploymentSessionService with MockAuditService{

  lazy val view = app.injector.instanceOf[MileageBenefitAmountView]
  lazy val controller  = new MileageBenefitAmountController()(
    mockMessagesControllerComponents,
    authorisedAction,
    view,
    inYearAction,
    mockAppConfig,
    mockEmploymentSessionService,
    mockErrorHandler,
    testClock
  )
  val taxYear = 2021
  val employmentId = "12345"
  val form: Form[BigDecimal] =  AmountForm.amountForm("benefits.mileageBenefitAmount.error.empty.individual")

  ".show" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth {

        val anyResult: Results.Status = Ok

        val result: Future[Result] = {

          mockGetAndHandle(taxYear, anyResult)

          controller.show(taxYear,employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }
  }

  ".submit" should {

    "return a result" which {

      s"has an SEE_OTHER($SEE_OTHER) status when there is no employment" in new TestWithAuth {
        val result: Future[Result] = {

          val anyResult = Redirect("redirect")
          mockGetSessionDataAndReturnResult(taxYear, employmentId, anyResult)

          controller.submit(taxYear,employmentId)(fakeRequest.withFormUrlEncodedBody("amount" -> "23423").withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe SEE_OTHER
      }
    }
  }

}
