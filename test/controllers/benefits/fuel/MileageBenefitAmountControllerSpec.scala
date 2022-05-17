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

package controllers.benefits.fuel

import common.SessionValues
import forms.AmountForm
import play.api.data.Form
import play.api.http.Status._
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{Result, Results}
import support.mocks.{MockAuditService, MockEmploymentSessionService, MockErrorHandler, MockFuelService}
import utils.{TaxYearHelper, UnitTest}
import views.html.benefits.fuel.MileageBenefitAmountView

import scala.concurrent.{ExecutionContext, Future}

class MileageBenefitAmountControllerSpec extends UnitTest
  with MockEmploymentSessionService
  with MockFuelService
  with MockAuditService
  with TaxYearHelper
  with MockErrorHandler {

  private implicit lazy val ec: ExecutionContext = ExecutionContext.Implicits.global
  private lazy val view = app.injector.instanceOf[MileageBenefitAmountView]
  private lazy val controller = new MileageBenefitAmountController(
    authorisedAction,
    view,
    inYearAction,
    mockEmploymentSessionService,
    mockFuelService,
    mockErrorHandler
  )(mockMessagesControllerComponents, mockAppConfig, ec)
  val employmentId = "12345"
  val form: Form[BigDecimal] = AmountForm.amountForm("benefits.mileageBenefitAmount.error.empty.individual")

  ".show" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val anyResult: Results.Status = Ok
        val result: Future[Result] = {
          mockGetAndHandle(taxYearEOY, anyResult)

          controller.show(taxYearEOY, employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
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
          mockGetSessionDataAndReturnResult(taxYearEOY, employmentId, anyResult)

          controller.submit(taxYearEOY, employmentId)(fakeRequest.withFormUrlEncodedBody("amount" -> "23423").withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))
        }

        status(result) shouldBe SEE_OTHER
      }
    }
  }

}
