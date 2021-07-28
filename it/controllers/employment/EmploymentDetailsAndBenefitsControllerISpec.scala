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

  def url(taxYear: Int) = s"$appUrl/$taxYear/employer-details-and-benefits?employmentId=001"

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

  def employmentDetailsUrl(taxYear: Int) = s"/income-through-software/return/employment-income/$taxYear/check-employment-details?employmentId=001"
  def employmentBenefitsUrl(taxYear: Int) = s"/income-through-software/return/employment-income/$taxYear/check-employment-benefits?employmentId=001"

  object ExpectedResults {

    object ContentEN {
      val h1Expected = "maggie"
      val titleExpected = "Employment details and benefits"
      def captionExpected(taxYear: Int) = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
      def p1ExpectedAgent(taxYear: Int) = s"You cannot update your client’s employment information until 6 April $taxYear."
      def p1ExpectedIndividual(taxYear: Int) = s"You cannot update your employment information until 6 April $taxYear."
      val fieldNames = List("Employment details", "Benefits")
      val buttonText = "Return to employment summary"
    }

    object ContentCY {
      val h1Expected = "maggie"
      val titleExpected = "Employment details and benefits"
      def captionExpected(taxYear: Int) = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
      def p1ExpectedAgent(taxYear: Int) = s"You cannot update your client’s employment information until 6 April $taxYear."
      def p1ExpectedIndividual(taxYear: Int) = s"You cannot update your employment information until 6 April $taxYear."
      val fieldNames = List("Employment details", "Benefits")
      val buttonText = "Return to employment summary"
    }
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    def expectedContent(taxYear: Int): String
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String
    val fieldNames: Seq[String]
    val buttonText: String
    val updated: String
    val cannotUpdate: String
    val notStarted: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int) = s"Employment for 6 April ${taxYear-1} to 5 April $taxYear"
    val fieldNames = Seq("Employment details", "Benefits")
    val buttonText = "Return to employment summary"
    val updated = "Updated"
    val cannotUpdate = "Cannot update"
    val notStarted = "Not started"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int) = s"Employment for 6 April ${taxYear-1} to 5 April $taxYear"
    val fieldNames = Seq("Employment details", "Benefits")
    val buttonText = "Return to employment summary"
    val updated = "Updated"
    val cannotUpdate = "Cannot update"
    val notStarted = "Not started"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employment details and benefits"
    def expectedContent(taxYear: Int): String = s"You cannot update your employment information until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employment details and benefits"
    def expectedContent(taxYear: Int): String = s"You cannot update your client’s employment information until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employment details and benefits"
    def expectedContent(taxYear: Int): String = s"You cannot update your employment information until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employment details and benefits"
    def expectedContent(taxYear: Int): String = s"You cannot update your client’s employment information until 6 April $taxYear."
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

        "render the page where the status for benefits is Cannot Update when there is no Benefits data in year" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(None)), nino, taxYear)
            urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), p1Selector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl(taxYear))
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

        "redirect to the overview page when there is no data in year" in {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(None).copy(hmrcEmploymentData = Seq())), nino, taxYear)
            urlGet(url(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
        }

        "render the page where the status for benefits is Updated when there is Benefits data in year" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, taxYear)
            urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), p1Selector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl(taxYear))
            textOnPageCheck(user.commonExpectedResults.updated, taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl(taxYear))
            textOnPageCheck(user.commonExpectedResults.updated, taskListRowFieldAmountSelector(2))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2022/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(user.isWelsh)
        }

        "render the page where the status for benefits is Not Started when there is no Benefits data for end of year" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(None)), nino, taxYear-1)
            urlGet(url(taxYear-1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear-1))

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl(taxYear-1))
            textOnPageCheck(user.commonExpectedResults.updated, taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl(taxYear-1))
            textOnPageCheck(user.commonExpectedResults.notStarted, taskListRowFieldAmountSelector(2))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2021/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(user.isWelsh)
        }

        "render the page where the status for benefits is Updated when there is Benefits data for end of year" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, taxYear-1)
            urlGet(url(taxYear-1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear-1))

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, employmentDetailsUrl(taxYear-1))
            textOnPageCheck(user.commonExpectedResults.updated, taskListRowFieldAmountSelector(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl(taxYear-1))
            textOnPageCheck(user.commonExpectedResults.updated, taskListRowFieldAmountSelector(2))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck("/income-through-software/return/employment-income/2021/employment-summary",
            "#main-content > div > div > form")

          welshToggleCheck(user.isWelsh)
        }

        "render Unauthorised user error page" which {
          lazy val result: WSResponse = {
            unauthorisedAgentOrIndividual(user.isAgent)
            urlGet(url(taxYear), welsh = user.isWelsh)
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }
}
