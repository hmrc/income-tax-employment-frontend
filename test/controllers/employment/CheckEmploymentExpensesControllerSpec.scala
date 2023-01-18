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
import controllers.expenses.CheckEmploymentExpensesController
import models.AuthorisationRequest
import play.api.http.HeaderNames.LOCATION
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.Helpers.{header, status, stubMessagesControllerComponents}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import support.ControllerUnitTest
import support.builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import support.mocks._
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.InYearUtil
import views.html.expenses.CheckEmploymentExpensesView

import scala.concurrent.Future

class CheckEmploymentExpensesControllerSpec extends ControllerUnitTest
  with DefaultAwaitTimeout
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockCheckEmploymentExpensesService
  with MockAuditService
  with MockErrorHandler {

  private lazy val view: CheckEmploymentExpensesView = app.injector.instanceOf[CheckEmploymentExpensesView]
  private lazy val controller = new CheckEmploymentExpensesController(
    view,
    createOrAmendExpensesService,
    mockEmploymentSessionService,
    mockCheckEmploymentExpensesService,
    new InYearUtil,
    mockErrorHandler
  )(
    appConfig,
    mockAuthorisedAction,
    stubMessagesControllerComponents,
    ec
  )

  private val nino = "AA123456A"
  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionValues.CLIENT_MTDITID -> "1234567890",
    SessionValues.CLIENT_NINO -> nino,
    SessionValues.TAX_YEAR -> taxYear.toString,
    SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(",")
  ).withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, nino, "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
      fakeRequest)
  implicit private val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest.withHeaders())

  "calling show() as an individual" should {
    "return status code 303 with correct Location header" when {
      "there is no expenses data in the database" in {
        mockAuth(Some(nino))
        val responseF: Future[Result] = {
          mockFind(taxYear, Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller.show(taxYear)(fakeRequest)
        }

        status(responseF) shouldBe SEE_OTHER
        header(LOCATION, responseF) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return status code 200 with correct content" when {
      "there is expenses data in the database" in {
        mockAuth(Some(nino))

        val responseF: Future[Result] = {
          mockFind(taxYear, Ok(view(taxYear, anExpensesViewModel, isInYear = true)))
          controller.show(taxYear)(fakeRequest)
        }

        status(responseF) shouldBe OK
      }
    }
  }

  "calling show() as an agent" should {
    "return status code 303 with correct Location header" when {
      "there is no expenses data in the database" in {
        mockAuthAsAgent()
        val responseF: Future[Result] = {
          mockFind(taxYear, Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller.show(taxYear)(fakeRequest)
        }

        status(responseF) shouldBe SEE_OTHER
        header(LOCATION, responseF) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return status code 200 with correct content" when {
      "there is expenses data in the database" in {
        mockAuthAsAgent()
        val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
        val responseF: Future[Result] = {
          mockFind(taxYear, Ok(view(taxYear, anExpensesViewModel, isInYear = true)))
          controller.show(taxYear)(request)
        }

        status(responseF) shouldBe OK
      }
    }
  }
}
