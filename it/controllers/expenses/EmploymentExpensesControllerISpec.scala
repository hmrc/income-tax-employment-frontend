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
import builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import forms.YesNoForm
import models.expenses.{Expenses, ExpensesViewModel}
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{checkYourExpensesUrl, claimEmploymentExpensesUrl, fullUrl, overviewUrl, startEmploymentExpensesUrl, taxReliefLink}

class EmploymentExpensesControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    ExpensesUserData(sessionId, mtditid, nino, taxYear - 1, isPriorSubmission = isPrior, hasPriorExpenses, expensesCyaModel)

  def cyaModel(isUsingCustomerData: Boolean, expenses: Expenses): ExpensesCYAModel =
    ExpensesCYAModel.makeModel(expenses, isUsingCustomerData, submittedOn = None)

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val canClaimParagraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(2)"
    val thisIncludesSelector: String = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(3)"
    val thisIncludesExample1Selector: String = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(1)"
    val thisIncludesExample2Selector: String = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(2)"
    val thisIncludesExample3Selector: String = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(3)"
    val expensesLinkSelector: String = "#expenses-link"
    val findOutMoreParagraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(5)"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedCanClaim: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedButtonText: String
    val expectedThisIncludes: String
    val expectedThisIncludesExample1: String
    val expectedThisIncludesExample2: String
    val expectedThisIncludesExample3: String
    val expectedFindOutMore: String
    val expectedFindOutMoreLink: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim employment expenses?"
    val expectedHeading = "Do you want to claim employment expenses?"
    val expectedCanClaim = "You can claim employment expenses you did not claim through your employer."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to claim employment expenses"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim employment expenses?"
    val expectedHeading = "Do you want to claim employment expenses?"
    val expectedCanClaim = "You can claim employment expenses you did not claim through your employer."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to claim employment expenses"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim employment expenses for your client?"
    val expectedHeading = "Do you want to claim employment expenses for your client?"
    val expectedCanClaim = "You can claim employment expenses your client did not claim through their employer."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to claim for your client’s employment expenses"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim employment expenses for your client?"
    val expectedHeading = "Do you want to claim employment expenses for your client?"
    val expectedCanClaim = "You can claim employment expenses your client did not claim through their employer."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to claim for your client’s employment expenses"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment expenses for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedThisIncludes = "Employment expenses include things like:"
    val expectedThisIncludesExample1 = "business travel and hotels and meals"
    val expectedThisIncludesExample2 = "professional fees and subscriptions"
    val expectedThisIncludesExample3 = "uniforms, work clothes and tools"
    val expectedFindOutMore = "Find out more about claiming employment expenses (opens in new tab)."
    val expectedFindOutMoreLink = "claiming employment expenses (opens in new tab)."
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment expenses for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedThisIncludes = "Employment expenses include things like:"
    val expectedThisIncludesExample1 = "business travel and hotels and meals"
    val expectedThisIncludesExample2 = "professional fees and subscriptions"
    val expectedThisIncludesExample3 = "uniforms, work clothes and tools"
    val expectedFindOutMore = "Find out more about claiming employment expenses (opens in new tab)."
    val expectedFindOutMoreLink = "claiming employment expenses (opens in new tab)."
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
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render 'Do you want to claim employment expenses?' page with the correct content and no values pre-filled when no user data" which {

          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              ExpensesCYAModel(ExpensesViewModel(isUsingCustomerData = false))), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(claimEmploymentExpensesUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          textOnPageCheck(user.specificExpectedResults.get.expectedCanClaim, canClaimParagraphSelector)
          textOnPageCheck(expectedThisIncludes, thisIncludesSelector)
          textOnPageCheck(expectedThisIncludesExample1, thisIncludesExample1Selector)
          textOnPageCheck(expectedThisIncludesExample2, thisIncludesExample2Selector)
          textOnPageCheck(expectedThisIncludesExample3, thisIncludesExample3Selector)
          textOnPageCheck(expectedFindOutMore, findOutMoreParagraphSelector)
          linkCheck(expectedFindOutMoreLink, expensesLinkSelector, taxReliefLink)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(claimEmploymentExpensesUrl(taxYearEOY), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'Do you want to claim employment expenses?' page with the correct content with yes value pre-filled" which {

          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(claimEmploymentExpensesUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          textOnPageCheck(user.specificExpectedResults.get.expectedCanClaim, canClaimParagraphSelector)
          textOnPageCheck(expectedThisIncludes, thisIncludesSelector)
          textOnPageCheck(expectedThisIncludesExample1, thisIncludesExample1Selector)
          textOnPageCheck(expectedThisIncludesExample2, thisIncludesExample2Selector)
          textOnPageCheck(expectedThisIncludesExample3, thisIncludesExample3Selector)
          textOnPageCheck(expectedFindOutMore, findOutMoreParagraphSelector)
          linkCheck(expectedFindOutMoreLink, expensesLinkSelector, taxReliefLink)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(claimEmploymentExpensesUrl(taxYearEOY), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'Do you want to claim employment expenses?' page with the correct content with no value pre-filled" which {

          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
              anExpensesCYAModel.copy(expenses = anExpensesViewModel.copy(claimingEmploymentExpenses = false))), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(claimEmploymentExpensesUrl(taxYearEOY)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          textOnPageCheck(user.specificExpectedResults.get.expectedCanClaim, canClaimParagraphSelector)
          textOnPageCheck(expectedThisIncludes, thisIncludesSelector)
          textOnPageCheck(expectedThisIncludesExample1, thisIncludesExample1Selector)
          textOnPageCheck(expectedThisIncludesExample2, thisIncludesExample2Selector)
          textOnPageCheck(expectedThisIncludesExample3, thisIncludesExample3Selector)
          textOnPageCheck(expectedFindOutMore, findOutMoreParagraphSelector)
          linkCheck(expectedFindOutMoreLink, expensesLinkSelector, taxReliefLink)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(claimEmploymentExpensesUrl(taxYearEOY), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {

      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

      "return a redirect when in year" which {

        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)

          userDataStub(anIncomeTaxUserData, nino, taxYear)
          urlGet(fullUrl(claimEmploymentExpensesUrl(taxYear)), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has a url of overview page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
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
              insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(claimEmploymentExpensesUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
            textOnPageCheck(user.specificExpectedResults.get.expectedCanClaim, canClaimParagraphSelector)
            textOnPageCheck(expectedThisIncludes, thisIncludesSelector)
            textOnPageCheck(expectedThisIncludesExample1, thisIncludesExample1Selector)
            textOnPageCheck(expectedThisIncludesExample2, thisIncludesExample2Selector)
            textOnPageCheck(expectedThisIncludesExample3, thisIncludesExample3Selector)
            textOnPageCheck(expectedFindOutMore, findOutMoreParagraphSelector)
            linkCheck(expectedFindOutMoreLink, expensesLinkSelector, taxReliefLink)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(claimEmploymentExpensesUrl(taxYearEOY), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText, Some("value"))
          }
        }
      }
    }

    "redirect to another page when a valid request is made and then" should {

      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedAgentEN))

      "redirect to 'check your expenses', update claimingEmploymentExpenses to no and wipe the expenses amounts when the user chooses no" which {

        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropExpensesDB()
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(fullUrl(claimEmploymentExpensesUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, aUserRequest).get

          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe false
          cyaModel.expensesCya.expenses.jobExpensesQuestion shouldBe None
          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe None
          cyaModel.expensesCya.expenses.otherAndCapitalAllowancesQuestion shouldBe None
          cyaModel.expensesCya.expenses.businessTravelCosts shouldBe None
          cyaModel.expensesCya.expenses.jobExpenses shouldBe None
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe None
          cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe None
          cyaModel.expensesCya.expenses.hotelAndMealExpenses shouldBe None
          cyaModel.expensesCya.expenses.otherAndCapitalAllowances shouldBe None
          cyaModel.expensesCya.expenses.vehicleExpenses shouldBe None
          cyaModel.expensesCya.expenses.mileageAllowanceRelief shouldBe None
        }

      }

      "redirect to 'expenses interrupt' page and update claimingEmploymentExpenses to yes when the user chooses yes" which {

        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(user.isAgent)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
            ExpensesCYAModel(ExpensesViewModel(isUsingCustomerData = false))), aUserRequest)
          urlPost(fullUrl(claimEmploymentExpensesUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the 'expenses interrupt' page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(startEmploymentExpensesUrl(taxYearEOY)) shouldBe true

          lazy val cyaModel = findExpensesCyaData(taxYearEOY, aUserRequest).get
          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
          cyaModel.expensesCya.expenses.jobExpensesQuestion shouldBe None
          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe None
          cyaModel.expensesCya.expenses.otherAndCapitalAllowancesQuestion shouldBe None
          cyaModel.expensesCya.expenses.businessTravelCosts shouldBe None
          cyaModel.expensesCya.expenses.jobExpenses shouldBe None
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe None
          cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe None
          cyaModel.expensesCya.expenses.hotelAndMealExpenses shouldBe None
          cyaModel.expensesCya.expenses.otherAndCapitalAllowances shouldBe None
          cyaModel.expensesCya.expenses.vehicleExpenses shouldBe None
          cyaModel.expensesCya.expenses.mileageAllowanceRelief shouldBe None
        }
      }

      "return a redirect when in year" which {

        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(anIncomeTaxUserData, nino, taxYear)
          urlPost(fullUrl(claimEmploymentExpensesUrl(taxYear)), body = "", user.isWelsh, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has a url of overview page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }
    }
  }
}
