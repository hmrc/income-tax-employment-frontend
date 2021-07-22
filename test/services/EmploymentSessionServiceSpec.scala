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

import config.{AppConfig, ErrorHandler, MockEmploymentUserDataRepository, MockIncomeTaxUserDataConnector}
import models.User
import models.employment._
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import scala.concurrent.Future

class EmploymentSessionServiceSpec extends UnitTest with MockIncomeTaxUserDataConnector with MockEmploymentUserDataRepository{

  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  val service: EmploymentSessionService =
    new EmploymentSessionService(mockEmploymentUserDataRepository, mockUserDataConnector, mockAppConfig, messages, errorHandler, mockExecutionContext)

  val taxYear = 2022

  def result(allEmploymentData: AllEmploymentData): Result = {
    allEmploymentData.hmrcExpenses match {
      case Some(expenses) => Ok("Ok")
      case None => Redirect("303")
    }
  }

  val data: AllEmploymentData = AllEmploymentData(
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
          pay = Some(Pay(Some(34234.15), Some(6782.92), None, None, None, None, None))
        )),
        None
      )
    ),
    hmrcExpenses = Some(
      EmploymentExpenses(
        Some("2020-01-04T05:01:01Z"), Some(800), Some(
          Expenses(
            Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100)
          )
        )
      )
    ),
    customerEmploymentData = Seq(),
    customerExpenses = Some(
      EmploymentExpenses(
        Some("2020-01-04T05:01:01Z"), Some(800), Some(
          Expenses(
            Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100)
          )
        )
      )
    )
  )

  val cya = EmploymentCYAModel(
    EmploymentDetails("Employer Name",currentDataIsHmrcHeld = true),
    None
  )

  val employmentData = EmploymentUserData(sessionId, "1234567890", nino, taxYear, "employmentId", true, cya, testClock.now())

  ".createOrUpdateSessionData" should {

    "return SEE_OTHER(303) status when createOrUpdate succeeds" in {

      mockCreateOrUpdate(employmentData, Some(employmentData))

      val response = service.createOrUpdateSessionData(
        "employmentId", cya, taxYear, true
      )(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "return BAD_REQUEST(400) status when createOrUpdate fails" in {

      val cya = EmploymentCYAModel(
        EmploymentDetails("Employer Name",currentDataIsHmrcHeld = true),
        None
      )

      mockCreateOrUpdate(employmentData, None)

      val response = service.createOrUpdateSessionData(
        "employmentId",cya, taxYear, true
      )(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "400"
    }
  }

  ".getSessionDataAndReturnResult" should {

    "redirect when data is retrieved" in {

      mockFind(taxYear,"employmentId",Some(employmentData))

      val response = service.getSessionDataAndReturnResult(taxYear,"employmentId")(){
        _ => Future.successful(Redirect("303"))
      }

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }
    "redirect to overview when no data is retrieved" in {

      mockFind(taxYear,"employmentId",None)

      val response = service.getSessionDataAndReturnResult(taxYear,"employmentId")(){
        _ => Future.successful(Redirect("303"))
      }

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "/overview"
    }
  }

  "getLatestExpenses" should {
    "return the latest expenses data" when {
      "only hmrc data is found in year" in {
        val response = service.getLatestExpenses(data, true)
        response shouldBe data.hmrcExpenses
      }
      "only hmrc data is found at the end of the year" in {
        val response = service.getLatestExpenses(data.copy(customerExpenses = None), false)
        response shouldBe data.hmrcExpenses
      }
      "only customer data is found at the end of the year" in {
        val response = service.getLatestExpenses(data.copy(hmrcExpenses = None), false)
        response shouldBe data.customerExpenses
      }
      "there is both customer and hmrc data" in {
        val response = service.getLatestExpenses(data, false)
        response shouldBe data.customerExpenses
      }
      "there is both customer and hmrc data but hmrc is the latest" in {
        val response = service.getLatestExpenses(data.copy(hmrcExpenses = data.hmrcExpenses.map(_.copy(submittedOn = Some("2020-02-04T05:01:01Z")))), false)
        response shouldBe data.hmrcExpenses.map(_.copy(submittedOn = Some("2020-02-04T05:01:01Z")))
      }
      "there is both customer and hmrc data but hmrc has a time submitted" in {
        val response = service.getLatestExpenses(data.copy(customerExpenses = data.customerExpenses.map(_.copy(submittedOn = None))), false)
        response shouldBe data.hmrcExpenses
      }
      "there is both customer and hmrc data but customer has a time submitted" in {
        val response = service.getLatestExpenses(data.copy(hmrcExpenses = data.hmrcExpenses.map(_.copy(submittedOn = None))), false)
        response shouldBe data.customerExpenses
      }
      "there is both customer and hmrc data but neither has a time submitted" in {
        val response = service.getLatestExpenses(data.copy(hmrcExpenses = data.hmrcExpenses.map(_.copy(submittedOn = None)),
          customerExpenses = data.customerExpenses.map(_.copy(submittedOn = None))), false)
        response shouldBe data.customerExpenses.map(_.copy(submittedOn = None))
      }
      "there is both customer and hmrc data but customer is the latest" in {
        val response = service.getLatestExpenses(data.copy(
          customerExpenses = data.customerExpenses.map(_.copy(submittedOn = Some("2020-02-04T05:01:01Z")))), false)
        response shouldBe data.customerExpenses.map(_.copy(submittedOn = Some("2020-02-04T05:01:01Z")))
      }
      "there are no expenses" in {
        val response = service.getLatestExpenses(data.copy(hmrcExpenses = None,customerExpenses = None), false)
        response shouldBe None
      }
    }
  }

  "getLatestEmploymentData" should {
    "return the latest employment data" when {
      "only hmrc data is found in year" in {
        val response = service.getLatestEmploymentData(data, true)
        response shouldBe data.hmrcEmploymentData
      }
      "only hmrc data is found at the end of the year" in {
        val response = service.getLatestEmploymentData(data, false)
        response shouldBe data.hmrcEmploymentData
      }
      "when there is no data in year" in {
        val response = service.getLatestEmploymentData(data.copy(hmrcEmploymentData = Seq()), true)
        response shouldBe Seq()
      }
      "when there is no data at the end of the year" in {
        val response = service.getLatestEmploymentData(data.copy(hmrcEmploymentData = Seq()), false)
        response shouldBe Seq()
      }
      "when there is hmrc data and customer data at the end of the year" in {
        val response = service.getLatestEmploymentData(data.copy(customerEmploymentData = data.hmrcEmploymentData.map(_.copy(employmentId = "C001"))), false)
        response shouldBe Seq(data.hmrcEmploymentData.head,data.hmrcEmploymentData.head.copy(employmentId = "C001"))
      }
      "when there is hmrc data and customer data at the end of the year where the hmrc has been ignored" in {
        val response = service.getLatestEmploymentData(data.copy(customerEmploymentData = data.hmrcEmploymentData.map(_.copy(employmentId = "C001")),
          hmrcEmploymentData = data.hmrcEmploymentData.map(_.copy(dateIgnored = Some("2020-01-04T05:01:01Z")))), false)
        response shouldBe Seq(data.hmrcEmploymentData.head.copy(employmentId = "C001"))
      }
      "when there is hmrc data and customer data at the end of the year where some customer data is an override and some are new" in {
        val response = service.getLatestEmploymentData(data.copy(customerEmploymentData = Seq(
          data.hmrcEmploymentData.head.copy(employerName = "Mr Bean"),
          data.hmrcEmploymentData.head.copy(employmentId = "C001")
        ),
          hmrcEmploymentData = Seq(data.hmrcEmploymentData.head, data.hmrcEmploymentData.head.copy(employmentId = "002"))), false)
        response shouldBe Seq(
          data.hmrcEmploymentData.head.copy(employerName = "Mr Bean"),
          data.hmrcEmploymentData.head.copy(employmentId = "002"),
          data.hmrcEmploymentData.head.copy(employmentId = "C001")
        )
      }
      "when there is hmrc data and customer data at the end of the year where some customer data is an override and some are new" +
        " and compares the submitted on dates" in {
        val response = service.getLatestEmploymentData(data.copy(customerEmploymentData = Seq(
          data.hmrcEmploymentData.head.copy(employerName = "Mr Bean", submittedOn = Some("2020-01-03T05:01:01Z")),
          data.hmrcEmploymentData.head.copy(employmentId = "C001", submittedOn = Some("2020-01-04T05:01:01Z")),
          data.hmrcEmploymentData.head.copy(employmentId = "002", submittedOn = Some("2020-05-04T05:01:01Z"))
        ),
          hmrcEmploymentData = Seq(
            data.hmrcEmploymentData.head.copy(submittedOn = Some("2020-01-04T05:01:01Z")),
            data.hmrcEmploymentData.head.copy(employmentId = "002", submittedOn = Some("2020-01-04T05:01:01Z"))
          )), false)
        response shouldBe Seq(
          data.hmrcEmploymentData.head.copy(employmentId = "002", submittedOn = Some("2020-05-04T05:01:01Z")),
          data.hmrcEmploymentData.head.copy(submittedOn = Some("2020-01-04T05:01:01Z")),
          data.hmrcEmploymentData.head.copy(employmentId = "C001", submittedOn = Some("2020-01-04T05:01:01Z"))
        )
      }
      "when there is only customer data at the end of the year" in {
        val response = service.getLatestEmploymentData(data.copy(customerEmploymentData = data.hmrcEmploymentData.map(_.copy(employmentId = "C001")),
          hmrcEmploymentData = Seq()), false)
        response shouldBe Seq(data.hmrcEmploymentData.head.copy(employmentId = "C001"))
      }
    }
  }

  "employmentSourceToUse" should {
    "return the latest employment source and whether it is customer data" when {
      "only hmrc data is found in year" in {

        val response = service.employmentSourceToUse(data,"001",true)
        response shouldBe Some(data.hmrcEmploymentData.head, false)

      }
      "only hmrc data is found at the end of the year" in {

        val response = service.employmentSourceToUse(data,"001",false)
        response shouldBe Some(data.hmrcEmploymentData.head, false)
      }
      "there is no data" in {

        val response = service.employmentSourceToUse(data.copy(hmrcEmploymentData = Seq()),"001",false)
        response shouldBe None
      }
      "there is only customer data in year" in {

        val response = service.employmentSourceToUse(data.copy(hmrcEmploymentData = Seq(), customerEmploymentData = data.hmrcEmploymentData),"001",true)
        response shouldBe None
      }
      "there is only customer data at the end of the year" in {

        val response = service.employmentSourceToUse(data.copy(hmrcEmploymentData = Seq(), customerEmploymentData = data.hmrcEmploymentData),"001",false)
        response shouldBe Some(data.hmrcEmploymentData.head, true)
      }
      "there is both hmrc and customer data with the same submitted on date" in {

        val response = service.employmentSourceToUse(
          data.copy(
            hmrcEmploymentData = data.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-01-04T05:01:01Z"))),
            customerEmploymentData = data.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-01-04T05:01:01Z"))),
          ),
          "001",false)
        response shouldBe Some(data.hmrcEmploymentData.head.copy(submittedOn = Some("2020-01-04T05:01:01Z")), true)
      }
      "there is both hmrc and customer data when customer has the latest submittedOn date" in {

        val response = service.employmentSourceToUse(
          data.copy(
            hmrcEmploymentData = data.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-01-04T05:01:01Z"))),
            customerEmploymentData = data.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-02-04T05:01:01Z"))),
          ),
          "001",false)
        response shouldBe Some(data.hmrcEmploymentData.head.copy(submittedOn = Some("2020-02-04T05:01:01Z")), true)
      }
      "there is both hmrc and customer data when hmrc has the latest submittedOn date" in {

        val response = service.employmentSourceToUse(
          data.copy(
            hmrcEmploymentData = data.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-03-04T05:01:01Z"))),
            customerEmploymentData = data.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-02-04T05:01:01Z"))),
          ),
          "001",false)
        response shouldBe Some(data.hmrcEmploymentData.head.copy(submittedOn = Some("2020-03-04T05:01:01Z")), false)
      }
      "there is both hmrc and customer data but only hmrc has a submitted on date" in {

        val response = service.employmentSourceToUse(
          data.copy(
            hmrcEmploymentData = data.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-01-04T05:01:01Z"))),
            customerEmploymentData = data.hmrcEmploymentData
          ),
          "001",false)
        response shouldBe Some(data.hmrcEmploymentData.head.copy(submittedOn = Some("2020-01-04T05:01:01Z")), false)
      }
      "there is both hmrc and customer data but only customer has a submitted on date" in {

        val response = service.employmentSourceToUse(
          data.copy(
            hmrcEmploymentData = data.hmrcEmploymentData,
            customerEmploymentData = data.hmrcEmploymentData.map(_.copy(submittedOn = Some("2020-01-04T05:01:01Z")))
          ),
          "001",false)
        response shouldBe Some(data.hmrcEmploymentData.head.copy(submittedOn = Some("2020-01-04T05:01:01Z")), true)
      }
      "there is both hmrc and customer data but both have no submitted on date" in {

        val response = service.employmentSourceToUse(
          data.copy(
            hmrcEmploymentData = data.hmrcEmploymentData,
            customerEmploymentData = data.hmrcEmploymentData
          ),
          "001",false)
        response shouldBe Some(data.hmrcEmploymentData.head, true)
      }
    }
  }

  ".clear" should {

    "redirect when the record in the database has been removed" in {

      mockClear(taxYear, "employmentId", true)

      val response = service.clear(taxYear,"employmentId")(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }
    "redirect to error when the record in the database has not been removed" in {

      mockClear(taxYear, "employmentId", false)

      val response = service.clear(taxYear,"employmentId")(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "400"
    }
  }

  ".findPreviousEmploymentUserData" should {

    "return the ok result" in {

      mockFind(nino, taxYear, userData)

      val response = service.findPreviousEmploymentUserData(
        User(mtditid = "1234567890", arn = None, nino = nino, sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
          AffinityGroup.Individual.toString),
        taxYear,
      )(result
      )

      status(response) shouldBe OK
    }
    "return the a redirect when no data" in {

      mockFind(nino, taxYear,userData.copy(employment = None))

      val response = service.findPreviousEmploymentUserData(
        User(mtditid = "1234567890", arn = None, nino = nino, sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
          AffinityGroup.Individual.toString),
        taxYear,
      )( result
      )

      status(response) shouldBe SEE_OTHER
    }
  }
}
