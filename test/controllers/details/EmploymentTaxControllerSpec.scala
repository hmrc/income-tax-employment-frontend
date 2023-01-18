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

package controllers.details

import common.SessionValues
import controllers.employment.routes.CheckEmploymentDetailsController
import forms.AmountForm
import forms.details.EmploymentDetailsFormsProvider
import models.AuthorisationRequest
import models.details.EmploymentDetails
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status, stubMessagesControllerComponents}
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.mocks.{MockAuthorisedAction, MockEmploymentService, MockEmploymentSessionService, MockErrorHandler}
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.InYearUtil
import views.html.details.EmploymentTaxView

import scala.concurrent.Future

class EmploymentTaxControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockEmploymentService
  with MockErrorHandler {

  object Model {

    private val employmentSource1: EmploymentDetails = EmploymentDetails(
      "Mishima Zaibatsu",
      employerRef = Some("223/AB12399"),
      startDate = Some("2019-04-21"),
      currentDataIsHmrcHeld = true
    )
    val employmentCyaModel: EmploymentCYAModel = EmploymentCYAModel(employmentSource1)
    val employmentUserData: EmploymentUserData = EmploymentUserData(aUser.sessionId, aUser.mtditid, aUser.nino, taxYear, employmentId, isPriorSubmission = false,
      hasPriorBenefits = false, hasPriorStudentLoans = false, employmentCyaModel)
  }

  private val nino = "AA123456A"
  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, nino, "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
      fakeRequest)
  private val employmentId = "223/AB12399"

  private lazy val view = app.injector.instanceOf[EmploymentTaxView]
  implicit private val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest.withHeaders())

  private lazy val underTest = new EmploymentTaxController(
    stubMessagesControllerComponents(),
    mockAuthorisedAction,
    view,
    mockEmploymentSessionService,
    mockEmploymentService,
    new InYearUtil,
    new EmploymentDetailsFormsProvider(),
    mockErrorHandler,
  )(appConfig)

  ".show" should {
    "return a result when " which {
      s"has an OK($OK) status" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {
          mockGetAndHandle(taxYearEOY, Ok(view(
            taxYearEOY, "001", "Dave", AmountForm.amountForm(""))
          ))

          underTest.show(taxYearEOY, employmentId = employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))
        }

        status(result) shouldBe OK
      }
    }
  }

  ".submit" should {
    "return a result when " which {
      s"Has a $SEE_OTHER status when cya in session" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          val redirect = CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url

          (mockEmploymentSessionService.getSessionDataAndReturnResult(_: Int, _: String)(_: String)(
            _: EmploymentUserData => Future[Result])(_: AuthorisationRequest[_])).expects(taxYearEOY, employmentId, redirect, *, *).returns(Future(Redirect(redirect)))

          underTest.submit(taxYearEOY, employmentId = employmentId)(fakeRequest.withFormUrlEncodedBody("amount" -> "32").withSession(
            SessionValues.TAX_YEAR -> taxYearEOY.toString
          ))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url)
      }
    }
  }
}
