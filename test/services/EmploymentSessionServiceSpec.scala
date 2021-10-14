/*
 * Copyright 2021 HM Revenue & Customs
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

import config._
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.mongo.{DataNotFound, DataNotUpdated, EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import scala.concurrent.Future

class EmploymentSessionServiceSpec extends UnitTest with MockIncomeTaxUserDataConnector
  with MockEmploymentUserDataRepository with MockCreateUpdateEmploymentDataConnector with MockExpensesUserDataRepository {

  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  val service: EmploymentSessionService =
    new EmploymentSessionService(mockEmploymentUserDataRepository, mockExpensesUserDataRepository, mockUserDataConnector, mockAppConfig, messages,
      errorHandler, mockCreateUpdateEmploymentDataConnector, mockExecutionContext)

  val taxYear = 2022

  private val anyResult = Ok

  def result(allEmploymentData: AllEmploymentData): Result = {
    allEmploymentData.hmrcExpenses match {
      case Some(expenses) => Ok("Ok")
      case None => Redirect("303")
    }
  }

  def allEmploymentData: AllEmploymentData = AllEmploymentData(
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
        Some("2020-01-04T05:01:01Z"), None, Some(800), Some(
          Expenses(
            Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100)
          )
        )
      )
    ),
    customerEmploymentData = Seq(),
    customerExpenses = Some(
      EmploymentExpenses(
        Some("2020-01-04T05:01:01Z"), None, Some(800), Some(
          Expenses(
            Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100)
          )
        )
      )
    )
  )

  val employmentCYA: EmploymentCYAModel = {
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
          assets = Some(100), submittedOn = Some("2020-02-04T05:01:01Z"), isUsingCustomerData = true
        )
      ))
  }

  val employmentDataFull: EmploymentUserData = {
    EmploymentUserData(sessionId, "1234567890", nino, taxYear, "employmentId", true, employmentCYA, testClock.now())
  }
  val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
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
      val response = service.getAndHandle(taxYear, "employmentId", true)((_, _) => Future(Ok))

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
    "use the request model to make the api call and return the correct redirect" in {
      mockCreateUpdateEmploymentData(nino, taxYear, model)()

      val response = service.createOrUpdateEmploymentResult(taxYear, model)

      status(response.map(_.right.get)) shouldBe SEE_OTHER
      redirectUrl(response.map(_.right.get)) shouldBe s"/income-through-software/return/employment-income/$taxYear/employment-summary"
    }

    "use the request model to make the api call  and handle an error" in {
      mockCreateUpdateEmploymentData(nino, taxYear, model)(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      val response = service.createOrUpdateEmploymentResult(taxYear, model)

      status(response.map(_.left.get)) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "createModelAndReturnResult" should {
    "return to overview when nothing to update" in {
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
      redirectUrl(response) shouldBe s"/income-through-software/return/employment-income/$taxYear/employment-summary"
    }

    "create the model to send and return the correct result" in {
      val response = service.createModelAndReturnResult(
        employmentDataFull, Some(allEmploymentData), taxYear
      )(model => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "create the model to send and return the correct result when there is no prior employment data" in {
      val response = service.createModelAndReturnResult(
        employmentDataFull, Some(allEmploymentData.copy(hmrcEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(employmentId = "employmentId", employmentData = None, employmentBenefits = Some(EmploymentBenefits(
          "2020-04-04T01:01:01Z", Some(Benefits(Some(100)))
        )))))), taxYear
      )(model => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "create the model to send and return the correct result when there are benefits already" in {
      val response = service.createModelAndReturnResult(
        employmentDataFull, Some(allEmploymentData.copy(hmrcEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(employmentId = "employmentId", employmentBenefits = Some(EmploymentBenefits(
          "2020-04-04T01:01:01Z", Some(Benefits(Some(100)))
        )))))), taxYear
      )(model => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "create the model to send and return the correct result when its a customer update" in {
      val response = service.createModelAndReturnResult(
        employmentDataFull, Some(allEmploymentData.copy(hmrcEmploymentData = Seq(), customerEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(employmentId = "employmentId")))), taxYear
      )(model => Future.successful(Redirect("303")))

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
      )(model => Future.successful(Redirect("303")))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2022/check-employment-details?employmentId=employmentId"
    }
  }

  ".createOrUpdateSessionData" should {
    val cya: EmploymentCYAModel = EmploymentCYAModel(
      EmploymentDetails("Employer Name", currentDataIsHmrcHeld = true),
      None
    )
    val employmentData: EmploymentUserData = EmploymentUserData(sessionId, "1234567890", nino, taxYear, "employmentId", true, cya, testClock.now())

    "return SEE_OTHER(303) status when createOrUpdate succeeds" in {
      mockCreateOrUpdate(employmentData, Right())

      val response = service.createOrUpdateSessionData(
        "employmentId", cya, taxYear, true
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
        "employmentId", cya, taxYear, true
      )(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "400"
    }
  }

  ".getSessionDataAndReturnResult" should {
    val cya: EmploymentCYAModel = EmploymentCYAModel(
      EmploymentDetails("Employer Name", currentDataIsHmrcHeld = true),
      None
    )
    val employmentData: EmploymentUserData = EmploymentUserData(sessionId, "1234567890", nino, taxYear, "employmentId", true, cya, testClock.now())

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

  "getLatestExpenses" should {
    "return the latest expenses data" when {
      "only hmrc data is found in year" in {
        val response = service.getLatestExpenses(allEmploymentData, true)
        response shouldBe Some((allEmploymentData.hmrcExpenses.get, false))
      }

      "only hmrc data is found at the end of the year" in {
        val response = service.getLatestExpenses(allEmploymentData.copy(customerExpenses = None), false)
        response shouldBe Some((allEmploymentData.hmrcExpenses.get, false))
      }

      "only customer data is found at the end of the year" in {
        val response = service.getLatestExpenses(allEmploymentData.copy(hmrcExpenses = None), false)
        response shouldBe Some((allEmploymentData.customerExpenses.get, true))
      }

      "there is both customer and hmrc data" in {
        val response = service.getLatestExpenses(allEmploymentData, false)
        response shouldBe Some((allEmploymentData.customerExpenses.get, true))
      }

      "there is both customer and hmrc data but customer has a time submitted" in {
        val response = service.getLatestExpenses(allEmploymentData.copy(hmrcExpenses = allEmploymentData.hmrcExpenses.map(_.copy(submittedOn = None))), false)
        response shouldBe Some((allEmploymentData.customerExpenses.get, true))
      }

      "there is both customer and hmrc data but neither has a time submitted" in {
        val response = service.getLatestExpenses(allEmploymentData.copy(hmrcExpenses = allEmploymentData.hmrcExpenses.map(_.copy(submittedOn = None)),
          customerExpenses = allEmploymentData.customerExpenses.map(_.copy(submittedOn = None))), false)
        response shouldBe Some((allEmploymentData.customerExpenses.map(_.copy(submittedOn = None)).get, true))
      }

      "there is both customer and hmrc data but customer is the latest" in {
        val response = service.getLatestExpenses(allEmploymentData.copy(
          customerExpenses = allEmploymentData.customerExpenses.map(_.copy(submittedOn = Some("2020-02-04T05:01:01Z")))), false)
        response shouldBe Some((allEmploymentData.customerExpenses.map(_.copy(submittedOn = Some("2020-02-04T05:01:01Z"))).get, true))
      }

      "there are no expenses" in {
        val response = service.getLatestExpenses(allEmploymentData.copy(hmrcExpenses = None, customerExpenses = None), false)
        response shouldBe None
      }
    }
  }

  "getLatestEmploymentData" should {
    "return the latest employment data" when {
      "only hmrc data is found in year" in {
        val response = service.getLatestEmploymentData(allEmploymentData, true)
        response shouldBe allEmploymentData.hmrcEmploymentData
      }

      "only hmrc data is found at the end of the year" in {
        val response = service.getLatestEmploymentData(allEmploymentData, false)
        response shouldBe allEmploymentData.hmrcEmploymentData
      }

      "when there is no data in year" in {
        val response = service.getLatestEmploymentData(allEmploymentData.copy(hmrcEmploymentData = Seq()), true)
        response shouldBe Seq()
      }

      "when there is no data at the end of the year" in {
        val response = service.getLatestEmploymentData(allEmploymentData.copy(hmrcEmploymentData = Seq()), false)
        response shouldBe Seq()
      }

      "when there is hmrc data and customer data at the end of the year" in {
        val response = service.getLatestEmploymentData(allEmploymentData.copy(customerEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(employmentId = "C001"))), false)
        response shouldBe Seq(allEmploymentData.hmrcEmploymentData.head, allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "C001"))
      }

      "when there is hmrc data and customer data at the end of the year where the hmrc has been ignored" in {
        val response = service.getLatestEmploymentData(allEmploymentData.copy(customerEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(employmentId = "C001")),
          hmrcEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(dateIgnored = Some("2020-01-04T05:01:01Z")))), false)
        response shouldBe Seq(allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "C001"))
      }

      "when there is hmrc data and customer data at the end of the year where some customer data is an override and some are new" in {
        val response = service.getLatestEmploymentData(allEmploymentData.copy(customerEmploymentData = Seq(
          allEmploymentData.hmrcEmploymentData.head.copy(employerName = "Mr Bean"),
          allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "C001")
        ),
          hmrcEmploymentData = Seq(allEmploymentData.hmrcEmploymentData.head, allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "002"))), false)
        response shouldBe Seq(
          allEmploymentData.hmrcEmploymentData.head,
          allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "002"),
          allEmploymentData.hmrcEmploymentData.head.copy(employerName = "Mr Bean"),
          allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "C001")
        )
      }

      "when there is hmrc data and customer data at the end of the year with different dates" in {
        val response = service.getLatestEmploymentData(allEmploymentData.copy(customerEmploymentData = Seq(
          allEmploymentData.hmrcEmploymentData.head.copy(employerName = "Mr Bean", submittedOn = Some("2020-01-03T05:01:01Z")),
          allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "C001", submittedOn = Some("2020-01-04T05:01:01Z")),
          allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "002", submittedOn = Some("2020-05-04T05:01:01Z"))
        ),
          hmrcEmploymentData = Seq(
            allEmploymentData.hmrcEmploymentData.head.copy(submittedOn = Some("2020-01-04T05:01:01Z")),
            allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "002", submittedOn = Some("2020-01-04T05:01:01Z"))
          )), false)
        response shouldBe Seq(
          allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "002", submittedOn = Some("2020-05-04T05:01:01Z")),
          allEmploymentData.hmrcEmploymentData.head.copy(submittedOn = Some("2020-01-04T05:01:01Z")),
          allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "002", submittedOn = Some("2020-01-04T05:01:01Z")),
          allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "C001", submittedOn = Some("2020-01-04T05:01:01Z")),
          allEmploymentData.hmrcEmploymentData.head.copy(employerName = "Mr Bean", submittedOn = Some("2020-01-03T05:01:01Z"))
        )
      }

      "when there is only customer data at the end of the year" in {
        val response = service.getLatestEmploymentData(allEmploymentData.copy(customerEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(employmentId = "C001")),
          hmrcEmploymentData = Seq()), false)
        response shouldBe Seq(allEmploymentData.hmrcEmploymentData.head.copy(employmentId = "C001"))
      }
    }
  }

  "employmentSourceToUse" should {
    "return the latest employment source and whether it is customer data" when {
      "only hmrc data is found in year" in {
        val response = service.employmentSourceToUse(allEmploymentData, "001", true)
        response shouldBe Some(allEmploymentData.hmrcEmploymentData.head, false)
      }

      "only hmrc data is found at the end of the year" in {
        val response = service.employmentSourceToUse(allEmploymentData, "001", false)
        response shouldBe Some(allEmploymentData.hmrcEmploymentData.head, false)
      }

      "there is no data" in {
        val response = service.employmentSourceToUse(allEmploymentData.copy(hmrcEmploymentData = Seq()), "001", false)
        response shouldBe None
      }

      "there is only customer data in year" in {
        val response = service.employmentSourceToUse(allEmploymentData.copy(hmrcEmploymentData = Seq(), customerEmploymentData = allEmploymentData.hmrcEmploymentData), "001", true)
        response shouldBe None
      }

      "there is only customer data at the end of the year" in {
        val response = service.employmentSourceToUse(allEmploymentData.copy(hmrcEmploymentData = Seq(), customerEmploymentData = allEmploymentData.hmrcEmploymentData), "001", false)
        response shouldBe Some(allEmploymentData.hmrcEmploymentData.head, true)
      }

      "there is both hmrc and customer data with the same submitted on date" in {
        val response = service.employmentSourceToUse(
          allEmploymentData.copy(
            hmrcEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-01-04T05:01:01Z"))),
            customerEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-01-04T05:01:01Z"))),
          ),
          "001", false)
        response shouldBe Some(allEmploymentData.hmrcEmploymentData.head.copy(submittedOn = Some("2020-01-04T05:01:01Z")), true)
      }

      "there is both hmrc and customer data when customer has the latest submittedOn date" in {
        val response = service.employmentSourceToUse(
          allEmploymentData.copy(
            hmrcEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-01-04T05:01:01Z"))),
            customerEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-02-04T05:01:01Z"))),
          ),
          "001", false)
        response shouldBe Some(allEmploymentData.hmrcEmploymentData.head.copy(submittedOn = Some("2020-02-04T05:01:01Z")), true)
      }

      "there is both hmrc and customer data but only customer has a submitted on date" in {
        val response = service.employmentSourceToUse(
          allEmploymentData.copy(
            hmrcEmploymentData = allEmploymentData.hmrcEmploymentData,
            customerEmploymentData = allEmploymentData.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-01-04T05:01:01Z")))
          ),
          "001", false)
        response shouldBe Some(allEmploymentData.hmrcEmploymentData.head.copy(submittedOn = Some("2020-01-04T05:01:01Z")), true)
      }

      "there is both hmrc and customer data but both have no submitted on date" in {
        val response = service.employmentSourceToUse(
          allEmploymentData.copy(
            hmrcEmploymentData = allEmploymentData.hmrcEmploymentData,
            customerEmploymentData = allEmploymentData.hmrcEmploymentData
          ),
          "001", false)
        response shouldBe Some(allEmploymentData.hmrcEmploymentData.head, true)
      }
    }
  }

  ".clear" should {
    "redirect when the record in the database has been removed" in {
      mockClear(taxYear, "employmentId", true)

      val response = service.clear(taxYear, "employmentId")(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "redirect to error when the record in the database has not been removed" in {
      mockClear(taxYear, "employmentId", false)

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
