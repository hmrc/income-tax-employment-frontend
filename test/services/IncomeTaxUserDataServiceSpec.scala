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

import config.{AppConfig, ErrorHandler, MockIncomeTaxUserDataConnector}
import models.User
import models.employment.AllEmploymentData
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import utils.UnitTest
import uk.gov.hmrc.auth.core.AffinityGroup
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Results.{Ok, Redirect}
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

class IncomeTaxUserDataServiceSpec extends UnitTest with MockIncomeTaxUserDataConnector{

  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  val service: IncomeTaxUserDataService = new IncomeTaxUserDataService(mockUserDataConnector,mockAppConfig,messages,errorHandler)

  val taxYear = 2022

  def result(allEmploymentData: AllEmploymentData): Result = {
    allEmploymentData.hmrcExpenses match {
      case Some(expenses) => Ok("Ok")
      case None => Redirect("303")
    }
  }

  ".findUserData" should {

    "return the ok result" in {

      mockFind(nino, taxYear, userData)

      val response = service.findUserData(
        User(mtditid = "1234567890", arn = None, nino = nino, sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
          AffinityGroup.Individual.toString),
        taxYear,
      )(result
      )

      status(response) shouldBe OK
    }
    "return the a redirect when no data" in {

      mockFind(nino, taxYear,userData.copy(employment = None))

      val response = service.findUserData(
        User(mtditid = "1234567890", arn = None, nino = nino, sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
          AffinityGroup.Individual.toString),
        taxYear,
      )( result
      )

      status(response) shouldBe SEE_OTHER
    }
  }
}
