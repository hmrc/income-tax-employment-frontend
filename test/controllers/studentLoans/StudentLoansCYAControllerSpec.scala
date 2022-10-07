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

package controllers.studentLoans

import common.{EmploymentSection, SessionValues}
import controllers.employment.routes._
import controllers.expenses.routes._
import controllers.studentLoans.routes._
import models.benefits.Benefits
import models.employment.createUpdate._
import models.employment.{Deductions, OptionalCyaAndPrior, StudentLoans}
import play.api.http.Status._
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status, stubMessagesControllerComponents}
import support.ControllerUnitTest
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks._
import utils.InYearUtil
import views.html.studentLoans.StudentLoansCYAView

import scala.concurrent.Future

class StudentLoansCYAControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockStudentLoansCYAService
  with MockAuditService
  with MockErrorHandler {

  private lazy val view: StudentLoansCYAView = app.injector.instanceOf[StudentLoansCYAView]

  private def controller(mimic: Boolean = false, slEnabled: Boolean = true, isEmploymentEOYEnabled: Boolean = true, taxYearErrorFeature: Boolean = true) = new StudentLoansCYAController(
    stubMessagesControllerComponents,
    view,
    mockStudentLoansCYAService,
    mockEmploymentSessionService,
    mockAuthorisedAction,
    new InYearUtil,
    mockErrorHandler)(appConfig = new MockAppConfig().config(_mimicEmploymentAPICalls = mimic, slEnabled = slEnabled,
    isEmploymentEOYEnabled = isEmploymentEOYEnabled, taxYearErrorEnabled = taxYearErrorFeature), ec)

  private val nino = "AA123456A"
  private val employmentId = "223AB12399"
  val employerName: String = "Mishima Zaibatsu"

  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")

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
        ),
        benefitsInKind = Some(
          Benefits(
            Some(100.00),
            Some(100.00),
            Some(100.00),
            Some(100.00),
            Some(100.00),
            Some(100.00),
            Some(100.00),
            Some(100.00),
            Some(100.00),
            Some(100.00)
          )
        ),
        deductions = Some(
          Deductions(
            Some(StudentLoans(
              Some(344.55),
              Some(344.55)
            ))
          )
        )
      )
    )
  )

  ".show" should {
    "redirect to the overview page" when {
      "employmentEOYEnabled feature switch is off" in {
        mockAuth(Some(nino))
        val result: Future[Result] = controller(isEmploymentEOYEnabled = false, taxYearErrorFeature = false).show(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
      }

      "studentLoansEnabled feature switch is off" in {
        mockAuth(Some(nino))
        val result: Future[Result] = controller(slEnabled = false, taxYearErrorFeature = false).show(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
      }
    }
  }

  ".submit" should {
    "return to employment information" when {
      "nothing to update" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Left(NothingToUpdate))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(EmployerInformationController.show(taxYear, employmentId).url)
      }
    }
    "continue to expenses" when {
      "nothing to update" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Left(NothingToUpdate))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(CheckEmploymentExpensesController.show(taxYear).url)
      }
      "student loans are added" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockGetPriorRight(taxYear, Some(anAllEmploymentData))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((None, anEmploymentUserData.copy(hasPriorStudentLoans = false))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(ExpensesInterruptPageController.show(taxYear).url)
      }
      "student loans are added and when mimicking the apis" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockGetPriorRight(taxYear, None)
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((None, anEmploymentUserData.copy(hasPriorStudentLoans = false))))
          mockCreateOrUpdateSessionData(Redirect(StudentLoansCYAController.show(taxYear, employmentId).url))

          controller(mimic = true).submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(StudentLoansCYAController.show(taxYear, employmentId).url)
      }
    }

    "return to employer information" when {
      "student loans are added to existing hmrc employment" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((Some("id"), anEmploymentUserData.copy(hasPriorStudentLoans = false))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(EmployerInformationController.show(taxYear, "id").url)
      }
      "student loans are added to existing customer employment" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((None, anEmploymentUserData.copy(hasPriorStudentLoans = false))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(EmployerInformationController.show(taxYear, employmentId).url)
      }
    }

    "return to CYA show method" when {
      "the journey is not finished" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Left(JourneyNotFinished))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(StudentLoansCYAController.show(taxYear, employmentId).url)
      }
      "there is no cya data" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(None, Some(anAllEmploymentData))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(StudentLoansCYAController.show(taxYear, employmentId).url)
      }
    }
    "return an error" when {
      "data can't be retrieved" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Left(InternalServerError))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "prior data can't be retrieved" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((None, anEmploymentUserData.copy(hasPriorStudentLoans = false))))
          mockGetPriorLeft(taxYear)
          mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to the overview page" when {
      "employmentEOYEnabled feature switch is off" in {
        mockAuth(Some(nino))
        val result: Future[Result] = controller(isEmploymentEOYEnabled = false, taxYearErrorFeature = false).submit(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
      }

      "studentLoansEnabled feature switch is off" in {
        mockAuth(Some(nino))
        val result: Future[Result] = controller(slEnabled = false, taxYearErrorFeature = false).submit(taxYearEOY,
          employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
      }
    }
  }
}
