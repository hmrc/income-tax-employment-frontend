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
import controllers.employment.routes.CheckYourBenefitsController
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Results.{BadRequest, InternalServerError, Ok, Redirect}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, redirectLocation, status, stubMessagesControllerComponents}
import services.DefaultRedirectService
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.mocks._
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.InYearUtil
import views.html.benefits.fuel.CompanyCarBenefitsView

import scala.concurrent.Future

class CompanyCarBenefitsControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockFuelService
  with MockErrorHandler {

  private val nino = "AA123456A"
  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, nino, "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
      fakeRequest)
  private val employmentId = "223/AB12399"

  private lazy val view = app.injector.instanceOf[CompanyCarBenefitsView]
  private lazy val employmentUserData = new EmploymentUserData(
    aUser.sessionId,
    aUser.mtditid,
    aUser.nino,
    taxYearEOY,
    employmentId,
    true,
    hasPriorBenefits = true, hasPriorStudentLoans = true,
    EmploymentCYAModel(anEmploymentSource, isUsingCustomerData = false)
  )

  private lazy val employmentUserDataWithoutBenefits = new EmploymentUserData(
    aUser.sessionId,
    aUser.mtditid,
    aUser.nino,
    taxYearEOY,
    employmentId,
    isPriorSubmission = false,
    hasPriorBenefits = false,
    hasPriorStudentLoans = true,
    employment = EmploymentCYAModel(anEmploymentSource.copy(employmentBenefits = None), isUsingCustomerData = false)
  )

  private lazy val controller = new CompanyCarBenefitsController(
    mockAuthorisedAction,
    new InYearUtil,
    view,
    mockEmploymentSessionService,
    mockFuelService,
    new DefaultRedirectService(),
    mockErrorHandler,
    new FuelFormsProvider
  )(stubMessagesControllerComponents, new MockAppConfig().config())

  ".show" should {
    "get user session data and return the result from the given execution block" in {
      mockAuth(Some(nino))
      val anyResult: Results.Status = Ok
      val result: Future[Result] = {
        mockGetSessionDataResult(taxYearEOY, employmentId, anyResult)
        controller.show(taxYearEOY, employmentId)(fakeRequest)
      }

      status(result) shouldBe anyResult.header.status
    }
  }

  ".handleShow" should {
    "return Redirect result income tax submission overview" when {
      "when employment user data not present" in {
        implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
          new AuthorisationRequest[AnyContent](models.User("1234567890", None, "AA123456A", "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
            fakeRequest)
        await(controller.handleShow(taxYearEOY, employmentId, None)) shouldBe Redirect(CheckYourBenefitsController.show(taxYearEOY, employmentId))
      }
    }

    "redirect" when {
      "user has no benefits data" in {
        val result = controller.handleShow(taxYearEOY, employmentId, Some(employmentUserDataWithoutBenefits))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/company-benefits?employmentId=223%2FAB12399")
      }
    }

    "render page" when {
      "with non empty form when there are benefits" in {
        val result = controller.handleShow(taxYearEOY, employmentId, Some(employmentUserData))

        status(result) shouldBe OK
        contentAsString(result) should include("checked")
      }
    }
  }

  ".submit" should {
    "return a result" which {
      "has a SEE_OTHER status with valid form body true" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {
          mockGetSessionDataResult(taxYearEOY, employmentId, InternalServerError("500"))
          controller.submit(taxYearEOY, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "true"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "has a SEE_OTHER status with valid form body false" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {
          mockGetSessionDataResult(taxYearEOY, employmentId, InternalServerError("500"))
          controller.submit(taxYearEOY, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "false"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "has a SEE_OTHER status with valid form body true but no carVanFuel benefits in session" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {
          mockGetSessionDataResult(taxYearEOY, employmentId, Redirect("/any-url"))
          controller.submit(taxYearEOY, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "false"))
        }
        status(result) shouldBe SEE_OTHER
      }

      "has a SEE_OTHER status with valid form body but no session" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {
          mockGetSessionDataResult(taxYearEOY, employmentId, Redirect("/any-url"))

          controller.submit(taxYearEOY, employmentId)(fakeRequest.withFormUrlEncodedBody("value" -> "false"))
        }
        status(result) shouldBe SEE_OTHER
      }

      "has a BAD_REQUEST status with invalid form body" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {
          mockGetSessionDataResult(taxYearEOY, employmentId, BadRequest)
          controller.submit(taxYearEOY, employmentId)(fakeRequest)
        }
        status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
