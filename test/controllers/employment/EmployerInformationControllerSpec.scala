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

import common.SessionValues
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.mocks.{MockAppConfig, MockEmploymentSessionService}
import utils.UnitTest
import views.html.employment.EmployerInformationView

import scala.concurrent.{ExecutionContext, Future}

class EmployerInformationControllerSpec extends UnitTest with MockEmploymentSessionService {

  private lazy val view = app.injector.instanceOf[EmployerInformationView]
  implicit private lazy val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit private val messages: Messages = getMessages(isWelsh = false)

  private def controller(isEmploymentEOYEnabled: Boolean = true) = new EmployerInformationController(
    authorisedAction,
    view,
    inYearAction,
    mockEmploymentSessionService,
  )(mockMessagesControllerComponents, appConfig = new MockAppConfig().config(isEmploymentEOYEnabled = isEmploymentEOYEnabled))

  private val employmentId: String = "223/AB12399"

  ".show" should {
    "render Employment And Benefits page when GetEmploymentDataModel is in mongo" which {
      s"has an OK($OK) status" in new TestWithAuth {
        val name: String = anEmploymentSource.employerName
        val employmentId: String = anEmploymentSource.employmentId
        val benefitsIsDefined: Boolean = anEmploymentSource.employmentBenefits.isDefined
        val studentLoansIsDefined: Boolean = anEmploymentSource.employmentData.flatMap(_.deductions).flatMap(_.studentLoans).isDefined

        val result: Future[Result] = {
          mockFind(taxYear, Ok(view(name, employmentId, benefitsIsDefined, studentLoansIsDefined, taxYear, isInYear = true, showNotification = false)))
          controller().show(taxYear, employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }

    "redirect the User to the Overview page when GetEmploymentDataModel is in mongo but " which {
      s"has an SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
        val result: Future[Result] = controller(isEmploymentEOYEnabled = false).show(taxYearEOY, anEmploymentSource.employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }

    "redirect the User to the Overview page no data in mongo" which {
      s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
        val result: Future[Result] = {
          mockFind(taxYear, Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller().show(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }
  }
}
