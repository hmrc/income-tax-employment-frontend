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

import common.{EmploymentSection, SessionValues}
import controllers.employment.routes._
import models.employment._
import models.employment.createUpdate._
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{Request, Result}
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks._
import utils.UnitTest
import views.html.employment.CheckEmploymentDetailsView

import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentDetailsControllerSpec extends UnitTest
  with MockEmploymentSessionService
  with MockAuditService
  with MockCheckEmploymentDetailsService
  with MockNrsService
  with MockErrorHandler {

  private lazy val view = app.injector.instanceOf[CheckEmploymentDetailsView]
  implicit private lazy val ec: ExecutionContext = ExecutionContext.Implicits.global

  private def controller(mimic: Boolean = false, isEmploymentEOYEnabled: Boolean = true) = new CheckEmploymentDetailsController(
    view,
    inYearAction,
    mockEmploymentSessionService,
    mockCheckEmploymentDetailsService,
    mockErrorHandler
  )(mockMessagesControllerComponents, ec, new MockAppConfig().config(_mimicEmploymentAPICalls = mimic, isEmploymentEOYEnabled = isEmploymentEOYEnabled), authorisedAction)

  private val employmentId = "223AB12399"

  private val createUpdateEmploymentRequest: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
    None,
    Some(
      CreateUpdateEmployment(
        anEmploymentUserData.employment.employmentDetails.employerRef,
        anEmploymentUserData.employment.employmentDetails.employerName,
        anEmploymentUserData.employment.employmentDetails.startDate.get
      )
    ),
    Some(
      CreateUpdateEmploymentData(
        pay = CreateUpdatePay(
          anEmploymentUserData.employment.employmentDetails.taxablePayToDate.get,
          anEmploymentUserData.employment.employmentDetails.totalTaxToDate.get
        )
      )
    )
  )

  ".show" should {
    "return a result when GetEmploymentDataModel is in Session" which {
      s"has an OK($OK) status" in new TestWithAuth {
        implicit lazy val messages: Messages = defaultMessages
        val result: Future[Result] = {
          mockFind(taxYear, Ok(view(
            EmploymentDetailsViewModel(
              employerName = "Dave",
              employerRef = Some("reference"),
              payrollId = Some("12345678"),
              employmentId = "id",
              startDate = Some(s"${taxYearEOY - 1}-02-12"),
              didYouLeaveQuestion = Some(true),
              cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
              taxablePayToDate = Some(34234.15),
              totalTaxToDate = Some(6782.92),
              isUsingCustomerData = false
            ), taxYear, isInYear = true, showNotification = true
          )))
          controller().show(taxYear, employmentId = employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }

    "redirect the User to the Overview page no data in session" which {
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

  ".submit" should {
    "return to employment information" when {
      "nothing to update" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYearEOY, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_DETAILS, Left(NothingToUpdate))

          controller().submit(taxYearEOY, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe EmployerInformationController.show(taxYearEOY, employmentId).url
      }
    }
    "return to CYA show method" when {
      "the journey is not finished" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYearEOY, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_DETAILS, Left(JourneyNotFinished))

          controller().submit(taxYearEOY, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url
      }
    }
    "return an error page" when {
      "submission fails" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYearEOY, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_DETAILS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYearEOY, employmentId, createUpdateEmploymentRequest, Left(InternalServerError))

          controller().submit(taxYearEOY, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))
        }

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "continue to benefits section" when {
      "a new employment is created" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYearEOY, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_DETAILS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYearEOY, employmentId, createUpdateEmploymentRequest, Right((Some("id"), anEmploymentUserData.copy(hasPriorBenefits = false))))

          controller().submit(taxYearEOY, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe CheckYourBenefitsController.show(taxYearEOY, "id").url
      }
      "a new employment is created and mimic api calls is on" in new TestWithAuth {
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYearEOY, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_DETAILS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYearEOY, employmentId, createUpdateEmploymentRequest, Right((Some("id"), anEmploymentUserData.copy(hasPriorBenefits = false))))
          mockCreateOrUpdateSessionData(Redirect(CheckYourBenefitsController.show(taxYearEOY, "id").url))
          (mockErrorHandler.internalServerError()(_: Request[_])).expects(*).returns(InternalServerError)

          controller(mimic = true).submit(taxYearEOY, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe CheckYourBenefitsController.show(taxYearEOY, "id").url
      }
    }

    "redirect to Overview page when EOY and employmentEOYEnabled not enabled" in new TestWithAuth {
      val result: Future[Result] = controller(isEmploymentEOYEnabled = false).submit(taxYearEOY, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }
  }
}
