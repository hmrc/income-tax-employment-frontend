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
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class TaxYearErrorControllerISpec extends IntegrationTest with ViewHelpers {

  object ExpectedResults {
    object ContentEN {
      val h1Expected = "Page not found"
      val p1Expected = "You can only enter information for the 2021 to 2022 tax year."
      val p2Expected = "Check that you’ve entered the correct web address."
      val p3Expected: String = "If the web address is correct or you selected a link or button, you can use Self Assessment: " +
        "general enquiries (opens in new tab) to speak to someone about your income tax."
      val p3ExpectedLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
      val p3ExpectedLinkText = "Self Assessment: general enquiries (opens in new tab)"
    }

    object ContentCY {
      val h1Expected = "Page not found"
      val p1Expected = "You can only enter information for the 2021 to 2022 tax year."
      val p2Expected = "Check that you’ve entered the correct web address."
      val p3Expected: String = "If the web address is correct or you selected a link or button, you can use Self Assessment: " +
        "general enquiries (opens in new tab) to speak to someone about your income tax."
      val p3ExpectedLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
      val p3ExpectedLinkText = "Self Assessment: general enquiries (opens in new tab)"
    }
  }

  object Selectors {
    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p:nth-child(1)"
    val p2Selector = "#main-content > div > div > div.govuk-body > p:nth-child(2)"
    val p3Selector = "#main-content > div > div > div.govuk-body > p:nth-child(3)"
    val linkSelector = "#govuk-self-assessment-link"
  }

  val url = s"$appUrl/error/wrong-tax-year"

  "When set to english" when {

    import ExpectedResults.ContentEN._
    import Selectors._

    "the page is requested" should {

      "render the page" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          urlGet(url)
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK status" in {
          result.status shouldBe OK
        }

        titleCheck(h1Expected)
        welshToggleCheck("English")
        h1Check(h1Expected, "xl")

        textOnPageCheck(p1Expected,p1Selector)
        textOnPageCheck(p2Expected,p2Selector)
        textOnPageCheck(p3Expected,p3Selector)
        linkCheck(p3ExpectedLinkText, linkSelector, p3ExpectedLink)
      }
    }
  }

  "When set to welsh" when {

    import ExpectedResults.ContentCY._
    import Selectors._

    "the page is requested" should {

      "render the page" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          urlGet(url, true)
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK status" in {
          result.status shouldBe OK
        }

        titleCheck(h1Expected)
        welshToggleCheck("Welsh")
        h1Check(h1Expected, "xl")

        textOnPageCheck(p1Expected,p1Selector)
        textOnPageCheck(p2Expected,p2Selector)
        textOnPageCheck(p3Expected,p3Selector)
        linkCheck(p3ExpectedLinkText, linkSelector, p3ExpectedLink)
      }
    }
  }
}
