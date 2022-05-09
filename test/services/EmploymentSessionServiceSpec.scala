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

package services

import common.EmploymentSection
import config._
import models.benefits.{AssetsModel, Benefits, BenefitsViewModel}
import models.employment._
import models.employment.createUpdate._
import models.expenses.Expenses
import models.mongo._
import models.{APIErrorBodyModel, APIErrorModel, AuthorisationRequest, User}
import play.api.Logging
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import support.mocks._
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.{InYearUtil, UnitTest}
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import scala.concurrent.Future

class EmploymentSessionServiceSpec extends UnitTest
  with MockIncomeTaxUserDataConnector
  with MockEmploymentUserDataRepository
  with MockCreateUpdateEmploymentDataConnector
  with MockIncomeSourceConnector
  with MockExpensesUserDataRepository
  with Logging {

  private val user: User = authorisationRequest.user

  private val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  private val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  private val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  private val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]
  private val mockInYearUtil: InYearUtil = app.injector.instanceOf[InYearUtil]

  private val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  private val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  private val underTest: EmploymentSessionService = new EmploymentSessionService(
    mockEmploymentUserDataRepository,
    mockExpensesUserDataRepository,
    mockUserDataConnector,
    mockIncomeSourceConnector,
    mockAppConfig,
    messages,
    errorHandler,
    mockCreateUpdateEmploymentDataConnector,
    testClock,
    mockInYearUtil,
    mockExecutionContext
  )

  private val underTestWithMimicking: EmploymentSessionService = new EmploymentSessionService(
    mockEmploymentUserDataRepository,
    mockExpensesUserDataRepository,
    mockUserDataConnector,
    mockIncomeSourceConnector,
    new MockAppConfig().config(_mimicEmploymentAPICalls = true),
    messages,
    errorHandler,
    mockCreateUpdateEmploymentDataConnector,
    testClock,
    mockInYearUtil,
    mockExecutionContext
  )

  private val anyResult = Ok

  private def result(allEmploymentData: AllEmploymentData): Result = {
    allEmploymentData.hmrcExpenses match {
      case Some(_) => Ok("Ok")
      case None => Redirect("303")
    }
  }

  private def allEmploymentData: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      HmrcEmploymentSource(
        employmentId = "001",
        employerName = "maggie",
        employerRef = None,
        payrollId = None,
        startDate = None,
        cessationDate = None,
        dateIgnored = None,
        submittedOn = None,
        hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
          employmentData = Some(EmploymentData(
            submittedOn = s"${taxYearEOY-1}-02-12",
            employmentSequenceNumber = None,
            companyDirector = None,
            closeCompany = None,
            directorshipCeasedDate = None,
            disguisedRemuneration = None,
            pay = Some(Pay(Some(34234.15), Some(6782.92), None, None, None, None)),
            Some(Deductions(
              studentLoans = Some(StudentLoans(
                uglDeductionAmount = Some(100.00),
                pglDeductionAmount = Some(100.00)
              ))
            ))
          )),
          None
        )),
        None
      )
    ),
    hmrcExpenses = Some(
      EmploymentExpenses(
        Some(s"${taxYearEOY-1}-01-04T05:01:01Z"), None, Some(800.00), Some(
          Expenses(
            Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00)
          )
        )
      )
    ),
    customerEmploymentData = Seq(),
    customerExpenses = Some(
      EmploymentExpenses(
        Some(s"${taxYearEOY-1}-01-04T05:01:01Z"), None, Some(800.00), Some(
          Expenses(
            Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00)
          )
        )
      )
    )
  )

  private val employmentCYA: EmploymentCYAModel = {
    EmploymentCYAModel(
      employmentDetails = EmploymentDetails(
        "Employer Name",
        employerRef = Some(
          "123/12345"
        ),
        startDate = Some(s"${taxYearEOY-1}-11-11"),
        taxablePayToDate = Some(55.99),
        totalTaxToDate = Some(3453453.00),
        employmentSubmittedOn = Some(s"${taxYearEOY-1}-04-04T01:01:01Z"),
        employmentDetailsSubmittedOn = Some(s"${taxYearEOY-1}-04-04T01:01:01Z"),
        currentDataIsHmrcHeld = false
      ),
      employmentBenefits = Some(
        BenefitsViewModel(
          assetsModel = Some(AssetsModel(Some(true), Some(true), Some(100.00), Some(true), Some(100.00))),
          submittedOn = Some(s"${taxYearEOY-1}-02-04T05:01:01Z"), isUsingCustomerData = true
        )
      ),
      studentLoans = Some(
        StudentLoansCYAModel(
          uglDeduction = true,
          Some(20000.00),
          pglDeduction = true,
          Some(30000.00)
        )
      )
    )
  }

  private val employmentDataFull: EmploymentUserData = {
    EmploymentUserData(sessionId, "1234567890", nino, taxYear, "employmentId", isPriorSubmission = true,
      hasPriorBenefits = true,
      hasPriorStudentLoans = true, employmentCYA, testClock.now())
  }

  private val createUpdateEmploymentRequest: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
    None,
    Some(
      CreateUpdateEmployment(
        employmentCYA.employmentDetails.employerRef,
        employmentCYA.employmentDetails.employerName,
        employmentCYA.employmentDetails.startDate.get
      )
    ),
    Some(
      CreateUpdateEmploymentData(
        pay = CreateUpdatePay(
          employmentCYA.employmentDetails.taxablePayToDate.get,
          employmentCYA.employmentDetails.totalTaxToDate.get
        )
      )
    )
  )

  "submitAndClear" should {
    "submit data and then clear the database" in {

      val requestWithoutEmploymentId = createUpdateEmploymentRequest.copy(employmentId = None)
      mockCreateUpdateEmploymentData(nino, taxYear, requestWithoutEmploymentId)(Right(None))

      mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
      mockClear(taxYear, "employmentId", response = true)

      val response = underTest.submitAndClear(taxYear, "employmentId", requestWithoutEmploymentId, anEmploymentUserData, Some(anAllEmploymentData))

      await(response) shouldBe Right((None, anEmploymentUserData))
    }
    "submit data and then clear the database and perform audits" in {

      def auditAndNRS(employmentId: String, taxYear: Int, model: CreateUpdateEmploymentRequest, prior: Option[AllEmploymentData], request: AuthorisationRequest[_]): Unit = {
        logger.info("Performing fake audits")
      }

      val requestWithoutEmploymentId = createUpdateEmploymentRequest.copy(employmentId = None)
      mockCreateUpdateEmploymentData(nino, taxYear, requestWithoutEmploymentId)(Right(None))

      mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
      mockClear(taxYear, "employmentId", response = true)

      val response = underTest.submitAndClear(taxYear, "employmentId", requestWithoutEmploymentId, anEmploymentUserData, Some(anAllEmploymentData), Some(
        auditAndNRS
      ))

      await(response) shouldBe Right((None, anEmploymentUserData))
    }
    "return an error from the create call" in {

      val requestWithoutEmploymentId = createUpdateEmploymentRequest.copy(employmentId = None)
      mockCreateUpdateEmploymentData(nino, taxYear, requestWithoutEmploymentId)(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      val response = underTest.submitAndClear(taxYear, "employmentId", requestWithoutEmploymentId, anEmploymentUserData, Some(anAllEmploymentData))

      status(response.map(_.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
    "return an error from the clear call" in {

      val requestWithoutEmploymentId = createUpdateEmploymentRequest.copy(employmentId = None)
      mockCreateUpdateEmploymentData(nino, taxYear, requestWithoutEmploymentId)(Right(None))

      mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
      mockClear(taxYear, "employmentId", response = false)

      val response = underTest.submitAndClear(taxYear, "employmentId", requestWithoutEmploymentId, anEmploymentUserData, Some(anAllEmploymentData))

      status(response.map(_.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
    "submit data and then clear the database with an employment id" in {

      val requestWithoutEmploymentId = createUpdateEmploymentRequest.copy(employmentId = None)
      mockCreateUpdateEmploymentData(nino, taxYear, requestWithoutEmploymentId)(Right(Some("id")))

      mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
      mockClear(taxYear, "employmentId", response = true)

      val response = underTest.submitAndClear(taxYear, "employmentId", requestWithoutEmploymentId, anEmploymentUserData, Some(anAllEmploymentData))

      await(response) shouldBe Right((Some("id"), anEmploymentUserData))
    }
  }

  "getCYAAndPriorForEndOfYear" should {
    "return a redirect when in year" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindNoContent(nino, taxYear)
      val response = underTest.getCYAAndPriorForEndOfYear(taxYear, "employmentId")

      status(response.map(_.left.get)) shouldBe SEE_OTHER
    }

    "return cya data and prior" in {
      mockFind(taxYear - 1, "employmentId", Right(Some(anEmploymentUserData)))
      mockFind(nino, taxYear - 1, anIncomeTaxUserData)
      val response = underTest.getCYAAndPriorForEndOfYear(taxYear - 1, "employmentId")

      await(response) shouldBe Right(CyaAndPrior(anEmploymentUserData, Some(anAllEmploymentData)))
    }
  }

  "getOptionalCYAAndPriorForEndOfYear" should {
    "return a redirect when in year" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindNoContent(nino, taxYear)
      val response = underTest.getOptionalCYAAndPriorForEndOfYear(taxYear, "employmentId")

      status(response.map(_.left.get)) shouldBe SEE_OTHER
    }
    "return optional data" in {
      mockFind(taxYear - 1, "employmentId", Right(None))
      mockFindNoContent(nino, taxYear - 1)
      val response = underTest.getOptionalCYAAndPriorForEndOfYear(taxYear - 1, "employmentId")

      await(response) shouldBe Right(OptionalCyaAndPrior(None, None))
    }
  }

  "getOptionalCYAAndPrior" should {
    "return optional data" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindNoContent(nino, taxYear)
      val response = underTest.getOptionalCYAAndPrior(taxYear, "employmentId")

      await(response) shouldBe Right(OptionalCyaAndPrior(None, None))
    }

    "return cya data and prior" in {
      mockFind(taxYear, "employmentId", Right(Some(anEmploymentUserData)))
      mockFind(nino, taxYear, anIncomeTaxUserData)
      val response = underTest.getOptionalCYAAndPrior(taxYear, "employmentId")

      await(response) shouldBe Right(OptionalCyaAndPrior(Some(anEmploymentUserData), Some(anAllEmploymentData)))
    }

    "return a left with a redirect" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindNoContent(nino, taxYear)
      val response = underTest.getOptionalCYAAndPrior(taxYear, "employmentId", redirectWhenNoPrior = true)

      status(response.map(_.left.get)) shouldBe SEE_OTHER
    }

    "return an error if the call failed" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindFail(nino, taxYear)

      val response = underTest.getOptionalCYAAndPrior(taxYear, "employmentId")

      status(response.map(_.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }

    "return an internal server error if the CYA find failed" in {
      mockFind(taxYear, "employmentId", Left(DataNotFound))
      mockFindNoContent(nino, taxYear)

      val response = underTest.getOptionalCYAAndPrior(taxYear, "employmentId")

      status(response.map(_.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "getCYAAndPrior" should {
    "return a redirect if no data" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindNoContent(nino, taxYear)
      val response = underTest.getCYAAndPrior(taxYear, "employmentId")

      status(response.map(_.left.get)) shouldBe SEE_OTHER
    }

    "return cya data and prior" in {
      mockFind(taxYear, "employmentId", Right(Some(anEmploymentUserData)))
      mockFind(nino, taxYear, anIncomeTaxUserData)
      val response = underTest.getCYAAndPrior(taxYear, "employmentId")

      await(response) shouldBe Right(CyaAndPrior(anEmploymentUserData, Some(anAllEmploymentData)))
    }

    "return an error if the call failed" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindFail(nino, taxYear)

      val response = underTest.getCYAAndPrior(taxYear, "employmentId")

      status(response.map(_.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }

    "return an internal server error if the CYA find failed" in {
      mockFind(taxYear, "employmentId", Left(DataNotFound))
      mockFindNoContent(nino, taxYear)

      val response = underTest.getCYAAndPrior(taxYear, "employmentId")

      status(response.map(_.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "getAndHandle" should {
    "redirect if no data and redirect is set to true" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindNoContent(nino, taxYear)
      val response = underTest.getAndHandle(taxYear, "employmentId", redirectWhenNoPrior = true)((_, _) => Future(Ok))

      status(response) shouldBe SEE_OTHER
    }

    "return an error if the call failed" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindFail(nino, taxYear)

      val response = underTest.getAndHandle(taxYear, "employmentId")((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "return an internal server error if the CYA find failed" in {
      mockFind(taxYear, "employmentId", Left(DataNotFound))
      mockFindNoContent(nino, taxYear)

      val response = underTest.getAndHandle(taxYear, "employmentId")((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "getAndHandleExpenses" should {
    "return an error if the call failed" in {
      mockFindFail(nino, taxYear)
      mockFind(taxYear, user, Right(None))

      val response = underTest.getAndHandleExpenses(taxYear)((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "return an internal server error if the CYA find failed" in {
      mockFind(taxYear, user, Left(DataNotFound))
      mockFindNoContent(nino, taxYear)

      val response = underTest.getAndHandleExpenses(taxYear)((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "findPreviousEmploymentUserData" should {
    "return an error if the call failed" in {
      mockFindFail(nino, taxYear)

      val response = underTest.findPreviousEmploymentUserData(
        User(mtditid = mtditid, arn = None, nino = nino, sessionId = sessionId, AffinityGroup.Individual.toString),
        taxYear)(_ => Ok)

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "createOrUpdateEmploymentResult" should {
    "use the request model to make the api call and return the response" when {
      "request has no employmentId" in {
        val requestWithoutEmploymentId = createUpdateEmploymentRequest.copy(employmentId = None)
        mockCreateUpdateEmploymentData(nino, taxYear, requestWithoutEmploymentId)(Right(None))

        val response = underTest.createOrUpdateEmploymentResult(taxYear, requestWithoutEmploymentId)

        await(response) shouldBe Right(None)
      }

      "request has employmentId" in {
        val requestWithEmploymentId = createUpdateEmploymentRequest.copy(employmentId = Some("some-employment-id"))
        mockCreateUpdateEmploymentData(nino, taxYear, requestWithEmploymentId)(Right(Some("some-employment-id")))

        val response = underTest.createOrUpdateEmploymentResult(taxYear, requestWithEmploymentId)

        await(response) shouldBe Right(Some("some-employment-id"))
      }
    }

    "use the request model to make the api call  and handle an error" in {
      mockCreateUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest)(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      val response = underTest.createOrUpdateEmploymentResult(taxYear, createUpdateEmploymentRequest)

      status(response.map(_.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "createModelOrReturnError" should {
    "return JourneyNotFinished redirect from an exception when an update is being made to student loans but no prior" in {
      lazy val response = underTest.createModelOrReturnError(authorisationRequest.user, employmentDataFull, None, EmploymentSection.STUDENT_LOANS)

      response.left.get shouldBe JourneyNotFinished
    }

    "return JourneyNotFinished redirect from an exception when an update is being made to student loans but no pay info in the prior employment" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,
        employmentDataFull, Some(
          AllEmploymentData(
            Seq(
              HmrcEmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                None,
                None
              )
            ), None, Seq(), None
          )
        ), EmploymentSection.STUDENT_LOANS
      )

      response.left.get shouldBe JourneyNotFinished
    }

    "return redirect when an update is being made to student loans and a prior hmrc employment" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,
        employmentDataFull,
        Some(AllEmploymentData(Seq(HmrcEmploymentSource(
          employmentDataFull.employmentId,
          employmentDataFull.employment.employmentDetails.employerName,
          employmentDataFull.employment.employmentDetails.employerRef,
          employmentDataFull.employment.employmentDetails.payrollId,
          employmentDataFull.employment.employmentDetails.startDate,
          employmentDataFull.employment.employmentDetails.cessationDate,
          employmentDataFull.employment.employmentDetails.dateIgnored,
          employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
          hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
            Some(EmploymentData(
              employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
              Some(Pay(
                employmentDataFull.employment.employmentDetails.taxablePayToDate,
                employmentDataFull.employment.employmentDetails.totalTaxToDate,
                None, None, None, None
              )), None
            )),
            None
          )),None
          )), None, Seq(), None)
        ), EmploymentSection.STUDENT_LOANS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), Some(Deductions(Some(StudentLoans(Some(20000.0), Some(30000.0))))), None)), None, Some(true)
      )
    }

    "return model with employment id when mimic api calls in on and section is after details section" in {
      lazy val response = underTestWithMimicking.createModelOrReturnError(
        authorisationRequest.user,
        employmentDataFull,
        Some(AllEmploymentData(Seq(HmrcEmploymentSource(
          employmentDataFull.employmentId,
          employmentDataFull.employment.employmentDetails.employerName,
          employmentDataFull.employment.employmentDetails.employerRef,
          employmentDataFull.employment.employmentDetails.payrollId,
          employmentDataFull.employment.employmentDetails.startDate,
          employmentDataFull.employment.employmentDetails.cessationDate,
          employmentDataFull.employment.employmentDetails.dateIgnored,
          employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
          hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
            Some(EmploymentData(
              employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
              Some(Pay(
                employmentDataFull.employment.employmentDetails.taxablePayToDate,
                employmentDataFull.employment.employmentDetails.totalTaxToDate,
                None, None, None, None
              )), None
            )),
            None
          )), None
          )), None, Seq(), None)
        ), EmploymentSection.STUDENT_LOANS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), Some(Deductions(Some(StudentLoans(Some(20000.0), Some(30000.0))))), Some(
          Benefits(assets = Some(100.00), assetTransfer = Some(100.00))
        ))), None, isHmrcEmploymentId = Some(true)
      )
    }

    "return redirect when an update is being made to student loans and a prior hmrc employment that has benefits" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,
        employmentDataFull, Some(
          AllEmploymentData(
            Seq(
              HmrcEmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
                  Some(EmploymentData(
                    employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                    Some(Pay(
                      employmentDataFull.employment.employmentDetails.taxablePayToDate,
                      employmentDataFull.employment.employmentDetails.totalTaxToDate,
                      None, None, None, None
                    )), None
                  )),
                  Some(
                    EmploymentBenefits(
                      "",
                      employmentDataFull.employment.employmentBenefits.map(_.toBenefits)
                    )
                  )
                )),
                None
              )
            ), None, Seq(), None
          )
        ), EmploymentSection.STUDENT_LOANS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), Some(Deductions(Some(StudentLoans(Some(20000.0), Some(30000.0))))),
          Some(Benefits(None, Some(100.0), Some(100.0), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)))), None, isHmrcEmploymentId = Some(true)
      )
    }

    "return redirect when an update is being made to student loans and a prior customer employment" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(), None,
            Seq(
              EmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                Some(EmploymentData(
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                  Some(Pay(
                    employmentDataFull.employment.employmentDetails.taxablePayToDate,
                    employmentDataFull.employment.employmentDetails.totalTaxToDate,
                    None, None, None, None
                  )), None
                )),
                None
              )
            ), None
          )
        ), EmploymentSection.STUDENT_LOANS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), Some(Deductions(Some(StudentLoans(Some(20000.0), Some(30000.0))))), None)), None
      )
    }
    "return redirect when an update is being made to student loans and a prior customer employment which has benefits" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(), None,
            Seq(
              EmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                Some(EmploymentData(
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                  Some(Pay(
                    employmentDataFull.employment.employmentDetails.taxablePayToDate,
                    employmentDataFull.employment.employmentDetails.totalTaxToDate,
                    None, None, None, None
                  )), None
                )),
                Some(
                  EmploymentBenefits(
                    "",
                    employmentDataFull.employment.employmentBenefits.map(_.toBenefits)
                  )
                )
              )
            ), None
          )
        ), EmploymentSection.STUDENT_LOANS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), Some(Deductions(Some(StudentLoans(Some(20000.0), Some(30000.0))))),
          Some(Benefits(None, Some(100.0), Some(100.0), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)))), None
      )
    }
    "return redirect when an update is being made to remove student loans and has a prior customer employment which has benefits" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull.copy(
          employment = employmentDataFull.employment.copy(
            studentLoans = None
          )
        ), Some(
          AllEmploymentData(
            Seq(), None,
            Seq(
              EmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                Some(EmploymentData(
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                  Some(Pay(
                    employmentDataFull.employment.employmentDetails.taxablePayToDate,
                    employmentDataFull.employment.employmentDetails.totalTaxToDate,
                    None, None, None, None
                  )), Some(
                    Deductions(
                      Some(StudentLoans(
                        Some(5466.77), Some(32545.55)
                      ))
                    )
                  )
                )),
                Some(
                  EmploymentBenefits(
                    "",
                    employmentDataFull.employment.employmentBenefits.map(_.toBenefits)
                  )
                )
              )
            ), None
          )
        ), EmploymentSection.STUDENT_LOANS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), None,
          Some(Benefits(None, Some(100.0), Some(100.0), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)))), None
      )
    }

    "return JourneyNotFinished redirect when an update is being made to student loans and a prior customer employment does not have pay details" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(), None,
            Seq(
              EmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                Some(EmploymentData(
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                  Some(Pay(
                    None, None, None, None, None, None
                  )), None
                )),
                None
              )
            ), None
          )
        ), EmploymentSection.STUDENT_LOANS
      )

      response.left.get shouldBe JourneyNotFinished
    }

    "return JourneyNotFinished redirect from an exception when an update is being made to benefits but no prior" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user, employmentDataFull, None, EmploymentSection.EMPLOYMENT_BENEFITS
      )

      response.left.get shouldBe JourneyNotFinished
    }

    "return JourneyNotFinished redirect from an exception when an update is being made to benefits but no pay info in the prior employment" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(
              HmrcEmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                None,
                None
              )
            ), None, Seq(), None
          )
        ), EmploymentSection.EMPLOYMENT_BENEFITS
      )

      response.left.get shouldBe JourneyNotFinished
    }

    "return redirect when an update is being made to benefits and a prior hmrc employment" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(
              HmrcEmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
                  Some(EmploymentData(
                    employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                    Some(Pay(
                      employmentDataFull.employment.employmentDetails.taxablePayToDate,
                      employmentDataFull.employment.employmentDetails.totalTaxToDate,
                      None, None, None, None
                    )), None
                  )),
                  None
                )), None
              )
            ), None, Seq(), None
          )
        ), EmploymentSection.EMPLOYMENT_BENEFITS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), None,
          Some(Benefits(None, Some(100.0), Some(100.0), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)))), None, isHmrcEmploymentId = Some(true)
      )
    }

    "return redirect when an update is being made to benefits and a prior customer employment" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(), None,
            Seq(
              EmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                Some(EmploymentData(
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                  Some(Pay(
                    employmentDataFull.employment.employmentDetails.taxablePayToDate,
                    employmentDataFull.employment.employmentDetails.totalTaxToDate,
                    None, None, None, None
                  )), None
                )),
                None
              )
            ), None
          )
        ), EmploymentSection.EMPLOYMENT_BENEFITS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), None,
          Some(Benefits(None, Some(100.0), Some(100.0), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)))), None
      )
    }
    "return redirect when an update is being made to benefits and a prior customer employment which has student loans" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(), None,
            Seq(
              EmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                Some(EmploymentData(
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                  Some(Pay(
                    employmentDataFull.employment.employmentDetails.taxablePayToDate,
                    employmentDataFull.employment.employmentDetails.totalTaxToDate,
                    None, None, None, None
                  )), Some(
                    Deductions(
                      Some(StudentLoans(
                        Some(5466.77), Some(32545.55)
                      ))
                    )
                  )
                )),
                None
              )
            ), None
          )
        ), EmploymentSection.EMPLOYMENT_BENEFITS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), Some(Deductions(Some(StudentLoans(Some(5466.77), Some(32545.55))))),
          Some(Benefits(None, Some(100.0), Some(100.0), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)))), None
      )
    }
    "return redirect when an update is being made to remove benefits and has a prior customer employment which has student loans" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull.copy(
          employment = employmentDataFull.employment.copy(
            employmentBenefits = Some(
              BenefitsViewModel(
                isUsingCustomerData = true, isBenefitsReceived = true
              )
            )
          )
        ), Some(
          AllEmploymentData(
            Seq(), None,
            Seq(
              EmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                Some(EmploymentData(
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                  Some(Pay(
                    employmentDataFull.employment.employmentDetails.taxablePayToDate,
                    employmentDataFull.employment.employmentDetails.totalTaxToDate,
                    None, None, None, None
                  )), Some(
                    Deductions(
                      Some(StudentLoans(
                        Some(5466.77), Some(32545.55)
                      ))
                    )
                  )
                )),
                None
              )
            ), None
          )
        ), EmploymentSection.EMPLOYMENT_BENEFITS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), Some(Deductions(Some(StudentLoans(Some(5466.77), Some(32545.55))))), None)), None
      )
    }

    "return JourneyNotFinished redirect when an update is being made to benefits and a prior customer employment does not have pay details" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(), None,
            Seq(
              EmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                Some(EmploymentData(
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                  Some(Pay(
                    None, None, None, None, None, None
                  )), None
                )),
                None
              )
            ), None
          )
        ), EmploymentSection.EMPLOYMENT_BENEFITS
      )

      response.left.get shouldBe JourneyNotFinished
    }

    "return to employment details when nothing to update" in {


      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(
              HmrcEmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
                  Some(EmploymentData(
                    employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                    Some(Pay(
                      employmentDataFull.employment.employmentDetails.taxablePayToDate,
                      employmentDataFull.employment.employmentDetails.totalTaxToDate,
                      None, None, None, None
                    )), None
                  )),
                  employmentBenefits = Some(EmploymentBenefits(employmentDataFull.employment.employmentBenefits.get.submittedOn.get,
                    Some(employmentDataFull.employment.employmentBenefits.get.toBenefits)))
                )), None
              )
            ), None, Seq(), None
          )
        ), EmploymentSection.EMPLOYMENT_DETAILS
      )

      response.left.get shouldBe NothingToUpdate
    }

    "create the model to send and return the correct result" in {

      val response = underTest.createModelOrReturnError(
        authorisationRequest.user, employmentDataFull, Some(allEmploymentData), EmploymentSection.EMPLOYMENT_DETAILS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        None, Some(CreateUpdateEmployment(Some("123/12345"), "Employer Name", s"${taxYearEOY-1}-11-11", None, None)), Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), None, None)), None
      )
    }

    "create the model to send and return the correct result when there is no prior customer employment data" in {

      val response = underTest.createModelOrReturnError(
        authorisationRequest.user, employmentDataFull, Some(allEmploymentData.copy(hmrcEmploymentData = allEmploymentData.hmrcEmploymentData
          .map(_.copy(employmentId = "employmentId", hmrcEmploymentFinancialData = Some(EmploymentFinancialData(employmentData = None, employmentBenefits = Some(EmploymentBenefits(
            s"${taxYearEOY-1}-04-04T01:01:01Z", Some(Benefits(Some(100.00)))))
          )))))), EmploymentSection.EMPLOYMENT_DETAILS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        None, Some(CreateUpdateEmployment(Some("123/12345"), "Employer Name", s"${taxYearEOY-1}-11-11", None, None)), Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), None,
          Some(Benefits(Some(100.0), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)))), Some("employmentId")
      )
    }

    "create the model to send and return the correct result when there are benefits already" in {

      val response = underTest.createModelOrReturnError(
        authorisationRequest.user, employmentDataFull, Some(allEmploymentData.copy(hmrcEmploymentData = allEmploymentData.hmrcEmploymentData
          .map(x => x.copy(employmentId = "employmentId", hmrcEmploymentFinancialData = Some(EmploymentFinancialData(employmentData = x.hmrcEmploymentFinancialData.get.employmentData,employmentBenefits = Some(EmploymentBenefits(
            s"${taxYearEOY-1}-04-04T01:01:01Z", Some(Benefits(Some(100.00)))))
          )))))), EmploymentSection.EMPLOYMENT_DETAILS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        None, Some(CreateUpdateEmployment(Some("123/12345"), "Employer Name", s"${taxYearEOY-1}-11-11", None, None)), Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), Some(Deductions(Some(StudentLoans(Some(100.0), Some(100.0))))),
          Some(Benefits(Some(100.0), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)))), Some("employmentId")
      )
    }

    "create the model to send and return the correct result when its a customer update" in {

      val response = underTest.createModelOrReturnError(
        authorisationRequest.user, employmentDataFull, Some(allEmploymentData.copy(hmrcEmploymentData = Seq(), customerEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.toEmploymentSource).map(_.copy(employmentId = "employmentId")))),

        EmploymentSection.EMPLOYMENT_DETAILS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), Some(CreateUpdateEmployment(Some("123/12345"), "Employer Name", s"${taxYearEOY-1}-11-11", None, None)), Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), Some(Deductions(Some(StudentLoans(Some(100.0), Some(100.0))))), None)), None
      )
    }

    "create the model to send and return the correct result when its a customer update for just employment info" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(),
            None,
            Seq(
              EmploymentSource(
                employmentDataFull.employmentId,
                "Name",
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                Some(EmploymentData(
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                  Some(Pay(
                    employmentDataFull.employment.employmentDetails.taxablePayToDate,
                    employmentDataFull.employment.employmentDetails.totalTaxToDate,
                    None, None, None, None
                  )), None
                )),
                employmentBenefits = Some(EmploymentBenefits(employmentDataFull.employment.employmentBenefits.get.submittedOn.get,
                  Some(employmentDataFull.employment.employmentBenefits.get.toBenefits)))
              )
            ), None
          )
        ), EmploymentSection.EMPLOYMENT_DETAILS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), Some(CreateUpdateEmployment(Some("123/12345"), "Employer Name", s"${taxYearEOY-1}-11-11", None, None)), None, None
      )
    }

    "create the model to send and return the correct result when its a customer update for just employment data info" in {

      lazy val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull, Some(
          AllEmploymentData(
            Seq(),
            None,
            Seq(
              EmploymentSource(
                employmentDataFull.employmentId,
                employmentDataFull.employment.employmentDetails.employerName,
                employmentDataFull.employment.employmentDetails.employerRef,
                employmentDataFull.employment.employmentDetails.payrollId,
                employmentDataFull.employment.employmentDetails.startDate,
                employmentDataFull.employment.employmentDetails.cessationDate,
                employmentDataFull.employment.employmentDetails.dateIgnored,
                employmentDataFull.employment.employmentDetails.employmentSubmittedOn,
                Some(EmploymentData(
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None,
                  Some(Pay(
                    Some(3455545.55),
                    employmentDataFull.employment.employmentDetails.totalTaxToDate,
                    None, None, None, None
                  )), None
                )),
                employmentBenefits = Some(EmploymentBenefits(employmentDataFull.employment.employmentBenefits.get.submittedOn.get,
                  Some(employmentDataFull.employment.employmentBenefits.get.toBenefits)))
              )
            ), None
          )
        ), EmploymentSection.EMPLOYMENT_DETAILS
      )

      response.right.get shouldBe CreateUpdateEmploymentRequest(
        Some("employmentId"), None, Some(CreateUpdateEmploymentData(CreateUpdatePay(55.99, 3453453.0), None,
          Some(Benefits(None, Some(100.0), Some(100.0), None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)))), None
      )
    }

    "create the model to send and return a redirect when no finished" in {

      val response = underTest.createModelOrReturnError(
        authorisationRequest.user,

        employmentDataFull.copy(
          employment = employmentDataFull.employment.copy(
            employmentDetails = employmentDataFull.employment.employmentDetails.copy(
              startDate = None
            )
          )
        ), Some(allEmploymentData), EmploymentSection.EMPLOYMENT_DETAILS
      )


      response.left.get shouldBe JourneyNotFinished
    }
  }

  ".createOrUpdateSessionData" should {
    val cya: EmploymentCYAModel = EmploymentCYAModel(
      EmploymentDetails("Employer Name", currentDataIsHmrcHeld = true),
      None
    )
    val employmentData: EmploymentUserData = EmploymentUserData(sessionId, "1234567890", nino, taxYear, "employmentId", isPriorSubmission = true,
      hasPriorBenefits = true,
      hasPriorStudentLoans = true, cya, testClock.now())

    "return SEE_OTHER(303) status when createOrUpdate succeeds" in {
      mockCreateOrUpdate(employmentData, Right())

      val response = underTest.createOrUpdateSessionData(user, taxYear, "employmentId", cya, isPriorSubmission = true, hasPriorBenefits = true, hasPriorStudentLoans = true)(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "return BAD_REQUEST(400) status when createOrUpdate fails" in {
      val cya = EmploymentCYAModel(
        EmploymentDetails("Employer Name", currentDataIsHmrcHeld = true),
        None
      )

      mockCreateOrUpdate(employmentData, Left(DataNotUpdated))

      val response = underTest.createOrUpdateSessionData(user = user, taxYear = taxYear, employmentId = "employmentId", cyaModel = cya, isPriorSubmission = true, hasPriorBenefits = true, hasPriorStudentLoans = true)(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "400"
    }
  }

  ".createOrUpdateEmploymentUserData" should {
    "create EmploymentUserData in repository end return successful result" in {
      val expected = EmploymentUserData(
        user.sessionId,
        user.mtditid,
        user.nino,
        taxYear,
        "employmentId",
        isPriorSubmission = true,
        hasPriorBenefits = true, hasPriorStudentLoans = true,
        employment = anEmploymentCYAModel,
        testClock.now()
      )

      mockCreateOrUpdate(expected, Right())

      val response = underTest.createOrUpdateEmploymentUserData(user, taxYear, "employmentId", expected, anEmploymentCYAModel)

      await(response) shouldBe Right(expected)
    }

    "return Left when repository createOrUpdate fails" in {
      val expected = EmploymentUserData(
        user.sessionId,
        user.mtditid,
        user.nino,
        taxYear,
        "employmentId",
        isPriorSubmission = true,
        hasPriorBenefits = false,
        hasPriorStudentLoans = false,
        employment = anEmploymentCYAModel,
        testClock.now()
      )

      mockCreateOrUpdate(expected, Left(DataNotUpdated))

      val response = underTest.createOrUpdateEmploymentUserData(user, taxYear, "employmentId", expected, anEmploymentCYAModel)

      await(response) shouldBe Left()
    }
  }

  ".createOrUpdateExpensesSessionData" should {
    val isPriorSubmission = true
    val hasPriorExpenses = true
    val expensesUserData = ExpensesUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      isPriorSubmission,
      hasPriorExpenses,
      anExpensesCYAModel,
      testClock.now()
    )

    "return onSuccess block when createOrUpdate succeeds" in {
      val onSuccessBlock = {
        Redirect("303")
      }

      mockCreateOrUpdateExpenses(expensesUserData, Right())

      val response = underTest.createOrUpdateExpensesSessionData(anExpensesCYAModel, taxYear,
        isPriorSubmission, hasPriorExpenses, user)(Redirect("400"))(onSuccessBlock)

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "return onFail block when createOrUpdate fails" in {
      val onFailBlock = {
        Redirect("400")
      }

      mockCreateOrUpdateExpenses(expensesUserData, Left(DataNotUpdated))

      val response = underTest.createOrUpdateExpensesSessionData(anExpensesCYAModel, taxYear = taxYear,
        isPriorSubmission = true, hasPriorExpenses = true, user)(onFailBlock)(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "400"
    }
  }

  ".createOrUpdateExpensesUserData" should {
    "create EmploymentUserData in repository end return successful result" in {
      val expected = ExpensesUserData(
        user.sessionId,
        user.mtditid,
        user.nino,
        taxYear,
        isPriorSubmission = true,
        hasPriorExpenses = false,
        expensesCya = anExpensesCYAModel,
        testClock.now()
      )

      mockCreateOrUpdateExpenses(expected, Right())

      val response = underTest.createOrUpdateExpensesUserData(user, taxYear, isPriorSubmission = true, hasPriorExpenses = false, anExpensesCYAModel)

      await(response) shouldBe Right(expected)
    }

    "return Left when repository createOrUpdate fails" in {
      val expected = ExpensesUserData(
        user.sessionId,
        user.mtditid,
        user.nino,
        taxYear,
        isPriorSubmission = true,
        hasPriorExpenses = false,
        expensesCya = anExpensesCYAModel,
        testClock.now()
      )

      mockCreateOrUpdateExpenses(expected, Left(DataNotUpdated))

      val response = underTest.createOrUpdateExpensesUserData(user, taxYear, isPriorSubmission = true, hasPriorExpenses = false, anExpensesCYAModel)

      await(response) shouldBe Left()
    }
  }

  ".getSessionDataAndReturnResult" should {
    val cya: EmploymentCYAModel = EmploymentCYAModel(
      EmploymentDetails("Employer Name", currentDataIsHmrcHeld = true),
      None
    )
    val employmentData: EmploymentUserData = EmploymentUserData(sessionId, "1234567890", nino, taxYear, "employmentId", isPriorSubmission = true,
      hasPriorBenefits = true, hasPriorStudentLoans = false, cya, testClock.now())

    "redirect when data is retrieved" in {
      mockFind(taxYear, "employmentId", Right(Some(employmentData)))

      val response = underTest.getSessionDataAndReturnResult(taxYear, "employmentId")() {
        _ => Future.successful(Redirect("303"))
      }

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "redirect to overview when no data is retrieved" in {
      mockFind(taxYear, "employmentId", Right(None))

      val response = underTest.getSessionDataAndReturnResult(taxYear, "employmentId")() {
        _ => Future.successful(Redirect("303"))
      }

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "/overview"
    }

    "return internal server error when DatabaseError" in {
      mockFind(taxYear, "employmentId", Left(DataNotUpdated))

      val response = underTest.getSessionDataAndReturnResult(taxYear, "employmentId")() {
        _ => Future.successful(anyResult)
      }

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".clear" should {
    "redirect when the record in the database has been removed" in {
      mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
      mockClear(taxYear, "employmentId", response = true)

      await(underTest.clear(user, taxYear, "employmentId")) shouldBe Right()
    }

    "redirect to error when the record in the database has not been removed" in {
      mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
      mockClear(taxYear, "employmentId", response = false)

      await(underTest.clear(user, taxYear, "employmentId")) shouldBe Left()
    }

    "error when incomeSourceConnector returns error" in {
      mockRefreshIncomeSourceResponseError(taxYear, nino)
      mockClear(taxYear, "employmentId", response = true)

      await(underTest.clear(user, taxYear, "employmentId")) shouldBe Left()
    }
  }

  ".findPreviousEmploymentUserData" should {
    "return the ok result" in {
      mockFind(nino, taxYear, userData)

      val response = underTest.findPreviousEmploymentUserData(
        User(mtditid = "1234567890", arn = None, nino = nino, sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81", AffinityGroup.Individual.toString),
        taxYear,
      )(result)

      status(response) shouldBe OK
    }

    "return the a redirect when no data" in {
      mockFind(nino, taxYear, userData.copy(employment = None))

      val response = underTest.findPreviousEmploymentUserData(
        User(mtditid = "1234567890", arn = None, nino = nino, sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81", AffinityGroup.Individual.toString),
        taxYear,
      )(result)

      status(response) shouldBe SEE_OTHER
    }
  }

  ".getSessionData" should {
    "return the Internal server error result when DatabaseError" in {
      mockFind(taxYear, "some-employment-id", Left(DataNotFound))

      val response = await(underTest.getSessionData(taxYear, "some-employment-id"))

      status(Future.successful(response.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".getExpensesSessionDataResult" should {
    "return the Internal server error result when DatabaseError" in {
      mockFind(taxYear, user, Left(DataNotUpdated))

      val response = underTest.getExpensesSessionDataResult(taxYear) {
        _ => Future.successful(anyResult)
      }

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    ".clearExpenses" should {
      "redirect when the record in the database has been removed" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
        mockClear(taxYear, user, response = true)

        val response = underTest.clearExpenses(taxYear)(Redirect("303"))

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "303"
      }

      "redirect to error when the record in the database has not been removed" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
        mockClear(taxYear, user, response = false)

        val response = underTest.clearExpenses(taxYear)(Redirect("303"))

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }

      "error when incomeSourceConnector returns error" in {
        mockRefreshIncomeSourceResponseError(taxYear, nino)
        mockClear(taxYear, user, response = true)

        val response = underTest.clearExpenses(taxYear)(Redirect("500"))

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return the given result when no DatabaseError" in {
      mockFind(taxYear, user, Right(None))

      val response = underTest.getExpensesSessionDataResult(taxYear) {
        _ => Future.successful(anyResult)
      }

      status(response) shouldBe anyResult.header.status
    }
  }

  ".getSessionDataResult" should {
    "return the Internal server error result when DatabaseError" in {
      mockFind(taxYear, "some-employment-id", Left(DataNotUpdated))

      val response = underTest.getSessionDataResult(taxYear, "some-employment-id") {
        _ => Future.successful(anyResult)
      }

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "return the given result when no DatabaseError" in {
      mockFind(taxYear, "some-employment-id", Right(None))

      val response = underTest.getSessionDataResult(taxYear, "some-employment-id") {
        _ => Future.successful(anyResult)
      }

      status(response) shouldBe anyResult.header.status
    }
  }

  ".findEmploymentUserData" should {
    "returns Left() when DatabaseError" in {
      mockFind(taxYear, "some-employment-id", Left(DataNotUpdated))

      await(underTest.findEmploymentUserData(taxYear, "some-employment-id", user)) shouldBe Left()
    }

    "return the given result when no DatabaseError" in {
      mockFind(taxYear, "some-employment-id", Right(Some(anEmploymentUserData)))

      await(underTest.findEmploymentUserData(taxYear, "some-employment-id", user)) shouldBe Right(Some(anEmploymentUserData))
    }
  }
}