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

package config

import akka.actor.ActorSystem
import common.SessionValues
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.i18n._
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import support.{TaxYearProvider, UnitTest, ViewHelper}
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import scala.concurrent.{ExecutionContext, Future}

class ErrorHandlerSpec extends UnitTest with GuiceOneAppPerSuite with ViewHelper with TaxYearProvider {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val actorSystem: ActorSystem = ActorSystem()
  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]

  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  val h1Expected = "Page not found"

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, "AA123456A", "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
      fakeRequest)

  ".handleError" should {

    "return a ServiceUnavailable when passed a SERVICE_UNAVAILABLE 503" in {


      val result = await(Future.successful(errorHandler.handleError(503)(fakeRequest)))

      await(result.body.consumeData.map(_.utf8String)) should include("Sorry, the service is unavailable")
      result.header.status shouldBe SERVICE_UNAVAILABLE

    }
    "return an InternalServerError when passed anything other than a 503" in {

      val result = await(Future.successful(errorHandler.handleError(400)(fakeRequest)))

      await(result.body.consumeData.map(_.utf8String)) should include("Sorry, there is a problem with the service")
      result.header.status shouldBe INTERNAL_SERVER_ERROR

    }
  }

  "the NotFoundTemplate" should {

    "return the notFoundTemplate when an incorrect web address when been entered" which {

      lazy val view = errorHandler.notFoundTemplate
      lazy implicit val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected, isWelsh = false)
    }


  }


}
