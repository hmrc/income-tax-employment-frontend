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
    hmrcExpenses = None,
    customerEmploymentData = Seq(),
    customerExpenses = Some(
      EmploymentExpenses(
        None,Some(800),Some(
          Expenses(
            Some(100),Some(100),Some(100),Some(100),Some(100),Some(100),Some(100),Some(100)
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

      val response = service.getSessionDataAndReturnResult(taxYear,"employmentId"){
        _ => Future.successful(Redirect("303"))
      }

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }
    "redirect to overview when no data is retrieved" in {

      mockFind(taxYear,"employmentId",None)

      val response = service.getSessionDataAndReturnResult(taxYear,"employmentId"){
        _ => Future.successful(Redirect("303"))
      }

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "/overview"
    }
  }

  ".employmentExpensesToUse" should {

    "return None when in year with no hmrc data" in {

      val response = service.employmentExpensesToUse(data, true)

      response shouldBe None
    }
    "return customer data when end of year with customer data" in {

      val response = service.employmentExpensesToUse(data, false)

      response shouldBe data.customerExpenses
    }
  }

  ".shouldUseCustomerExpenses" should {

    "return false when in year" in {

      val response = service.shouldUseCustomerExpenses(data, true)

      response shouldBe false
    }
    "return true when end of year" in {

      val response = service.shouldUseCustomerExpenses(data, false)

      response shouldBe true
    }
    "return false when end of year with no data" in {

      val response = service.shouldUseCustomerExpenses(data.copy(customerExpenses = None), false)

      response shouldBe false
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
