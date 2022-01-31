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

package controllers.expenses

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{fullUrl, overviewUrl, startEmploymentExpensesUrl}

class ExpensesInterruptPageControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    ExpensesUserData(sessionId, mtditid, nino, taxYear - 1, isPriorSubmission = isPrior, hasPriorExpenses, expensesCyaModel)
  
  object Selectors {
    def paragraphSelector(index: Int): String = s"#main-content > div > div > div.govuk-panel.govuk-panel--interruption > form > p:nth-child($index)"

    val continueButtonSelector: String = "button.govuk-button"
  }
  
  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val buttonText: String
    val expectedTitle: String
    val expectedHeading: String
  }

  trait SpecificExpectedResults {
    val expectedExample1: String
    val expectedExample2: String
    val expectedExample3: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val expectedTitle = "Employment expenses"
    val expectedHeading = "Employment expenses"

  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val expectedTitle = "Employment expenses"
    val expectedHeading = "Employment expenses"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedExample1 = "Use this section to update your employment expenses."
    val expectedExample2 = "You can claim expenses you did not claim through your employer."
    val expectedExample3 = "There is one expenses section. This section is for all your employment in the tax year."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedExample1 = "Use this section to update your employment expenses."
    val expectedExample2 = "You can claim expenses you did not claim through your employer."
    val expectedExample3 = "There is one expenses section. This section is for all your employment in the tax year."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedExample1 = "Use this section to update your client’s employment expenses."
    val expectedExample2 = "You can claim expenses your client did not claim through their employer."
    val expectedExample3 = "There is one expenses section. This section is for all your client’s employment in the tax year."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedExample1 = "Use this section to update your client’s employment expenses."
    val expectedExample2 = "You can claim expenses your client did not claim through their employer."
    val expectedExample3 = "There is one expenses section. This section is for all your client’s employment in the tax year."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render expenses interrupt page page with the correct content" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, ExpensesCYAModel(ExpensesViewModel(isUsingCustomerData = false))), aUserRequest)
            urlGet(fullUrl(startEmploymentExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedTitle)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, paragraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample3, paragraphSelector(5))
          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render expenses interrupt page when CYA data exists" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
            urlGet(fullUrl(startEmploymentExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedTitle)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, paragraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample3, paragraphSelector(5))
          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render expenses interrupt page when it is not a prior submission" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              ExpensesCYAModel(ExpensesViewModel(isUsingCustomerData = false))), aUserRequest)
            urlGet(fullUrl(startEmploymentExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedTitle)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, paragraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample3, paragraphSelector(5))
          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
        }

      }
    }


    "redirect to tax overview page with it's not EOY" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlGet(fullUrl(startEmploymentExpensesUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to check employment expenses page when prior submission and if user has no expenses" which {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(startEmploymentExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
        result.header("location") shouldBe None
      }
    }
  }

}

