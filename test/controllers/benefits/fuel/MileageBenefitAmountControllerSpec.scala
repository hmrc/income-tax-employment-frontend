/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.benefits.fuel.FuelFormsProvider
import play.api.data.Form
import play.api.http.Status._
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, stubMessagesControllerComponents}
import services.DefaultRedirectService
import support.mocks._
import support.{ControllerUnitTest, TaxYearProvider}
import utils.InYearUtil
import views.html.benefits.fuel.MileageBenefitAmountView

import scala.concurrent.Future

class MileageBenefitAmountControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockFuelService
  with MockAuditService
  with TaxYearProvider
  with MockErrorHandler {

  private lazy val view = app.injector.instanceOf[MileageBenefitAmountView]
  private lazy val controller = new MileageBenefitAmountController(
    mockAuthorisedAction,
    view,
    new InYearUtil,
    mockEmploymentSessionService,
    mockFuelService,
    new DefaultRedirectService(),
    mockErrorHandler,
    new FuelFormsProvider
  )(stubMessagesControllerComponents(), appConfig, ec)
  private val nino = "AA123456A"
  val employmentId = "12345"
  val form: Form[BigDecimal] = AmountForm.amountForm("benefits.mileageBenefitAmount.error.empty.individual")
  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")

  ".show" should {

    "return a result" which {

      s"has an OK($OK) status" in {
        mockAuth(Some(nino))
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

      s"has an SEE_OTHER($SEE_OTHER) status when there is no employment" in {
        mockAuth(Some(nino))
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
