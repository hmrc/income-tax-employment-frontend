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

import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import config._
import models.benefits.{AssetsModel, Benefits, BenefitsViewModel}
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.expenses.Expenses
import models.mongo._
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import scala.concurrent.Future

class EmploymentSessionServiceSpec extends UnitTest
  with MockIncomeTaxUserDataConnector
  with MockEmploymentUserDataRepository
  with MockCreateUpdateEmploymentDataConnector
  with MockIncomeSourceConnector
  with MockExpensesUserDataRepository {

  private val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  private val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  private val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  private val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  private val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  private val service: EmploymentSessionService =
    new EmploymentSessionService(mockEmploymentUserDataRepository, mockExpensesUserDataRepository, mockUserDataConnector, mockIncomeSourceConnector, mockAppConfig, messages,
      errorHandler, mockCreateUpdateEmploymentDataConnector, mockExecutionContext)

  private val taxYear = 2022

  private val anyResult = Ok

  private def result(allEmploymentData: AllEmploymentData): Result = {
    allEmploymentData.hmrcExpenses match {
      case Some(_) => Ok("Ok")
      case None => Redirect("303")
    }
  }

  private def allEmploymentData: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      EmploymentSource(
        employmentId = "001",
        employerName = "maggie",
        employerRef = None,
        payrollId = None,
        startDate = None,
        cessationDate = None,
        dateIgnored = None,
        submittedOn = None,
        employmentData = Some(EmploymentData(
          submittedOn = "2020-02-12",
          employmentSequenceNumber = None,
          companyDirector = None,
          closeCompany = None,
          directorshipCeasedDate = None,
          occPen = None,
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
      )
    ),
    hmrcExpenses = Some(
      EmploymentExpenses(
        Some("2020-01-04T05:01:01Z"), None, Some(800.00), Some(
          Expenses(
            Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00), Some(100.00)
          )
        )
      )
    ),
    customerEmploymentData = Seq(),
    customerExpenses = Some(
      EmploymentExpenses(
        Some("2020-01-04T05:01:01Z"), None, Some(800.00), Some(
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
        startDate = Some("2020-11-11"),
        taxablePayToDate = Some(55.99),
        totalTaxToDate = Some(3453453.00),
        employmentSubmittedOn = Some("2020-04-04T01:01:01Z"),
        employmentDetailsSubmittedOn = Some("2020-04-04T01:01:01Z"),
        currentDataIsHmrcHeld = false
      ),
      employmentBenefits = Some(
        BenefitsViewModel(
          assetsModel = Some(AssetsModel(Some(true), Some(true), Some(100.00), Some(true), Some(100.00))),
          submittedOn = Some("2020-02-04T05:01:01Z"), isUsingCustomerData = true
        )
      ))
  }

  private val employmentDataFull: EmploymentUserData = {
    EmploymentUserData(sessionId, "1234567890", nino, taxYear, "employmentId", isPriorSubmission = true,
      hasPriorBenefits = true, employmentCYA, testClock.now())
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

  "getAndHandle" should {
    "redirect if no data and redirect is set to true" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindNoContent(nino, taxYear)
      val response = service.getAndHandle(taxYear, "employmentId", redirectWhenNoPrior = true)((_, _) => Future(Ok))

      status(response) shouldBe SEE_OTHER
    }

    "return an error if the call failed" in {
      mockFind(taxYear, "employmentId", Right(None))
      mockFindFail(nino, taxYear)

      val response = service.getAndHandle(taxYear, "employmentId")((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "return an internal server error if the CYA find failed" in {
      mockFind(taxYear, "employmentId", Left(DataNotFound))
      mockFindNoContent(nino, taxYear)

      val response = service.getAndHandle(taxYear, "employmentId")((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "getAndHandleExpenses" should {
    "return an error if the call failed" in {
      mockFindFail(nino, taxYear)
      mockFind(taxYear, Right(None))

      val response = service.getAndHandleExpenses(taxYear)((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "return an internal server error if the CYA find failed" in {
      mockFind(taxYear, Left(DataNotFound))
      mockFindNoContent(nino, taxYear)

      val response = service.getAndHandleExpenses(taxYear)((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "findPreviousEmploymentUserData" should {
    "return an error if the call failed" in {
      mockFindFail(nino, taxYear)

      val response = service.findPreviousEmploymentUserData(
        User(mtditid = mtditid, arn = None, nino = nino, sessionId = sessionId, AffinityGroup.Individual.toString),
        taxYear)(_ => Ok)

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "createOrUpdateEmploymentResult" should {
    "use the request model to make the api call and return the correct redirect when" when {
      "request has no employmentId" in {
        val requestWithoutEmploymentId = createUpdateEmploymentRequest.copy(employmentId = None)
        mockCreateUpdateEmploymentData(nino, taxYear, requestWithoutEmploymentId)(Right(None))

        val response = service.createOrUpdateEmploymentResult(taxYear, requestWithoutEmploymentId)

        status(response.map(_.right.get)) shouldBe SEE_OTHER
        redirectUrl(response.map(_.right.get)) shouldBe s"/update-and-submit-income-tax-return/employment-income/$taxYear/employment-summary"
      }

      "request has employmentId" in {
        val requestWithEmploymentId = createUpdateEmploymentRequest.copy(employmentId = Some("some-employment-id"))
        mockCreateUpdateEmploymentData(nino, taxYear, requestWithEmploymentId)(Right(None))

        val response = service.createOrUpdateEmploymentResult(taxYear, requestWithEmploymentId)

        status(response.map(_.right.get)) shouldBe SEE_OTHER
        redirectUrl(response.map(_.right.get)) shouldBe s"/update-and-submit-income-tax-return/employment-income/$taxYear/employer-information?employmentId=some-employment-id"
      }
    }

    "use the request model to make the api call and return the correct redirect when it is an add case" in {
      mockCreateUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest)(Right(Some("473474747")))

      val response = service.createOrUpdateEmploymentResult(taxYear, createUpdateEmploymentRequest)

      status(response.map(_.right.get)) shouldBe SEE_OTHER
      redirectUrl(response.map(_.right.get)) shouldBe s"/update-and-submit-income-tax-return/employment-income/$taxYear/check-employment-benefits?employmentId=473474747"
    }

    "use the request model to make the api call  and handle an error" in {
      mockCreateUpdateEmploymentData(nino, taxYear, createUpdateEmploymentRequest)(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      val response = service.createOrUpdateEmploymentResult(taxYear, createUpdateEmploymentRequest)

      status(response.map(_.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "createModelAndReturnResult" should {
    "return to employment details when nothing to update" in {
      lazy val response = service.createModelAndReturnResult(
        employmentDataFull, Some(
          AllEmploymentData(
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
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None, None,
                  Some(Pay(
                    employmentDataFull.employment.employmentDetails.taxablePayToDate,
                    employmentDataFull.employment.employmentDetails.totalTaxToDate,
                    None, None, None, None
                  )), None
                )),
                employmentBenefits = Some(EmploymentBenefits(employmentDataFull.employment.employmentBenefits.get.submittedOn.get,
                  Some(employmentDataFull.employment.employmentBenefits.get.toBenefits)))
              )
            ), None, Seq(), None
          )
        ), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe s"/update-and-submit-income-tax-return/employment-income/$taxYear/employer-information?employmentId=${employmentDataFull.employmentId}"
    }

    "create the model to send and return the correct result" in {
      val response = service.createModelAndReturnResult(
        employmentDataFull, Some(allEmploymentData), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "create the model to send and return the correct result when there is no prior employment data" in {
      val response = service.createModelAndReturnResult(
        employmentDataFull, Some(allEmploymentData.copy(hmrcEmploymentData = allEmploymentData.hmrcEmploymentData
          .map(_.copy(employmentId = "employmentId", employmentData = None, employmentBenefits = Some(EmploymentBenefits(
            "2020-04-04T01:01:01Z", Some(Benefits(Some(100.00)))
          )))))), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "create the model to send and return the correct result when there are benefits already" in {
      val response = service.createModelAndReturnResult(
        employmentDataFull, Some(allEmploymentData.copy(hmrcEmploymentData = allEmploymentData.hmrcEmploymentData
          .map(_.copy(employmentId = "employmentId", employmentBenefits = Some(EmploymentBenefits(
            "2020-04-04T01:01:01Z", Some(Benefits(Some(100.00)))
          )))))), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "create the model to send and return the correct result when its a customer update" in {
      val response = service.createModelAndReturnResult(
        employmentDataFull, Some(allEmploymentData.copy(hmrcEmploymentData = Seq(), customerEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(employmentId = "employmentId")))), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "create the model to send and return the correct result when its a customer update for just employment info" in {
      lazy val response = service.createModelAndReturnResult(
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
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None, None,
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
        ), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "create the model to send and return the correct result when its a customer update for just employment data info" in {
      lazy val response = service.createModelAndReturnResult(
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
                  employmentDataFull.employment.employmentDetails.employmentDetailsSubmittedOn.get, None, None, None, None, None, None,
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
        ), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "create the model to send and return a redirect when no finished" in {
      val response = service.createModelAndReturnResult(
        employmentDataFull.copy(
          employment = employmentDataFull.employment.copy(
            employmentDetails = employmentDataFull.employment.employmentDetails.copy(
              startDate = None
            )
          )
        ), Some(allEmploymentData), taxYear
      )(_ => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "/update-and-submit-income-tax-return/employment-income/2022/check-employment-details?employmentId=employmentId"
    }
  }

  ".createOrUpdateSessionData" should {
    val cya: EmploymentCYAModel = EmploymentCYAModel(
      EmploymentDetails("Employer Name", currentDataIsHmrcHeld = true),
      None
    )
    val employmentData: EmploymentUserData = EmploymentUserData(sessionId, "1234567890", nino, taxYear, "employmentId", isPriorSubmission = true,
      hasPriorBenefits = true, cya, testClock.now())

    "return SEE_OTHER(303) status when createOrUpdate succeeds" in {
      mockCreateOrUpdate(employmentData, Right())

      val response = service.createOrUpdateSessionData(
        "employmentId", cya, taxYear, isPriorSubmission = true,
        hasPriorBenefits = true,
      )(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "return BAD_REQUEST(400) status when createOrUpdate fails" in {
      val cya = EmploymentCYAModel(
        EmploymentDetails("Employer Name", currentDataIsHmrcHeld = true),
        None
      )

      mockCreateOrUpdate(employmentData, Left(DataNotUpdated))

      val response = service.createOrUpdateSessionData(
        "employmentId", cya, taxYear, isPriorSubmission = true,
        hasPriorBenefits = true
      )(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "400"
    }
  }

  ".createOrUpdateEmploymentUserDataWith" should {
    "create EmploymentUserData in repository end return successful result" in {
      val expected = EmploymentUserData(
        user.sessionId,
        user.mtditid,
        user.nino,
        taxYear,
        "employmentId",
        isPriorSubmission = true,
        hasPriorBenefits = false,
        employment = anEmploymentCYAModel,
        testClock.now()
      )

      mockCreateOrUpdate(expected, Right())

      val response = service.createOrUpdateEmploymentUserDataWith(taxYear, "employmentId", isPriorSubmission = true, hasPriorBenefits = false, anEmploymentCYAModel)

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
        employment = anEmploymentCYAModel,
        testClock.now()
      )

      mockCreateOrUpdate(expected, Left(DataNotUpdated))

      val response = service.createOrUpdateEmploymentUserDataWith(taxYear, "employmentId", isPriorSubmission = true, hasPriorBenefits = false, anEmploymentCYAModel)

      await(response) shouldBe Left()
    }
  }

  ".createOrUpdateExpensesUserDataWith" should {
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

      val response = service.createOrUpdateExpensesUserDataWith(taxYear, isPriorSubmission = true, hasPriorExpenses = false, anExpensesCYAModel)

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

      val response = service.createOrUpdateExpensesUserDataWith(taxYear, isPriorSubmission = true, hasPriorExpenses = false, anExpensesCYAModel)

      await(response) shouldBe Left()
    }
  }

  ".getSessionDataAndReturnResult" should {
    val cya: EmploymentCYAModel = EmploymentCYAModel(
      EmploymentDetails("Employer Name", currentDataIsHmrcHeld = true),
      None
    )
    val employmentData: EmploymentUserData = EmploymentUserData(sessionId, "1234567890", nino, taxYear, "employmentId", isPriorSubmission = true,
      hasPriorBenefits = true, cya, testClock.now())

    "redirect when data is retrieved" in {
      mockFind(taxYear, "employmentId", Right(Some(employmentData)))

      val response = service.getSessionDataAndReturnResult(taxYear, "employmentId")() {
        _ => Future.successful(Redirect("303"))
      }

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "redirect to overview when no data is retrieved" in {
      mockFind(taxYear, "employmentId", Right(None))

      val response = service.getSessionDataAndReturnResult(taxYear, "employmentId")() {
        _ => Future.successful(Redirect("303"))
      }

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "/overview"
    }

    "return internal server error when DatabaseError" in {
      mockFind(taxYear, "employmentId", Left(DataNotUpdated))

      val response = service.getSessionDataAndReturnResult(taxYear, "employmentId")() {
        _ => Future.successful(anyResult)
      }

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".clear" should {
    "redirect when the record in the database has been removed" in {
      mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
      mockClear(taxYear, "employmentId", response = true)

      val response = service.clear(taxYear, "employmentId")(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "redirect to error when the record in the database has not been removed" in {
      mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
      mockClear(taxYear, "employmentId", response = false)

      val response = service.clear(taxYear, "employmentId")(Redirect("303"))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "error when incomeSourceConnector returns error" in {
      mockRefreshIncomeSourceResponseError(taxYear, nino, "employment")
      mockClear(taxYear, "employmentId", response = true)

      val response = service.clear(taxYear, "employmentId")(Redirect("303"))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".findPreviousEmploymentUserData" should {
    "return the ok result" in {
      mockFind(nino, taxYear, userData)

      val response = service.findPreviousEmploymentUserData(
        User(mtditid = "1234567890", arn = None, nino = nino, sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
          AffinityGroup.Individual.toString),
        taxYear,
      )(result)

      status(response) shouldBe OK
    }

    "return the a redirect when no data" in {
      mockFind(nino, taxYear, userData.copy(employment = None))

      val response = service.findPreviousEmploymentUserData(
        User(mtditid = "1234567890", arn = None, nino = nino, sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
          AffinityGroup.Individual.toString),
        taxYear,
      )(result
      )

      status(response) shouldBe SEE_OTHER
    }
  }

  ".getSessionData" should {
    "return the Internal server error result when DatabaseError" in {
      mockFind(taxYear, "some-employment-id", Left(DataNotFound))

      val response = await(service.getSessionData(taxYear, "some-employment-id"))

      status(Future.successful(response.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".getExpensesSessionData" should {
    "return the Internal server error result when DatabaseError" in {

      mockFind(taxYear, Left(DataNotFound))

      val response = await(service.getExpensesSessionData(taxYear))

      status(Future.successful(response.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".getExpensesSessionDataResult" should {
    "return the Internal server error result when DatabaseError" in {
      mockFind(taxYear, Left(DataNotUpdated))

      val response = service.getExpensesSessionDataResult(taxYear) {
        _ => Future.successful(anyResult)
      }

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    ".clearExpenses" should {
      "redirect when the record in the database has been removed" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
        mockClear(taxYear, response = true)

        val response = service.clearExpenses(taxYear)(Redirect("303"))

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "303"
      }

      "redirect to error when the record in the database has not been removed" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino, "employment")
        mockClear(taxYear, response = false)

        val response = service.clearExpenses(taxYear)(Redirect("303"))

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }

      "error when incomeSourceConnector returns error" in {
        mockRefreshIncomeSourceResponseError(taxYear, nino, "employment")
        mockClear(taxYear, response = true)

        val response = service.clearExpenses(taxYear)(Redirect("500"))

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return the given result when no DatabaseError" in {
      mockFind(taxYear, Right(None))

      val response = service.getExpensesSessionDataResult(taxYear) {
        _ => Future.successful(anyResult)
      }

      status(response) shouldBe anyResult.header.status
    }
  }

  ".getSessionDataResult" should {
    "return the Internal server error result when DatabaseError" in {
      mockFind(taxYear, "some-employment-id", Left(DataNotUpdated))

      val response = service.getSessionDataResult(taxYear, "some-employment-id") {
        _ => Future.successful(anyResult)
      }

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "return the given result when no DatabaseError" in {
      mockFind(taxYear, "some-employment-id", Right(None))

      val response = service.getSessionDataResult(taxYear, "some-employment-id") {
        _ => Future.successful(anyResult)
      }

      status(response) shouldBe anyResult.header.status
    }
  }
}