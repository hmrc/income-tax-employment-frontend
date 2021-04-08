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

package views.templates.helpers

import config.{AppConfig, MockAppConfig}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.ViewTest
import views.html.templates.helpers.BetaBar

class BetaBarViewSpec extends ViewTest {

  lazy val betaBar: BetaBar = app.injector.instanceOf[BetaBar]

  private val aTagSelector = "a"

  "BetaBarView" when {

    "provided with an implicit appConfig" should {

      "use appConfig.feedbackUrl in the beta banner link" which {

        implicit val appConfig: AppConfig = new MockAppConfig().config
        lazy val view = betaBar()(fakeRequest, messages, appConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        "contains the correct href value" in {
          element(aTagSelector).attr("href") shouldBe appConfig.betaFeedbackUrl
        }
      }
    }
  }

}
