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
import config.MockStudentLoansCYAService
import controllers.employment.routes._
import controllers.expenses.routes._
import controllers.studentLoans.routes._
import models.benefits.Benefits
import models.employment.createUpdate._
import models.employment.{Deductions, OptionalCyaAndPrior, StudentLoans}
import play.api.http.Status._
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, Redirect}
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks.{MockAppConfig, MockAuditService, MockEmploymentSessionService}
import utils.{TaxYearHelper, UnitTestWithApp}
import views.html.studentLoans.StudentLoansCYAView

import scala.concurrent.Future

class StudentLoansCYAControllerSpec extends UnitTestWithApp
  with MockEmploymentSessionService
  with MockStudentLoansCYAService
  with MockAuditService {

  private lazy val view: StudentLoansCYAView = app.injector.instanceOf[StudentLoansCYAView]

  private def controller(mimic: Boolean = false, slEnabled: Boolean = true) = new StudentLoansCYAController(
    mockMessagesControllerComponents,
    view,
    mockStudentLoansCYAService,
    mockEmploymentSessionService,
    authorisedAction,
    inYearAction,
    new MockAppConfig().config(_mimicEmploymentAPICalls = mimic, slEnabled = slEnabled),
    ec
  )

  private val employmentId = "223AB12399"
  val employerName: String = "Mishima Zaibatsu"

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

  ".submit" should {
    "return to employment information" when {
      "nothing to update" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Left(NothingToUpdate))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe EmployerInformationController.show(taxYear, employmentId).url
      }
    }
    "continue to expenses" when {
      "nothing to update" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Left(NothingToUpdate))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }
      "student loans are added" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((None, anEmploymentUserData.copy(hasPriorStudentLoans = false))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe CheckEmploymentExpensesController.show(taxYear).url
      }
      "student loans are added and when mimicking the apis" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((None, anEmploymentUserData.copy(hasPriorStudentLoans = false))))
          mockCreateOrUpdateSessionData(Redirect(StudentLoansCYAController.show(taxYear, employmentId).url))

          controller(mimic = true).submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe StudentLoansCYAController.show(taxYear, employmentId).url
      }
    }

    "return to employer information" when {
      "student loans are added to existing hmrc employment" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((Some("id"), anEmploymentUserData.copy(hasPriorStudentLoans = false))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe EmployerInformationController.show(taxYear, "id").url
      }
      "student loans are added to existing customer employment" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((None, anEmploymentUserData.copy(hasPriorStudentLoans = false))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe EmployerInformationController.show(taxYear, employmentId).url
      }
    }

    "return to CYA show method" when {
      "the journey is not finished" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorStudentLoans = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.STUDENT_LOANS, Left(JourneyNotFinished))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe StudentLoansCYAController.show(taxYear, employmentId).url
      }
      "there is no cya data" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(None, Some(anAllEmploymentData))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe StudentLoansCYAController.show(taxYear, employmentId).url
      }
    }
    "return an error" when {
      "data can't be retrieved" in new TestWithAuth {

        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Left(InternalServerError))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
