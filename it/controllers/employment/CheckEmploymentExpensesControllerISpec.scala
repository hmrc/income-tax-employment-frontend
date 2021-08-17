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
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}


class CheckEmploymentExpensesControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach {

  val url = s"$appUrl/$taxYear/check-employment-expenses"

  object Selectors {
    val headingSelector = "#main-content > div > div > header > h1"
    val subHeadingSelector = "#main-content > div > div > header > p"
    val contentSelector = "#main-content > div > div > p"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val summaryListSelector = "#main-content > div > div > dl"
    def summaryListRowFieldNameSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dt"
    def summaryListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dd"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedContentSingle: String
    val expectedContentMultiple: String
    val expectedInsetText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val fieldNames: Seq[String]
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val fieldNames = Seq("Amount for business travel and subsistence expenses",
      "Job expenses",
      "Uniform, work cloths and tools (Flat rate expenses)",
      "Professional fees and subscriptions",
      "Hotel and meal expenses",
      "Other expenses and capital allowances",
      "Vehicle expense",
      "Mileage allowance relief")
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val fieldNames = Seq("Amount for business travel and subsistence expenses",
      "Job expenses",
      "Uniform, work cloths and tools (Flat rate expenses)",
      "Professional fees and subscriptions",
      "Hotel and meal expenses",
      "Other expenses and capital allowances",
      "Vehicle expense",
      "Mileage allowance relief")
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your employment expenses"
    val expectedTitle = "Check your employment expenses"
    val expectedContentSingle = "Your employment expenses are based on the information we already hold about you."
    val expectedContentMultiple = "Your employment expenses are based on the information we already hold about you. " +
      "This is a total of expenses from all employment in the tax year."
    val expectedInsetText = s"You cannot update your employment expenses until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment expenses"
    val expectedTitle = "Check your client’s employment expenses"
    val expectedContentSingle = "Your client’s employment expenses are based on information we already hold about them."
    val expectedContentMultiple = "Your client’s employment expenses are based on information we already hold about them. " +
      "This is a total of expenses from all employment in the tax year."
    val expectedInsetText = s"You cannot update your client’s employment expenses until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Check your employment expenses"
    val expectedTitle = "Check your employment expenses"
    val expectedContentSingle = "Your employment expenses are based on the information we already hold about you."
    val expectedContentMultiple = "Your employment expenses are based on the information we already hold about you. " +
      "This is a total of expenses from all employment in the tax year."
    val expectedInsetText = s"You cannot update your employment expenses until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment expenses"
    val expectedTitle = "Check your client’s employment expenses"
    val expectedContentSingle = "Your client’s employment expenses are based on information we already hold about them."
    val expectedContentMultiple = "Your client’s employment expenses are based on information we already hold about them. " +
      "This is a total of expenses from all employment in the tax year."
    val expectedInsetText = s"You cannot update your client’s employment expenses until 6 April $taxYear."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val multipleEmployments= fullEmploymentsModel(None).copy(hmrcEmploymentData = Seq(employmentDetailsAndBenefitsModel(None,"002"))
    ++ Seq (employmentDetailsAndBenefitsModel(None,"001")))
  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return a fully populated page with correct paragraph text when all the fields are populated and there are single employments" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(None)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedContentSingle, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText, insetTextSelector)
          textOnPageCheck(user.commonExpectedResults.fieldNames.head, summaryListRowFieldNameSelector(1))
          textOnPageCheck("£1", summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListRowFieldNameSelector(2))
          textOnPageCheck("£2", summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.fieldNames(2), summaryListRowFieldNameSelector(3))
          textOnPageCheck("£3", summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.fieldNames(3), summaryListRowFieldNameSelector(4))
          textOnPageCheck("£4", summaryListRowFieldAmountSelector(4))
          textOnPageCheck(user.commonExpectedResults.fieldNames(4), summaryListRowFieldNameSelector(5))
          textOnPageCheck("£5", summaryListRowFieldAmountSelector(5))
          textOnPageCheck(user.commonExpectedResults.fieldNames(5), summaryListRowFieldNameSelector(6))
          textOnPageCheck("£6", summaryListRowFieldAmountSelector(6))
          textOnPageCheck(user.commonExpectedResults.fieldNames(6), summaryListRowFieldNameSelector(7))
          textOnPageCheck("£7", summaryListRowFieldAmountSelector(7))
          textOnPageCheck(user.commonExpectedResults.fieldNames(7), summaryListRowFieldNameSelector(8))
          textOnPageCheck("£8", summaryListRowFieldAmountSelector(8))
          welshToggleCheck(user.isWelsh)
        }
        "return a fully populated page with correct paragraph text when all the fields are populated and there are multiple employments" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(multipleEmployments), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedContentMultiple, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText, insetTextSelector)
          textOnPageCheck(user.commonExpectedResults.fieldNames.head, summaryListRowFieldNameSelector(1))
          textOnPageCheck("£1", summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListRowFieldNameSelector(2))
          textOnPageCheck("£2", summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.fieldNames(2), summaryListRowFieldNameSelector(3))
          textOnPageCheck("£3", summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.fieldNames(3), summaryListRowFieldNameSelector(4))
          textOnPageCheck("£4", summaryListRowFieldAmountSelector(4))
          textOnPageCheck(user.commonExpectedResults.fieldNames(4), summaryListRowFieldNameSelector(5))
          textOnPageCheck("£5", summaryListRowFieldAmountSelector(5))
          textOnPageCheck(user.commonExpectedResults.fieldNames(5), summaryListRowFieldNameSelector(6))
          textOnPageCheck("£6", summaryListRowFieldAmountSelector(6))
          textOnPageCheck(user.commonExpectedResults.fieldNames(6), summaryListRowFieldNameSelector(7))
          textOnPageCheck("£7", summaryListRowFieldAmountSelector(7))
          textOnPageCheck(user.commonExpectedResults.fieldNames(7), summaryListRowFieldNameSelector(8))
          textOnPageCheck("£8", summaryListRowFieldAmountSelector(8))
          welshToggleCheck(user.isWelsh)
        }

        "redirect to overview page when theres no expenses" in {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(None).copy(hmrcExpenses = None)),nino,taxYear)
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }

        "returns an action when auth call fails" which {
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