/*
 * Copyright 2023 HM Revenue & Customs
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

package views.templates.helpers

import common.SessionValues
import config.AppConfig
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import support.mocks.MockAppConfig
import support.{TaxYearProvider, UnitTest, ViewHelper}
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.templates.helpers.BetaBar

class BetaBarViewSpec extends UnitTest
  with ViewHelper
  with GuiceOneAppPerSuite
  with TaxYearProvider {

  lazy val betaBar: BetaBar = app.injector.instanceOf[BetaBar]

  private val aTagSelector = "a"

  lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy val messages: Messages = messagesApi.preferred(fakeRequest.withHeaders())
  val mockAppConfig: AppConfig = new MockAppConfig().config()

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, "AA123456A", "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
      fakeRequest)

  "BetaBarView" when {

    "provided with an implicit appConfig" should {

      "use appConfig.feedbackUrl in the beta banner link" which {

        implicit val appConfig: AppConfig = new MockAppConfig().config()
        implicit val isAgent: Boolean = false
        lazy val view = betaBar(isAgent)(fakeRequest, messages, appConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        "contains the correct href value" in {
          element(aTagSelector).attr("href") shouldBe appConfig.betaFeedbackUrl
        }
      }
    }
  }

}
