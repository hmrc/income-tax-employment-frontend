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

package controllers.expenses

import controllers.expenses.routes.CheckEmploymentExpensesController
import forms.YesNoForm
import models.User
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class UniformsOrToolsExpensesControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel) =
    ExpensesUserData(sessionId, mtditid, nino, taxYear - 1, isPriorSubmission = isPrior, hasPriorExpenses, expensesCyaModel)

  private def pageUrl(taxYear: Int) = s"$appUrl/$taxYear/expenses/uniforms-work-clothes-or-tools"

  private val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/expenses/uniforms-work-clothes-or-tools"
  private val uniformsAndToolsLink = "https://www.gov.uk/guidance/job-expenses-for-uniforms-work-clothing-and-tools"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val canClaimParagraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p.govuk-body:nth-child(2)"
    val canClaimExample1Selector: String = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(1)"
    val canClaimExample2Selector: String = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(2)"
    val flatRateExpenseParagraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p.govuk-body:nth-child(4)"
    val uniformsAndToolsLinkSelector: String = "#uniforms-and-tools-link"
    val findOutMoreParagraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(5)"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedCanClaimExample1: String
    val expectedUniformsAndToolsLink: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedCanClaim: String
    val expectedButtonText: String
    val expectedCanClaimExample2: String
    val flatRateExpense: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for uniforms, work clothes, or tools?"
    val expectedHeading = "Do you want to claim for uniforms, work clothes, or tools?"
    val expectedCanClaimExample1 = "repairing or replacing small tools you need to do your job"
    val expectedUniformsAndToolsLink = "Check if you can claim flat rate expenses for uniforms, work clothes, or tools (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes to claim for uniforms, work clothes, or tools"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for uniforms, work clothes, or tools?"
    val expectedHeading = "Do you want to claim for uniforms, work clothes, or tools?"
    val expectedCanClaimExample1 = "repairing or replacing small tools you need to do your job"
    val expectedUniformsAndToolsLink = "Check if you can claim flat rate expenses for uniforms, work clothes, or tools (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes to claim for uniforms, work clothes, or tools"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for uniforms, work clothes, or tools for your client?"
    val expectedHeading = "Do you want to claim for uniforms, work clothes, or tools for your client?"
    val expectedCanClaimExample1 = "repairing or replacing small tools your client needs to do their job"
    val expectedUniformsAndToolsLink = "Check if your client can claim flat rate expenses for uniforms, work clothes, or tools (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes to claim for your client’s uniforms, work clothes, or tools"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for uniforms, work clothes, or tools for your client?"
    val expectedHeading = "Do you want to claim for uniforms, work clothes, or tools for your client?"
    val expectedCanClaimExample1 = "repairing or replacing small tools your client needs to do their job"
    val expectedUniformsAndToolsLink = "Check if your client can claim flat rate expenses for uniforms, work clothes, or tools (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes to claim for your client’s uniforms, work clothes, or tools"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment expenses for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedCanClaim = "You might be able to claim for the cost of:"
    val expectedCanClaimExample2 = "cleaning, repairing or replacing uniforms or specialist work clothes"
    val flatRateExpense = "These expenses are paid at an agreed rate (a ‘flat rate expense’ or ‘fixed deduction’)."
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment expenses for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedCanClaim = "You might be able to claim for the cost of:"
    val expectedCanClaimExample2 = "cleaning, repairing or replacing uniforms or specialist work clothes"
    val flatRateExpense = "These expenses are paid at an agreed rate (a ‘flat rate expense’ or ‘fixed deduction’)."
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render 'Do you want to claim for uniforms, work clothes, or tools?' page with the correct content and no values pre-filled when no user data" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, emptyExpensesCYAModel), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedCanClaim, canClaimParagraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedCanClaimExample1, canClaimExample1Selector)
          textOnPageCheck(expectedCanClaimExample2, canClaimExample2Selector)
          textOnPageCheck(flatRateExpense, flatRateExpenseParagraphSelector)
          linkCheck(user.specificExpectedResults.get.expectedUniformsAndToolsLink, uniformsAndToolsLinkSelector, uniformsAndToolsLink)
          radioButtonCheck(yesText, 1, None)
          radioButtonCheck(noText, 2, None)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'Do you want to claim for uniforms, work clothes, or tools?' page with the correct content with yes value pre-filled" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYear - 1)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, fullExpensesCYAModel), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedCanClaim, canClaimParagraphSelector)
          textOnPageCheck(flatRateExpense, flatRateExpenseParagraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedCanClaimExample1, canClaimExample1Selector)
          textOnPageCheck(expectedCanClaimExample2, canClaimExample2Selector)
          linkCheck(user.specificExpectedResults.get.expectedUniformsAndToolsLink, uniformsAndToolsLinkSelector, uniformsAndToolsLink)
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'Do you want to claim for uniforms, work clothes, or tools?' page with the correct content with no value pre-filled" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYearEOY)
            val model = fullExpensesCYAModel.expenses.copy(flatRateJobExpensesQuestion = Some(false))
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, fullExpensesCYAModel.copy(expenses = model)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedCanClaim, canClaimParagraphSelector)
          textOnPageCheck(flatRateExpense, flatRateExpenseParagraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedCanClaimExample1, canClaimExample1Selector)
          textOnPageCheck(expectedCanClaimExample2, canClaimExample2Selector)
          linkCheck(user.specificExpectedResults.get.expectedUniformsAndToolsLink, uniformsAndToolsLinkSelector, uniformsAndToolsLink)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "return a redirect when in year" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
          urlGet(pageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has a url of overview page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }

      "return a redirect when ExpensesUserData data is None" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has a url of overview page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
        }
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        s"return a BAD_REQUEST($BAD_REQUEST) status" when {
          "the value is empty" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

            lazy val result: WSResponse = {
              dropExpensesDB()
              userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYearEOY)
              insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, fullExpensesCYAModel), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(pageUrl(taxYearEOY), body = form, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedCanClaim, canClaimParagraphSelector)
            textOnPageCheck(flatRateExpense, flatRateExpenseParagraphSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedCanClaimExample1, canClaimExample1Selector)
            textOnPageCheck(expectedCanClaimExample2, canClaimExample2Selector)
            linkCheck(user.specificExpectedResults.get.expectedUniformsAndToolsLink, uniformsAndToolsLinkSelector, uniformsAndToolsLink)
            radioButtonCheck(yesText, 1, Some(false))
            radioButtonCheck(noText, 2, Some(false))
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText, Some("value"))
          }
        }
      }
    }

    "redirect to another page when a valid request is made and then" should {
      "redirect to 'check your expenses', update flatRateJobExpensesQuestion to no and wipe the flatRateJobExpenses amounts when the user chooses no" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropExpensesDB()
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, fullExpensesCYAModel), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header(name = "location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, userRequest).get

          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe Some(false)
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe None
        }
      }

      "redirect to 'check your expenses', update flatRateJobExpensesQuestion to yes and preserve the flatRateJobExpenses amounts when the user chooses yes" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropExpensesDB()
          val expenses = fullExpensesCYAModel.expenses.copy(flatRateJobExpensesQuestion = None, flatRateJobExpenses = Some(10.00))
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, fullExpensesCYAModel.copy(expenses = expenses)), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, userRequest).get

          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe Some(10.00)
        }
      }

      "return a redirect when in year" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
          urlPost(pageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has a url of overview page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }

      "redirect the user to the check employment expenses page when theres no session data for that user" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
        }
      }
    }
  }
}
