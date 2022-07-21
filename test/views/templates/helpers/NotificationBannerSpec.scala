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

package views.templates.helpers

import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import views.html.templates.helpers.NotificationBanner

class NotificationBannerSpec extends ViewUnitTest {

  object Selectors {
    var bannerTitleSelector: String = "#govuk-notification-banner-title"
    var bannerContentSelector: String = "#my-paragraph"
  }

  trait CommonExpectedResults {
    val bannerTitle: String
    val bannerContent: String
  }

  trait SpecificExpectedResults {
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val bannerTitle: String = "Important"
    val bannerContent: String = "some-content"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val bannerTitle: String = "Pwysig"
    val bannerContent: String = "some-content"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None)
  )

  private val underTest = inject[NotificationBanner]

  userScenarios.foreach { userScenario =>
    import Selectors._
    val common = userScenario.commonExpectedResults
    s"language is ${welshTest(userScenario.isWelsh)}" should {
      "show correctly translated title" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(HtmlContent("<p id='my-paragraph'>some-content</p>").asHtml)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        textOnPageCheck(common.bannerTitle, bannerTitleSelector)
        textOnPageCheck(common.bannerContent, bannerContentSelector)
      }
    }
  }
}
