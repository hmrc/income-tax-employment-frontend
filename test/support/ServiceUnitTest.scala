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

package support

import akka.actor.ActorSystem
import config.AppConfig
import models.AuthorisationRequest
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContent, Result}
import support.builders.models.UserBuilder.aUser
import support.mocks.{MockAppConfig, MockAuthorisedAction}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.InYearUtil

import scala.concurrent.{ExecutionContext, Future}

trait ServiceUnitTest extends UnitTest
  with MockFactory
  with MockAuthorisedAction
  with GuiceOneAppPerSuite
  with FakeRequestHelper
  with TaxYearHelper {

  protected implicit lazy val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val appConfig: AppConfig = new MockAppConfig().config()

  protected val sessionId: String = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"
  protected implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
  protected val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()
  protected implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString), fakeRequest)
  protected implicit val actorSystem: ActorSystem = ActorSystem()

  protected val inYearAction = new InYearUtil()(new MockAppConfig().config())
  protected val nino: String = aUser.nino
  protected val mtditid: String = aUser.mtditid

  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String)(ExecutionContext.Implicits.global))
  }

  def redirectUrl(awaitable: Future[Result]): String = {
    await(awaitable).header.headers.getOrElse("Location", "/")
  }

}
