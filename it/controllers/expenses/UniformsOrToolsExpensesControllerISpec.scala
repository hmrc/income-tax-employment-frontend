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

import forms.YesNoForm
import models.mongo.ExpensesCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserData
import support.builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import support.builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import utils.PageUrls.{checkYourExpensesUrl, fullUrl, overviewUrl, uniformsAndToolsLink, uniformsWorkClothesToolsExpensesUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class UniformsOrToolsExpensesControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel) =
    anExpensesUserData.copy(isPriorSubmission = isPrior, hasPriorExpenses = hasPriorExpenses, expensesCya = expensesCyaModel)

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val canClaimParagraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p.govuk-body:nth-child(2)"
    val canClaimExample1Selector: String = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(1)"
    val canClaimExample2Selector: String = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(2)"
    val flatRateExpenseParagraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p.govuk-body:nth-child(4)"
    val uniformsAndToolsLinkSelector: String = "#uniforms-and-tools-link"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
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
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, ExpensesCYAModel(anExpensesViewModel.copy(flatRateJobExpensesQuestion = None))))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

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
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(uniformsWorkClothesToolsExpensesUrl(taxYearEOY), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'Do you want to claim for uniforms, work clothes, or tools?' page with the correct content with yes value pre-filled" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

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
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(uniformsWorkClothesToolsExpensesUrl(taxYearEOY), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'Do you want to claim for uniforms, work clothes, or tools?' page with the correct content with no value pre-filled" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val expensesViewModel = anExpensesViewModel.copy(flatRateJobExpensesQuestion = Some(false))
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel.copy(expenses = expensesViewModel)))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

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
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(uniformsWorkClothesToolsExpensesUrl(taxYearEOY), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "return a redirect when in year" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(anIncomeTaxUserData, nino, taxYear)
          urlGet(fullUrl(uniformsWorkClothesToolsExpensesUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        lazy val document = Jsoup.parse(result.body)

        implicit def documentSupplier: () => Document = () => document

        "has a url of overview page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "return a redirect when ExpensesUserData data is None" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        lazy val document = Jsoup.parse(result.body)

        implicit def documentSupplier: () => Document = () => document

        "has a url of overview page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
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
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

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
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(uniformsWorkClothesToolsExpensesUrl(taxYearEOY), continueButtonFormSelector)
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
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header(name = "location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get

          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe Some(false)
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe None
        }
      }

      "redirect to 'check your expenses', update flatRateJobExpensesQuestion to yes and preserve the flatRateJobExpenses amounts when the user chooses yes" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropExpensesDB()
          val expenses = anExpensesViewModel.copy(flatRateJobExpensesQuestion = None, flatRateJobExpenses = Some(10.00))
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel.copy(expenses = expenses)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get

          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe Some(10.00)
        }
      }

      "return a redirect when in year" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(anIncomeTaxUserData, nino, taxYear)
          urlPost(fullUrl(uniformsWorkClothesToolsExpensesUrl(taxYear)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        lazy val document = Jsoup.parse(result.body)

        implicit def documentSupplier: () => Document = () => document

        "has a url of overview page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect the user to the check employment expenses page when theres no session data for that user" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }
}
