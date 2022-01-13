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

package controllers.employment

import builders.models.benefits.BenefitsBuilder.aBenefits
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import common.SessionValues
import config.{MockCheckYourBenefitsService, MockEmploymentSessionService}
import play.api.http.Status._
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import utils.{UnitTest, UnitTestWithApp}
import views.html.employment.{CheckYourBenefitsView, CheckYourBenefitsViewEOY}

import scala.concurrent.Future

class CheckYourBenefitsControllerSpec extends UnitTestWithApp
  with MockEmploymentSessionService
  with UnitTest
  with MockCheckYourBenefitsService {

  lazy val view: CheckYourBenefitsView = app.injector.instanceOf[CheckYourBenefitsView]
  lazy val viewEOY: CheckYourBenefitsViewEOY = app.injector.instanceOf[CheckYourBenefitsViewEOY]

  lazy val controller = new CheckYourBenefitsController(
    authorisedAction,
    mockMessagesControllerComponents,
    mockAppConfig,
    view,
    viewEOY,
    mockEmploymentSessionService,
    mockCheckYourBenefitsService,
    inYearAction,
    mockErrorHandler,
    testClock,
    ec
  )

  val taxYear: Int = mockAppConfig.defaultTaxYear
  val employmentId = "223/AB12399"


  ".show" should {

    "return a result when all data is in Session" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = {
          mockFind(taxYear, Ok(view(taxYear, aBenefitsViewModel, isSingleEmployment = true, employmentId)))
          controller.show(taxYear, employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }

    "return a result when all data is in Session for EOY" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = {
          mockFind(taxYear, Ok(viewEOY(taxYear, aBenefits.toBenefitsViewModel(isUsingCustomerData = true),
            employmentId = employmentId, isUsingCustomerData = true)))
          controller.show(taxYear, employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }

    "redirect the User to the Overview page no data in mongo" which {

      s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
        val result: Future[Result] = {
          mockFind(taxYear, Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller.show(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe "/overview"
      }
    }
  }

}
