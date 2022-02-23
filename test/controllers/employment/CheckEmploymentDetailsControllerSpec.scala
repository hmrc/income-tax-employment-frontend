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
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{Request, Result}
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks._
import utils.UnitTestWithApp
import views.html.employment.CheckEmploymentDetailsView

import scala.concurrent.Future

class CheckEmploymentDetailsControllerSpec extends UnitTestWithApp
  with MockEmploymentSessionService
  with MockAuditService
  with MockCheckEmploymentDetailsService
  with MockNrsService {

  private lazy val view = app.injector.instanceOf[CheckEmploymentDetailsView]

  private def controller(mimic: Boolean = false) = new CheckEmploymentDetailsController()(
    mockMessagesControllerComponents,
    view,
    authorisedAction,
    inYearAction,
    new MockAppConfig().config(_mimicEmploymentAPICalls = mimic),
    mockEmploymentSessionService,
    mockCheckEmploymentDetailsService,
    ec,
    mockErrorHandler
  )

  private val taxYear = mockAppConfig.defaultTaxYear
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
        val result: Future[Result] = {
          mockFind(taxYear, Ok(view(
            EmploymentDetailsViewModel(
              employerName = "Dave",
              employerRef = Some("reference"),
              payrollId = Some("12345678"),
              employmentId = "id",
              startDate = Some("2020-02-12"),
              cessationDateQuestion = Some(true),
              cessationDate = Some("2020-02-12"),
              taxablePayToDate = Some(34234.15),
              totalTaxToDate = Some(6782.92),
              isUsingCustomerData = false
            ), taxYear, isInYear = true, isSingleEmployment = true
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

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_DETAILS, Left(NothingToUpdate))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe EmployerInformationController.show(taxYear, employmentId).url
      }
    }
    "return to CYA show method" when {
      "the journey is not finished" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_DETAILS, Left(JourneyNotFinished))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe CheckEmploymentDetailsController.show(taxYear, employmentId).url
      }
    }
    "return an error page" when {
      "submission fails" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_DETAILS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Left(InternalServerError))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "continue to benefits section" when {
      "a new employment is created" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_DETAILS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((Some("id"), anEmploymentUserData.copy(hasPriorBenefits = false))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe CheckYourBenefitsController.show(taxYear, "id").url
      }
      "a new employment is created and mimic api calls is on" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_DETAILS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((Some("id"), anEmploymentUserData.copy(hasPriorBenefits = false))))
          mockCreateOrUpdateSessionData(Redirect(CheckYourBenefitsController.show(taxYear, "id").url))
          (mockErrorHandler.internalServerError()(_: Request[_])).expects(*).returns(InternalServerError)

          controller(true).submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe CheckYourBenefitsController.show(taxYear, "id").url
      }
    }
  }
}
