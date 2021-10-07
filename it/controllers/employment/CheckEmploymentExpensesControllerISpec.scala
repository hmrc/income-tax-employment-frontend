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

import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import models.{IncomeTaxUserData, User}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}


class CheckEmploymentExpensesControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper{

  def url(taxYearToUse: Int = taxYear) = s"$appUrl/$taxYearToUse/check-employment-expenses"

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
    def expectedInsetText(taxYear:Int = taxYear): String
    val expectedContentSingle: String
    val expectedContentMultiple: String
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear:Int = taxYear): String
    val fieldNames: Seq[String]
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear:Int = taxYear) = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val fieldNames = Seq("Other business travel expenses",
      "Job expenses",
      "Uniforms, work clothes, or tools",
      "Professional fees and subscriptions",
      "Hotel and meal expenses",
      "Other expenses",
      "Vehicle expenses",
      "Mileage allowance relief")
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear:Int = taxYear) = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val fieldNames = Seq("Other business travel expenses",
      "Job expenses",
      "Uniforms, work clothes, or tools",
      "Professional fees and subscriptions",
      "Hotel and meal expenses",
      "Other expenses",
      "Vehicle expenses",
      "Mileage allowance relief")
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your employment expenses"
    val expectedTitle = "Check your employment expenses"
    val expectedContentSingle = "Your employment expenses are based on the information we already hold about you."
    val expectedContentMultiple = "Your employment expenses are based on the information we already hold about you. " +
      "This is a total of expenses from all employment in the tax year."
    def expectedInsetText(taxYear:Int = taxYear) = s"You cannot update your employment expenses until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment expenses"
    val expectedTitle = "Check your client’s employment expenses"
    val expectedContentSingle = "Your client’s employment expenses are based on information we already hold about them."
    val expectedContentMultiple = "Your client’s employment expenses are based on information we already hold about them. " +
      "This is a total of expenses from all employment in the tax year."
    def expectedInsetText(taxYear:Int = taxYear) = s"You cannot update your client’s employment expenses until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Check your employment expenses"
    val expectedTitle = "Check your employment expenses"
    val expectedContentSingle = "Your employment expenses are based on the information we already hold about you."
    val expectedContentMultiple = "Your employment expenses are based on the information we already hold about you. " +
      "This is a total of expenses from all employment in the tax year."
    def expectedInsetText(taxYear:Int = taxYear) = s"You cannot update your employment expenses until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment expenses"
    val expectedTitle = "Check your client’s employment expenses"
    val expectedContentSingle = "Your client’s employment expenses are based on information we already hold about them."
    val expectedContentMultiple = "Your client’s employment expenses are based on information we already hold about them. " +
      "This is a total of expenses from all employment in the tax year."
    def expectedInsetText(taxYear:Int = taxYear) = s"You cannot update your client’s employment expenses until 6 April $taxYear."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val multipleEmployments = fullEmploymentsModel(Seq(employmentDetailsAndBenefits(employmentId = "002"), employmentDetailsAndBenefits()))

  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return a fully populated page when all the fields are populated" which {

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
            urlGet(url(), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption())
          textOnPageCheck(user.specificExpectedResults.get.expectedContentSingle, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText(), insetTextSelector)
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

        "return a fully populated page when all the fields are populated at the end of the year" which {

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear-1)
            urlGet(url(taxYear-1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear-1))
          textOnPageCheck(user.specificExpectedResults.get.expectedContentSingle, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText(taxYear-1), insetTextSelector)
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
        "return a empty populated page when all the fields are empty at the end of the year" which {

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel().copy(hmrcExpenses = None)), nino, taxYear-1)
            urlGet(url(taxYear-1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear-1))
          textOnPageCheck(user.specificExpectedResults.get.expectedContentSingle, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText(taxYear-1), insetTextSelector)
          welshToggleCheck(user.isWelsh)
        }
        "return a empty populated page when there is no prior data at the end of the year" which {

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(), nino, taxYear-1)
            urlGet(url(taxYear-1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear-1))
          textOnPageCheck(user.specificExpectedResults.get.expectedContentSingle, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText(taxYear-1), insetTextSelector)
          welshToggleCheck(user.isWelsh)
        }

        "return a fully populated page when all the fields are populated at the end of the year when there is CYA data" which {

          def expensesUserData(isPrior: Boolean, employmentCyaModel: ExpensesCYAModel): ExpensesUserData =
            ExpensesUserData(sessionId, mtditid, nino, taxYear-1, isPriorSubmission = isPrior, employmentCyaModel)

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(true,ExpensesCYAModel(employmentExpenses.expenses.get,true)),userRequest)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear-1)
            urlGet(url(taxYear-1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear-1))
          textOnPageCheck(user.specificExpectedResults.get.expectedContentSingle, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText(taxYear-1), insetTextSelector)
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
            urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContentMultiple, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText(taxYear), insetTextSelector)
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
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel().copy(hmrcExpenses = None)),nino,taxYear)
            urlGet(url(), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }

        "returns an action when auth call fails" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            unauthorisedAgentOrIndividual(user.isAgent)
            urlGet(url(), welsh = user.isWelsh)
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return a redirect when in year" which {

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
            urlPost(url(), body = "{}", welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has a url of overview page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
          }
        }

        "return internal server error page whilst not implemented" in {
          def expensesUserData(isPrior: Boolean, employmentCyaModel: ExpensesCYAModel): ExpensesUserData =
            ExpensesUserData(sessionId, mtditid, nino, taxYear-1, isPriorSubmission = isPrior, employmentCyaModel)

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(true,ExpensesCYAModel(employmentExpenses.expenses.get,true)),userRequest)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear-1)
            urlPost(url(taxYear-1), body = "{}", welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          result.status shouldBe INTERNAL_SERVER_ERROR
        }
        "return a redirect to show method when at end of year" which {

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear-1)
            urlPost(url(taxYear-1), body = "{}", welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has a url of expenses show method" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/check-employment-expenses")
          }
        }
      }
    }
  }
}