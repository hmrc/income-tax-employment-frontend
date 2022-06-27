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

import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import utils.PageUrls.{businessTravelExpensesUrl, checkYourExpensesUrl, fullUrl, startEmploymentExpensesUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ExpensesInterruptPageControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

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
    val expectedExample2: String
  }

  trait SpecificExpectedResults {
    val expectedExample1: String
    val expectedExample3: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val expectedTitle = "Employment expenses"
    val expectedHeading = "Employment expenses"
    val expectedExample2 = "You must add expenses as a total for all employment."

  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText = "Yn eich blaen"
    val expectedTitle = "Treuliau cyflogaeth"
    val expectedHeading = "Treuliau cyflogaeth"
    val expectedExample2 = "Mae’n rhaid i chi ychwanegu treuliau fel cyfanswm ar gyfer pob cyflogaeth."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedExample1 = "Use this section to update your employment expenses."
    val expectedExample3 = "Tell us about expenses you did not claim through your employers."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedExample1 = "Defnyddiwch yr adran hon i ddiweddaru eich treuliau cyflogaeth."
    val expectedExample3 = "Rhowch wybod i ni am dreuliau na wnaethoch eu hawlio drwy’ch cyflogwyr."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedExample1 = "Use this section to update your client’s employment expenses."
    val expectedExample3 = "Tell us about expenses your client did not claim through their employers."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedExample1 = "Defnyddiwch yr adran hon i ddiweddaru treuliau cyflogaeth eich cleient."
    val expectedExample3 = "Rhowch wybod i ni am dreuliau na wnaeth eich cleient eu hawlio drwy ei gyflogwyr."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render expenses interrupt page page with the correct content" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, ExpensesCYAModel(ExpensesViewModel(isUsingCustomerData = false))))
            urlGet(fullUrl(startEmploymentExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedTitle)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, paragraphSelector(3))
          textOnPageCheck(expectedExample2, paragraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample3, paragraphSelector(5))
          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render expenses interrupt page when CYA data exists" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
            urlGet(fullUrl(startEmploymentExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedTitle)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, paragraphSelector(3))
          textOnPageCheck(expectedExample2, paragraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample3, paragraphSelector(5))
          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render expenses interrupt page when it is not a prior submission" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              ExpensesCYAModel(ExpensesViewModel(isUsingCustomerData = false))))
            urlGet(fullUrl(startEmploymentExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedTitle)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, paragraphSelector(3))
          textOnPageCheck(expectedExample2, paragraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample3, paragraphSelector(5))
          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to check employment expenses page with it's not EOY" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlGet(fullUrl(startEmploymentExpensesUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYear)) shouldBe true
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

  ".submit" should {
    "redirect to the correct page" when {
      "it is not end of year" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          urlPost(fullUrl(startEmploymentExpensesUrl(taxYear)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYear)) shouldBe true
        }
      }

      "it is end of year, and nothing in DB" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          urlPost(fullUrl(startEmploymentExpensesUrl(taxYearEOY)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a INTERNAL_SERVER_ERROR ($INTERNAL_SERVER_ERROR) status" in {
          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }

      "it is end of year, and there is prior expenses" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
          urlPost(fullUrl(startEmploymentExpensesUrl(taxYearEOY)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }

      "it is end of year, and there is no prior expenses" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          userDataStub(anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcExpenses = None))), nino, taxYearEOY)
          urlPost(fullUrl(startEmploymentExpensesUrl(taxYearEOY)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(businessTravelExpensesUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }
}

