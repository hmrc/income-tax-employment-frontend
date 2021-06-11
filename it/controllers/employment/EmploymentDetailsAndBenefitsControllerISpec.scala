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

  val employmentDetailsUrl = "/income-through-software/return/employment-income/2022/check-employment-details?employmentId=001"
  val employmentBenefitsUrl = "/income-through-software/return/employment-income/2022/check-employment-benefits?employmentId=001"

  object ExpectedResults {

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

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedContent: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val fieldNames: Seq[String]
    val buttonText: String
    val updated: String
    val cannotUpdate: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = "Employment for 6 April 2021 to 5 April 2022"
    val fieldNames = Seq("Employment details", "Benefits")
    val buttonText = "Return to employment summary"
    val updated = "Updated"
    val cannotUpdate = "Cannot update"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = "Employment for 6 April 2021 to 5 April 2022"
    val fieldNames = Seq("Employment details", "Benefits")
    val buttonText = "Return to employment summary"
    val updated = "Updated"
    val cannotUpdate = "Cannot update"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employment details and benefits"
    val expectedContent: String = "You cannot update your employment information until 6 April 2022."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employment details and benefits"
    val expectedContent: String = "You cannot update your client’s employment information until 6 April 2022."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employment details and benefits"
    val expectedContent: String = "You cannot update your employment information until 6 April 2022."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employment details and benefits"
    val expectedContent: String = "You cannot update your client’s employment information until 6 April 2022."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page where the status for benefits is Cannot Update when there is no Benefits data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(None)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, p1Selector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl)
            textOnPageCheck(user.commonExpectedResults.updated, taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            textOnPageCheck(user.commonExpectedResults.fieldNames(1), taskListRowFieldNameSelector(2))
            textOnPageCheck(user.commonExpectedResults.cannotUpdate, taskListRowFieldAmountSelector(2))
          }
          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary", "#main-content > div > div > form")

          welshToggleCheck(user.isWelsh)
        }

        "redirect when there is no data" in {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(None).copy(hmrcEmploymentData = Seq())), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
        }


        "render the page where the status for benefits is Updated when there is Benefits data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, p1Selector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl)
            textOnPageCheck(user.commonExpectedResults.updated, taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl)
            textOnPageCheck(user.commonExpectedResults.updated, taskListRowFieldAmountSelector(2))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(user.isWelsh)
        }

        "render Unauthorised user error page" which {
          lazy val result: WSResponse = {
            unauthorisedAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh)
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }
}
