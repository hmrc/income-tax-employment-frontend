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

package controllers.employment

import common.SessionValues
import models.AuthorisationRequest
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status, stubMessagesControllerComponents}
import support.ControllerUnitTest
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.mocks.{MockAppConfig, MockAuthorisedAction, MockEmploymentSessionService}
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.InYearUtil
import views.html.employment.EmployerInformationView

import scala.concurrent.Future

class EmployerInformationControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService {

  private lazy val view = app.injector.instanceOf[EmployerInformationView]

  private def controller(isEmploymentEOYEnabled: Boolean = true) = new EmployerInformationController(
    mockAuthorisedAction,
    view,
    new InYearUtil(),
    mockEmploymentSessionService,
  )(stubMessagesControllerComponents(), appConfig = new MockAppConfig().config(isEmploymentEOYEnabled = isEmploymentEOYEnabled))

  private val nino = "AA123456A"
  private val employmentId: String = "223/AB12399"

  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, nino, "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
      fakeRequest)
  implicit private val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest.withHeaders())

  ".show" should {
    "render Employment And Benefits page when GetEmploymentDataModel is in mongo" which {
      s"has an OK($OK) status" in {
        mockAuth(Some(nino))
        val employerName: String = anEmploymentSource.employerName
        val employmentId: String = anEmploymentSource.employmentId

        val result: Future[Result] = {
          mockFind(taxYear, Ok(view(employerName, employmentId, Seq(), taxYear, isInYear = true, showNotification = false)))
          controller().show(taxYear, employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }

    "redirect the User to the Overview page when GetEmploymentDataModel is in mongo but " which {
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        mockAuth(Some(nino))
        val result: Future[Result] = controller(isEmploymentEOYEnabled = false)
          .show(taxYearEOY, anEmploymentSource.employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect the User to the Overview page no data in mongo" which {
      s"has the SEE_OTHER($SEE_OTHER) status" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {
          mockFind(taxYear, Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller().show(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }
}
