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
import config.MockEmploymentSessionService
import play.api.http.Status._
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import utils.UnitTestWithApp
import views.html.employment.EmploymentDetailsAndBenefitsView

import scala.concurrent.Future

class EmploymentDetailsAndBenefitsControllerSpec extends UnitTestWithApp with MockEmploymentSessionService {

  lazy val view = app.injector.instanceOf[EmploymentDetailsAndBenefitsView]

  lazy val controller = new EmploymentDetailsAndBenefitsController()(
    mockMessagesControllerComponents,
    authorisedAction,
    view,
    inYearAction,
    mockAppConfig,
    mockEmploymentSessionService,
    ec
  )

  val taxYear:Int = mockAppConfig.defaultTaxYear
  val employmentId:String = "223/AB12399"

  ".show" should {

    "render Employment And Benefits page when GetEmploymentDataModel is in mongo" which {

      s"has an OK($OK) status" in new TestWithAuth {

        val name: String = employmentsModel.hmrcEmploymentData.head.employerName
        val employmentId: String = employmentsModel.hmrcEmploymentData.head.employmentId
        val benefitsIsDefined: Boolean = employmentsModel.hmrcEmploymentData.head.employmentBenefits.isDefined

        val result: Future[Result] = {
          mockFind(taxYear,Ok(view(name, employmentId, benefitsIsDefined, taxYear, isInYear = true,doExpensesExist=true,isSingleEmployment = true)))
          controller.show(taxYear, employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }

    "redirect the User to the Overview page no data in mongo" which {

      s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth{
        val result: Future[Result] = {
          mockFind(taxYear, Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller.show(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }
  }

}
