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
import builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserData
import builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import forms.YesNoForm
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{businessTravelExpensesUrl, checkYourExpensesUrl, fullUrl, overviewUrl, professionalFeesExpensesAmountUrl, professionalFeesExpensesUrl, professionalFeesLink}

class ProfessionalFeesAndSubscriptionsExpensesControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    anExpensesUserData.copy(isPriorSubmission = isPrior, hasPriorExpenses = hasPriorExpenses, expensesCya = expensesCyaModel)

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > p:nth-child($index)"

    def bulletListSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child($index)"

    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val professionFeesLinkSelector = "#professional-fees-link"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedParagraphText: String
    val yesText: String
    val noText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
    val expectedExample1: String
    val expectedExample2: String
    val checkIfYouCanClaim: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedParagraphText = "This includes things like:"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedParagraphText = "This includes things like:"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for professional fees and subscriptions?"
    val expectedHeading = "Do you want to claim for professional fees and subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for professional fees and subscriptions"
    val expectedExample1 = "professional membership fees, if you have to pay the fees to do your job"
    val expectedExample2 = "yearly subscriptions to approved professional bodies or learned societies relevant to your job"
    val checkIfYouCanClaim = "Check if you can claim for professional fees and subscriptions (opens in new tab)."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for professional fees and subscriptions?"
    val expectedHeading = "Do you want to claim for professional fees and subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for professional fees and subscriptions"
    val expectedExample1 = "professional membership fees, if you have to pay the fees to do your job"
    val expectedExample2 = "yearly subscriptions to approved professional bodies or learned societies relevant to your job"
    val checkIfYouCanClaim = "Check if you can claim for professional fees and subscriptions (opens in new tab)."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for professional fees and subscriptions for your client?"
    val expectedHeading = "Do you want to claim for professional fees and subscriptions for your client?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for your client’s professional fees and subscriptions"
    val expectedExample1 = "professional membership fees, if your client has to pay the fees to do their job"
    val expectedExample2 = "yearly subscriptions to approved professional bodies or learned societies relevant to your client’s job"
    val checkIfYouCanClaim = "Check if your client can claim for professional fees and subscriptions (opens in new tab)."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim for professional fees and subscriptions for your client?"
    val expectedHeading = "Do you want to claim for professional fees and subscriptions for your client?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for your client’s professional fees and subscriptions"
    val expectedExample1 = "professional membership fees, if your client has to pay the fees to do their job"
    val expectedExample2 = "yearly subscriptions to approved professional bodies or learned societies relevant to your client’s job"
    val checkIfYouCanClaim = "Check if your client can claim for professional fees and subscriptions (opens in new tab)."
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
        "render professional fees and subscriptions expenses question page with no pre-filled radio buttons" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              anExpensesCYAModel.copy(anExpensesViewModel.copy(professionalSubscriptionsQuestion = None))), aUserRequest)
            urlGet(fullUrl(professionalFeesExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, bulletListSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, bulletListSelector(2))
          linkCheck(user.specificExpectedResults.get.checkIfYouCanClaim, professionFeesLinkSelector, professionalFeesLink)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(professionalFeesExpensesUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render professional fees and subscriptions expenses question page with 'Yes' pre-filled and CYA data exists" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, anExpensesCYAModel), aUserRequest)
            urlGet(fullUrl(professionalFeesExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, bulletListSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, bulletListSelector(2))
          linkCheck(user.specificExpectedResults.get.checkIfYouCanClaim, professionFeesLinkSelector, professionalFeesLink)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(professionalFeesExpensesUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render professional fees and subscriptions expenses question page with 'No' pre-filled and not a prior submission" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              anExpensesCYAModel.copy(anExpensesViewModel.copy(professionalSubscriptionsQuestion = Some(false)))), aUserRequest)
            urlGet(fullUrl(professionalFeesExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, bulletListSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, bulletListSelector(2))
          linkCheck(user.specificExpectedResults.get.checkIfYouCanClaim, professionFeesLinkSelector, professionalFeesLink)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(professionalFeesExpensesUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

      }
    }

    "redirect to tax overview page with it's not EOY" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlGet(fullUrl(professionalFeesExpensesUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        urlGet(fullUrl(professionalFeesExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to jobExpenses Question page if jobExpenses is None and it's a new submission" which {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          anExpensesCYAModel.copy(ExpensesViewModel(claimingEmploymentExpenses = true, isUsingCustomerData = true))), aUserRequest)
        urlGet(fullUrl(professionalFeesExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(businessTravelExpensesUrl(taxYearEOY)) shouldBe true
      }

    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when form is submitted with no entry" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            dropExpensesDB()
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              anExpensesCYAModel.copy(anExpensesViewModel.copy(professionalSubscriptionsQuestion = None))), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(professionalFeesExpensesUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, bulletListSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, bulletListSelector(2))
          linkCheck(user.specificExpectedResults.get.checkIfYouCanClaim, professionFeesLinkSelector, professionalFeesLink)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(professionalFeesExpensesUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))
        }

      }
    }

    "redirect to Professional Subscriptions amount page when user selects 'yes' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          anExpensesCYAModel.copy(anExpensesViewModel.copy(professionalSubscriptions = None))), aUserRequest)
        urlPost(fullUrl(professionalFeesExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(professionalFeesExpensesAmountUrl(taxYearEOY)) shouldBe true
      }

      "updates professionalSubscriptionQuestion to Some(true)" in {
        lazy val cyaModel = findExpensesCyaData(taxYearEOY, aUserRequest).get
        cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
        cyaModel.expensesCya.expenses.professionalSubscriptionsQuestion shouldBe Some(true)
      }
    }

    "redirect to Check Employment Expenses page" when {
      "user selects no and it's a prior submission" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(anIncomeTaxUserData, nino, taxYear)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
          urlPost(fullUrl(professionalFeesExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }

        "update professionalSubscriptionQuestion to Some(false) and wipes jobExpenses amount" in {
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, aUserRequest).get

          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
          cyaModel.expensesCya.expenses.professionalSubscriptionsQuestion shouldBe Some(false)
          cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe None
        }
      }

      "user has no expenses" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(professionalFeesExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }
    }

    "redirect to tax overview page if it's not EOY" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlPost(fullUrl(professionalFeesExpensesUrl(taxYear)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }
}
