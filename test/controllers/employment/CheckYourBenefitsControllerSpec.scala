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
import controllers.expenses.routes._
import controllers.studentLoans.routes._
import models.AuthorisationRequest
import models.benefits.Benefits
import models.employment.OptionalCyaAndPrior
import models.employment.createUpdate._
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status, stubMessagesControllerComponents}
import services.DefaultRedirectService
import support.ControllerUnitTest
import support.builders.models.benefits.BenefitsBuilder.aBenefits
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks._
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.InYearUtil
import views.html.employment.CheckYourBenefitsView

import scala.concurrent.Future

class CheckYourBenefitsControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService
  with MockCheckYourBenefitsService
  with MockAuditService
  with MockErrorHandler {

  private lazy val view: CheckYourBenefitsView = app.injector.instanceOf[CheckYourBenefitsView]

  private def controller(mimic: Boolean = false, slEnabled: Boolean = true) = new CheckYourBenefitsController(
    view,
    mockEmploymentSessionService,
    mockCheckYourBenefitsService,
    mockAuditService,
    new DefaultRedirectService(),
    new InYearUtil(),
    mockErrorHandler
  )(stubMessagesControllerComponents, ec, new MockAppConfig().config(_mimicEmploymentAPICalls = mimic, slEnabled = slEnabled), mockAuthorisedAction)

  private val nino = "AA123456A"
  private val employmentId = "223AB12399"
  private val employerName: String = "Mishima Zaibatsu"

  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, nino, "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
      fakeRequest)
  implicit private val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest.withHeaders())

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
        )
      )
    )
  )

  ".show" should {
    "return a result when all data is in Session" which {
      s"has an OK($OK) status" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {
          mockFind(taxYear, Ok(view(taxYear, employmentId, employerName, aBenefitsViewModel, isUsingCustomerData = false, isInYear = true, showNotification = true)))
          controller().show(taxYear, employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }

    "return a result when all data is in Session for EOY" which {
      s"has an OK($OK) status" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {
          mockFind(taxYear, Ok(view(taxYear, employmentId, employerName, aBenefits.toBenefitsViewModel(isUsingCustomerData = true),
            isUsingCustomerData = true, isInYear = false, showNotification = false)))
          controller().show(taxYear, employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
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
        redirectLocation(result) shouldBe Some("/overview")
      }
    }
  }


  ".submit" should {
    "return to employment information" when {
      "nothing to update" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_BENEFITS, Left(NothingToUpdate))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(EmployerInformationController.show(taxYear, employmentId).url)
      }
    }
    "continue to student loans" when {
      "nothing to update" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_BENEFITS, Left(NothingToUpdate))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(StudentLoansCYAController.show(taxYear, employmentId).url)
      }
    }
    "continue to expenses" when {
      "nothing to update and student loans is off" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_BENEFITS, Left(NothingToUpdate))

          controller(slEnabled = false).submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(CheckEmploymentExpensesController.show(taxYear).url)
      }
    }

    "continue to student loans section" when {
      "benefits are added" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_BENEFITS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((None, anEmploymentUserData.copy(hasPriorBenefits = false))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(StudentLoansCYAController.show(taxYear, employmentId).url)
      }
      "benefits are added and when mimicking the apis" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_BENEFITS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((None, anEmploymentUserData.copy(hasPriorBenefits = false))))
          mockCreateOrUpdateSessionData(Redirect(StudentLoansCYAController.show(taxYear, employmentId).url))
          (mockErrorHandler.internalServerError()(_: Request[_])).expects(*).returns(InternalServerError)

          controller(mimic = true).submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(StudentLoansCYAController.show(taxYear, employmentId).url)
      }
    }

    "return to employer information" when {
      "benefits are added to existing hmrc employment" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_BENEFITS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((Some("id"), anEmploymentUserData.copy(hasPriorBenefits = false))))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(EmployerInformationController.show(taxYear, "id").url)
      }
      "benefits are added to existing customer employment" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_BENEFITS, Right(createUpdateEmploymentRequest))
          mockSubmitAndClear(taxYear, employmentId, createUpdateEmploymentRequest, Right((None, anEmploymentUserData.copy(hasPriorBenefits = false))))

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

          mockGetOptionalCYAAndPriorForEndOfYear(taxYear, Right(OptionalCyaAndPrior(Some(anEmploymentUserData.copy(hasPriorBenefits = false)), Some(anAllEmploymentData))))
          mockCreateModelOrReturnError(EmploymentSection.EMPLOYMENT_BENEFITS, Left(JourneyNotFinished))

          controller().submit(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(CheckYourBenefitsController.show(taxYear, employmentId).url)
      }
    }
  }
}
