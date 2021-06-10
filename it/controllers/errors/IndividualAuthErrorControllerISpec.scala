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

package controllers.errors

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class IndividualAuthErrorControllerISpec extends IntegrationTest with ViewHelpers {

  object ExpectedResults {
    object ContentEN {
      val validTitle: String = "You cannot view this page"
      val pageContent: String = "You need to sign up for Making Tax Digital for Income Tax before you can view this page."
      val linkContent: String = "sign up for Making Tax Digital for Income Tax"
      val linkHref: String = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax"
    }

    object ContentCY {
      val validTitle: String = "You cannot view this page"
      val pageContent: String = "You need to sign up for Making Tax Digital for Income Tax before you can view this page."
      val linkContent: String = "sign up for Making Tax Digital for Income Tax"
      val linkHref: String = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax"
    }
  }

  object Selectors {
    val paragraphSelector: String = ".govuk-body"
    val linkSelector: String = paragraphSelector + " > a"
  }

  val url = s"$appUrl/error/you-need-to-sign-up"

  "When set to english" when {

    import ExpectedResults.ContentEN._

    "the page is requested" should {

      "render the page" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          urlGet(url)
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }

        titleCheck(validTitle)
        welshToggleCheck("English")
        h1Check(validTitle, "xl")
        textOnPageCheck(pageContent, Selectors.paragraphSelector)
        linkCheck(linkContent, Selectors.linkSelector, linkHref)
      }
    }
  }

  "When set to welsh" when {

    import ExpectedResults.ContentCY._

    "the page is requested" should {

      "render the page" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          urlGet(url, true)
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }

        titleCheck(validTitle)
        welshToggleCheck("Welsh")
        h1Check(validTitle, "xl")
        textOnPageCheck(pageContent, Selectors.paragraphSelector)
        linkCheck(linkContent, Selectors.linkSelector, linkHref)
      }
    }
  }
}
