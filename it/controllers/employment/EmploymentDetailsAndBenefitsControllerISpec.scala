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

package controllers.employment

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}


class EmploymentDetailsAndBenefitsControllerISpec extends IntegrationTest with ViewHelpers {

  val url = s"$appUrl/2022/employer-details-and-benefits?employmentId=001"

  object Selectors {
    val headingSelector = "#main-content > div > div > header > h1"
    val subHeadingSelector = "#main-content > div > div > header > p"
    val p1Selector = "#main-content > div > div > p"
    val buttonSelector = "#employmentSummaryBtn"
    val employmentDetailsLinkSelector = "#employment-details_link"
    val employmentBenefitsLinkSelector = "#employment-benefits_link"

    def taskListRowFieldNameSelector(i: Int) = s"#main-content > div > div > ul > li:nth-child($i) > span.app-task-list__task-name"

    def taskListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > ul > li:nth-child($i) > span.hmrc-status-tag"
  }

  object ExpectedResults {

    val employmentDetailsUrl = "/income-through-software/return/employment-income/2022/check-employment-details?employmentId=001"
    val employmentBenefitsUrl = "/income-through-software/return/employment-income/2022/check-employment-benefits?employmentId=001"

    object ContentEN {
      val h1Expected = "maggie"
      val titleExpected = "Employment details and benefits"
      val captionExpected = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
      val p1ExpectedAgent = "You cannot update your client’s employment information until 6 April 2022."
      val p1ExpectedIndividual = "You cannot update your employment information until 6 April 2022."
      val fieldNames = List("Employment details", "Benefits")
      val buttonText = "Return to employment summary"
    }

    object ContentCY {
      val h1Expected = "maggie"
      val titleExpected = "Employment details and benefits"
      val captionExpected = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
      val p1ExpectedAgent = "You cannot update your client’s employment information until 6 April 2022."
      val p1ExpectedIndividual = "You cannot update your employment information until 6 April 2022."
      val fieldNames = List("Employment details", "Benefits")
      val buttonText = "Return to employment summary"
    }
  }

  "in english" when {

    import ExpectedResults.ContentCY._
    import ExpectedResults.employmentDetailsUrl
    import ExpectedResults.employmentBenefitsUrl
    import Selectors._

    "the user is an individual" when {

      ".show" should {

        "render the page where the status for benefits is Cannot Update when there is no Benefits data in mongo" which {

          implicit lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(fullEmploymentsModel(None)), nino, taxYear)
            urlGet(url, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          captionCheck(captionExpected)
          textOnPageCheck(p1ExpectedIndividual, p1Selector)

          "has an employment details section" which {
            linkCheck(fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            textOnPageCheck(fieldNames(1), taskListRowFieldNameSelector(2))
            textOnPageCheck("Cannot update", taskListRowFieldAmountSelector(2))
          }
          buttonCheck(buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(ENGLISH)
        }

        "redirect when there is no data" in {

          lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(fullEmploymentsModel(None).copy(hmrcEmploymentData = Seq())), nino, taxYear)
            urlGet(url, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
        }

        "render the page where the status for benefits is Updated when there is Benefits data in mongo" which {

          implicit lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, taxYear)
            urlGet(url, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          captionCheck(captionExpected)

          textOnPageCheck(p1ExpectedIndividual, p1Selector)

          "has an employment details section" which {
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            linkCheck(fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(2))
          }

          buttonCheck(buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(ENGLISH)
        }
      }

      "render Unauthorised user error page" which {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          urlGet(url)
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

    "the user is an agent" when {

      ".show" should {

        "render the page where the status for benefits is Cannot Update when there is no Benefits data in mongo" which {

          lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(fullEmploymentsModel(None)), nino, taxYear)
            urlGet(url, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          captionCheck(captionExpected)

          textOnPageCheck(p1ExpectedAgent, p1Selector)

          "has an employment details section" which {
            linkCheck(fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            textOnPageCheck(fieldNames(1), taskListRowFieldNameSelector(2))
            textOnPageCheck("Cannot update", taskListRowFieldAmountSelector(2))
          }

          buttonCheck(buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(ENGLISH)
        }

        "render the page where the status for benefits is Updated when there is Benefits data in mongo" which {

          implicit lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, taxYear)
            urlGet(url, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          captionCheck(captionExpected)

          textOnPageCheck(p1ExpectedAgent, p1Selector)

          "has an employment details section" which {
            linkCheck(fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            linkCheck(fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(2))
          }

          buttonCheck(buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(ENGLISH)
        }
      }

      "render Unauthorised user error page" which {
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          urlGet(url)
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }
  }

  "in welsh" when {

    import ExpectedResults.ContentCY._
    import ExpectedResults.employmentDetailsUrl
    import ExpectedResults.employmentBenefitsUrl
    import Selectors._

    "when the user is an individual" when {

      ".show" should {

        "render the page where the status for benefits is Cannot Update when there is no Benefits data in mongo" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(fullEmploymentsModel(None)), nino, taxYear)
            urlGet(url, welsh = true, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          captionCheck(captionExpected)

          textOnPageCheck(p1ExpectedIndividual, p1Selector)

          "has an employment details section" which {
            linkCheck(fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            textOnPageCheck(fieldNames(1), taskListRowFieldNameSelector(2))
            textOnPageCheck("Cannot update", taskListRowFieldAmountSelector(2))
          }

          buttonCheck(buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(WELSH)
        }

        "render the page where the status for benefits is Updated when there is Benefits data in mongo" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, taxYear)
            urlGet(url, welsh = true, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          captionCheck(captionExpected)

          textOnPageCheck(p1ExpectedIndividual, p1Selector)

          "has an employment details section" which {
            linkCheck(fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            linkCheck(fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(2))
          }

          buttonCheck(buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(WELSH)
        }
      }
    }

    "the user is an agent" when {

      ".show" should {

        "render the page where the status for benefits is Cannot Update when there is no Benefits data in mongo" which {

          lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(fullEmploymentsModel(None)),nino,taxYear)
            urlGet(url, welsh = true, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          captionCheck(captionExpected)

          textOnPageCheck(p1ExpectedAgent, p1Selector)

          "has an employment details section" which {
            linkCheck(fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            textOnPageCheck(fieldNames(1), taskListRowFieldNameSelector(2))
            textOnPageCheck("Cannot update", taskListRowFieldAmountSelector(2))
          }

          buttonCheck(buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(WELSH)
        }

        "render the page where the status for benefits is Updated when there is Benefits data in mongo" which {

          lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, taxYear)
            urlGet(url, welsh = true, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          captionCheck(captionExpected)

          textOnPageCheck(p1ExpectedAgent, p1Selector)

          "has an employment details section" which {
            linkCheck(fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            linkCheck(fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl)
            textOnPageCheck("Updated", taskListRowFieldAmountSelector(2))
          }

          buttonCheck(buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(WELSH)
        }
      }
    }
  }
}
