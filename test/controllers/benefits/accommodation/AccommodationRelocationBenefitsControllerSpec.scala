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

package controllers.benefits.accommodation

import common.SessionValues
import controllers.errors.routes.UnauthorisedUserErrorController
import forms.benefits.accommodation.AccommodationFormsProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results.{BadRequest, InternalServerError, Redirect}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, contentType, status}
import support.builders.models.UserBuilder.aUser
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks.{MockAccommodationService, MockAuthorisedAction, MockEmploymentSessionService, MockErrorHandler}
import support.{ControllerUnitTest, ViewHelper}
import utils.InYearUtil
import views.html.benefits.accommodation.AccommodationRelocationBenefitsView

class AccommodationRelocationBenefitsControllerSpec extends ControllerUnitTest with ViewHelper
  with MockAuthorisedAction
  with MockAccommodationService
  with MockEmploymentSessionService
  with MockErrorHandler {

  private val employmentId = "employment-id"
  private val agentUser = aUser.copy(arn = Some("0987654321"), affinityGroup = "Agent")

  private val yesRadioButtonCssSelector = ".govuk-radios__item > input#value"
  private val noRadioButtonCssSelector = ".govuk-radios__item > input#value-no"

  private val fakeIndividualRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withHeaders("X-Session-ID" -> aUser.sessionId)

  private val fakeAgentRequest: FakeRequest[AnyContentAsEmpty.type] = fakeIndividualRequest
    .withHeaders("X-Session-ID" -> aUser.sessionId)
    .withSession(SessionValues.CLIENT_MTDITID -> "1234567890", SessionValues.CLIENT_NINO -> "AA123456A")

  private val pageView = inject[AccommodationRelocationBenefitsView]
  private val formsProvider = new AccommodationFormsProvider()

  private lazy val underTest = new AccommodationRelocationBenefitsController(
    mockAuthorisedAction,
    inYearAction = new InYearUtil(),
    pageView: AccommodationRelocationBenefitsView,
    mockAccommodationService,
    mockEmploymentSessionService,
    mockErrorHandler,
    formsProvider)

  ".show" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      await(underTest.show(taxYear = taxYearEOY, employmentId = employmentId)(fakeIndividualRequest)) shouldBe
        Redirect(UnauthorisedUserErrorController.show)
    }

    "redirect to Overview page when in year" in {
      mockAuthAsIndividual(Some("AA123456A"))
      mockFindEmploymentUserData(taxYear, employmentId, aUser, Right(Some(anEmploymentUserData)))

      await(underTest.show(taxYear = taxYear, employmentId = employmentId)(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return internal server error when find employment returns Left" in {
      mockAuthAsIndividual(Some("AA123456A"))
      mockFindEmploymentUserData(taxYearEOY, employmentId, aUser, Left())
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      await(underTest.show(taxYearEOY, employmentId).apply(fakeIndividualRequest)) shouldBe InternalServerError
    }

    "return result for individual" which {
      "has empty form when accommodation model is None" in {
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = None)
        val employmentUserDataWithEmptyAccommodation = anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits =
          Some(benefitsViewModel)))

        mockAuthAsIndividual(Some("AA123456A"))
        mockFindEmploymentUserData(taxYearEOY, employmentId, aUser, Right(Some(employmentUserDataWithEmptyAccommodation)))

        val result = underTest.show(taxYearEOY, employmentId).apply(fakeIndividualRequest)

        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/html")

        implicit val document: Document = Jsoup.parse(contentAsString(result))

        document.select(yesRadioButtonCssSelector).hasAttr("checked") shouldBe false
        document.select(noRadioButtonCssSelector).hasAttr("checked") shouldBe false
      }

      "has yes selected when accommodation section question is true" in {
        mockAuthAsIndividual(Some("AA123456A"))
        mockFindEmploymentUserData(taxYearEOY, employmentId, aUser, Right(Some(anEmploymentUserData)))

        val result = underTest.show(taxYearEOY, employmentId).apply(fakeIndividualRequest)

        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/html")

        implicit val document: Document = Jsoup.parse(contentAsString(result))

        document.select(yesRadioButtonCssSelector).hasAttr("checked") shouldBe true
        document.select(noRadioButtonCssSelector).hasAttr("checked") shouldBe false
      }
    }

    "return result for Agent" which {
      "has empty form when accommodation model is None" in {
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = None)
        val employmentUserDataWithEmptyAccommodation = anEmploymentUserData
          .copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel)))

        mockAuthAsAgent()
        mockFindEmploymentUserData(taxYearEOY, employmentId, agentUser, Right(Some(employmentUserDataWithEmptyAccommodation)))

        val result = underTest.show(taxYearEOY, employmentId).apply(fakeAgentRequest)

        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/html")

        implicit val document: Document = Jsoup.parse(contentAsString(result))

        document.select(yesRadioButtonCssSelector).hasAttr("checked") shouldBe false
        document.select(noRadioButtonCssSelector).hasAttr("checked") shouldBe false
      }

      "has yes selected when accommodation section question is true" in {
        mockAuthAsAgent()
        mockFindEmploymentUserData(taxYearEOY, employmentId, agentUser, Right(Some(anEmploymentUserData)))

        val result = underTest.show(taxYearEOY, employmentId).apply(fakeAgentRequest)

        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/html")

        implicit val document: Document = Jsoup.parse(contentAsString(result))

        document.select(yesRadioButtonCssSelector).hasAttr("checked") shouldBe true
        document.select(noRadioButtonCssSelector).hasAttr("checked") shouldBe false
      }
    }
  }

  ".submit" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      await(underTest.submit(taxYear = taxYearEOY, employmentId = employmentId)(fakeIndividualRequest.withFormUrlEncodedBody("value" -> "true"))) shouldBe
        Redirect(UnauthorisedUserErrorController.show)
    }

    "redirect to Overview page when in year" in {
      mockAuthAsIndividual(Some("AA123456A"))
      mockGetSessionDataResult(taxYear, employmentId, Redirect("/any-url"))

      val request = fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString).withFormUrlEncodedBody("value" -> "true")

      await(underTest.submit(taxYear = taxYear, employmentId = employmentId)(request)) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return BadRequest when getSessionData returns BadRequest" in {
      val request = fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("value" -> "true")

      mockAuthAsIndividual(Some("AA123456A"))
      mockGetSessionDataResult(taxYearEOY, employmentId, BadRequest)

      await(underTest.submit(taxYearEOY, employmentId).apply(request)) shouldBe BadRequest
    }
  }
}

