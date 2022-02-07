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
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import builders.models.expenses.ExpensesBuilder.anExpenses
import builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserData
import builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import forms.AmountForm
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{businessTravelExpensesUrl, checkYourExpensesUrl, fullUrl, overviewUrl, travelAmountExpensesUrl, uniformsWorkClothesToolsExpensesUrl}

class TravelAndOvernightAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val newAmount = 25
  private val amountInputName = "amount"

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    anExpensesUserData.copy(isPriorSubmission = isPrior, hasPriorExpenses = hasPriorExpenses, expensesCya = expensesCyaModel)

  private def expensesViewModel(jobExpensesQuestion: Option[Boolean] = None): ExpensesViewModel =
    ExpensesViewModel(isUsingCustomerData = true, claimingEmploymentExpenses = true, jobExpensesQuestion = jobExpensesQuestion)
  
  object Selectors {
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val amountSelector = "#amount"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > label > p:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hintText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedDoNotClaim: String
    val expectedReplay: Int => String
    val expectedNoEntryError: String
    val expectedFormatError: String
    val expectedOverMaxError: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "Total amount for the year For example, £600 or £193.54"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "Total amount for the year For example, £600 or £193.54"
    val buttonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for business travel and overnight stays?"
    val expectedHeading = "How much do you want to claim for business travel and overnight stays?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedDoNotClaim = "Do not claim any amount your employer has paid you for."
    val expectedReplay: Int => String = amount =>
      s"You told us you want to claim £$amount for other business travel and overnight stays. Tell us if this has changed."
    val expectedNoEntryError = "Enter the amount you want to claim for business travel and overnight stays"
    val expectedFormatError = "Enter the amount you want to claim for business travel and overnight stays in the correct format"
    val expectedOverMaxError = "The amount you want to claim for business travel and overnight stays must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for business travel and overnight stays?"
    val expectedHeading = "How much do you want to claim for business travel and overnight stays?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim travel and overnight stays"
    val expectedDoNotClaim = "Do not claim any amount your employer has paid you for."
    val expectedReplay: Int => String = amount =>
      s"You told us you want to claim £$amount for other business travel and overnight stays. Tell us if this has changed."
    val expectedNoEntryError = "Enter the amount you want to claim for business travel and overnight stays"
    val expectedFormatError = "Enter the amount you want to claim for business travel and overnight stays in the correct format"
    val expectedOverMaxError = "The amount you want to claim for business travel and overnight stays must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for your client’s business travel and overnight stays?"
    val expectedHeading = "How much do you want to claim for your client’s business travel and overnight stays?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for your client’s travel and overnight stays"
    val expectedDoNotClaim = "Do not claim any amount your client’s employer has paid them for."
    val expectedReplay: Int => String = amount =>
      s"You told us you want to claim £$amount for your client’s other business travel and overnight stays. Tell us if this has changed."
    val expectedNoEntryError = "Enter the amount you want to claim for your client’s business travel and overnight stays"
    val expectedFormatError = "Enter the amount you want to claim for business travel and overnight stays for your client in the correct format"
    val expectedOverMaxError = "The amount you want to claim for your client’s business travel and overnight stays must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for your client’s business travel and overnight stays?"
    val expectedHeading = "How much do you want to claim for your client’s business travel and overnight stays?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for your client’s travel and overnight stays"
    val expectedDoNotClaim = "Do not claim any amount your client’s employer has paid them for."
    val expectedReplay: Int => String = amount =>
      s"You told us you want to claim £$amount for your client’s other business travel and overnight stays. Tell us if this has changed."
    val expectedNoEntryError = "Enter the amount you want to claim for your client’s business travel and overnight stays"
    val expectedFormatError = "Enter the amount you want to claim for business travel and overnight stays for your client in the correct format"
    val expectedOverMaxError = "The amount you want to claim for your client’s business travel and overnight stays must be less than £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "display the 'Business travel and Overnight stays Amount' page with correct content" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            val employmentData = anAllEmploymentData.copy(hmrcExpenses = Some(anEmploymentExpenses.copy(expenses = Some(anExpenses.copy(jobExpenses = None)))))
            userDataStub(anIncomeTaxUserData.copy(Some(employmentData)), nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel.copy(expensesViewModel(Some(true)))), aUserRequest)
            urlGet(fullUrl(travelAmountExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedDoNotClaim, paragraphSelector(2))
          hintTextCheck(hintText)
          inputFieldValueCheck(amountInputName, Selectors.amountSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(travelAmountExpensesUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "display the 'Business travel and Overnight stays Amount' page with pre-filled amount and replay content" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
              ExpensesCYAModel(ExpensesViewModel(isUsingCustomerData = true)).copy(expensesViewModel(Some(true)).copy(jobExpenses = Some(newAmount)))), aUserRequest)
            urlGet(fullUrl(travelAmountExpensesUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedReplay(newAmount), paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedDoNotClaim, paragraphSelector(3))
          hintTextCheck(hintText)
          inputFieldValueCheck(amountInputName, Selectors.amountSelector, newAmount.toString)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(travelAmountExpensesUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "the user has not answered 'yes' to the 'Business travel and Overnight Stays' question" should {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          anExpensesCYAModel.copy(expenses = anExpensesCYAModel.expenses.copy(jobExpensesQuestion = Some(false)))), aUserRequest)
        urlGet(fullUrl(travelAmountExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the CheckEmploymentExpenses page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "the user has no cya data in session" should {

      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(travelAmountExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the CheckEmploymentExpenses page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "the user is in year" should {
      lazy val result: WSResponse = {
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          anExpensesCYAModel), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(travelAmountExpensesUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" when {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return an error when the flatRateJobExpenses amount is in the wrong format" which {
          lazy val form: Map[String, String] = Map(AmountForm.amount -> "badThings")
          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, anExpensesCYAModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(travelAmountExpensesUrl(taxYearEOY)), form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedDoNotClaim, paragraphSelector(2))
          hintTextCheck(hintText)
          inputFieldValueCheck(amountInputName, Selectors.amountSelector, "badThings")
          buttonCheck(buttonText, continueButtonSelector)

          errorAboveElementCheck(user.specificExpectedResults.get.expectedFormatError, Some("amount"))
          errorSummaryCheck(user.specificExpectedResults.get.expectedFormatError, Selectors.amountSelector)
          formPostLinkCheck(travelAmountExpensesUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when no flatRateJobExpenses amount is submitted" which {

          lazy val form: Map[String, String] = Map(AmountForm.amount -> "")

          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, anExpensesCYAModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(travelAmountExpensesUrl(taxYearEOY)), form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedDoNotClaim, paragraphSelector(2))
          hintTextCheck(hintText)
          inputFieldValueCheck(amountInputName, Selectors.amountSelector, "")
          buttonCheck(buttonText, continueButtonSelector)

          errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryError, Some("amount"))
          errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryError, Selectors.amountSelector)
          formPostLinkCheck(travelAmountExpensesUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when the submitted flatRateJobExpenses amount is too great" which {

          lazy val form: Map[String, String] = Map(AmountForm.amount -> "100000000000")

          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, anExpensesCYAModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(travelAmountExpensesUrl(taxYearEOY)), form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedDoNotClaim, paragraphSelector(2))
          hintTextCheck(hintText)
          inputFieldValueCheck(amountInputName, Selectors.amountSelector, "100000000000")
          buttonCheck(buttonText, continueButtonSelector)

          errorAboveElementCheck(user.specificExpectedResults.get.expectedOverMaxError, Some("amount"))
          errorSummaryCheck(user.specificExpectedResults.get.expectedOverMaxError, Selectors.amountSelector)
          formPostLinkCheck(travelAmountExpensesUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "the user is in year" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")
      lazy val result: WSResponse = {
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, anExpensesCYAModel), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(travelAmountExpensesUrl(taxYear)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }

      "not update the CYA model" in {
        findExpensesCyaData(taxYearEOY, aUserRequest).get.expensesCya.expenses.jobExpenses shouldBe Some(BigDecimal(200))
      }
    }

    "there is no CYA data" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(travelAmountExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the CYA page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }

      "not update the CYA model" in {
        findExpensesCyaData(taxYearEOY, aUserRequest) shouldBe None
      }
    }

    "the user successfully submits a valid amount" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")
      lazy val result: WSResponse = {
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          anExpensesCYAModel.copy(expensesViewModel(Some(true)))), aUserRequest)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          anExpensesCYAModel.copy(anExpensesViewModel.copy(flatRateJobExpensesQuestion = None))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(travelAmountExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to Uniforms Work Clothes or Tools question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(uniformsWorkClothesToolsExpensesUrl(taxYearEOY)) shouldBe true
      }

      "update the CYA model" in {
        findExpensesCyaData(taxYearEOY, aUserRequest).get.expensesCya.expenses.jobExpenses shouldBe Some(newAmount)
      }
    }

    "jobExpensesQuestion is empty" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")

      lazy val result: WSResponse = {
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          ExpensesCYAModel(expensesViewModel(None))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(travelAmountExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to Business Travel and Overnight question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(businessTravelExpensesUrl(taxYearEOY)) shouldBe true
      }
    }
  }
}
