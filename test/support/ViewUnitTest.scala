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

import config.AppConfig
import models.AuthorisationRequest
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.AnyContent
import play.api.test.Injecting
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.UserBuilder.aUser
import support.mocks.MockAppConfig
import uk.gov.hmrc.auth.core.AffinityGroup

trait ViewUnitTest extends UnitTest
  with UserScenarios
  with ViewHelper
  with GuiceOneAppPerSuite
  with Injecting
  with TaxYearProvider
  with FakeRequestProvider {

  private lazy val individualUserRequest: AuthorisationRequest[AnyContent] = anAuthorisationRequest.copy[AnyContent](request = fakeIndividualRequest)
  private lazy val agentUserRequest: AuthorisationRequest[AnyContent] = anAuthorisationRequest.copy[AnyContent](aUser.copy(arn = Some("arn"), affinityGroup = AffinityGroup.Agent.toString))

  protected implicit val mockAppConfig: AppConfig = new MockAppConfig().config()
  protected implicit lazy val messagesApi: MessagesApi = inject[MessagesApi]

  protected lazy val defaultMessages: Messages = messagesApi.preferred(fakeRequest)
  protected lazy val welshMessages: Messages = messagesApi.preferred(Seq(Lang("cy")))

  protected def getMessages(isWelsh: Boolean): Messages = if (isWelsh) welshMessages else defaultMessages

  protected def getAuthRequest(isAgent: Boolean): AuthorisationRequest[AnyContent] =
    if (isAgent) agentUserRequest else individualUserRequest
}
