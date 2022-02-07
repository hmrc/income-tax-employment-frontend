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
import utils.PageUrls.{checkYourExpensesUrl, fullUrl, otherEquipmentExpensesUrl, overviewUrl, professionalFeesExpensesAmountUrl, professionalFeesExpensesUrl}

class ProfFeesAndSubscriptionsExpensesAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val amount: BigDecimal = 400
  private val newAmount: BigDecimal = 100
  private val amountFieldName = "amount"
  private val expectedErrorHref = "#amount"
  private val poundPrefixText = "£"
  private val maxLimit: String = "100,000,000,000"

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    anExpensesUserData.copy(isPriorSubmission = isPrior, hasPriorExpenses = hasPriorExpenses, expensesCya = expensesCyaModel)

  private def expensesViewModel(profFeesAndSubscriptions: Option[BigDecimal] = None): ExpensesViewModel =
    anExpensesViewModel.copy(professionalSubscriptions = profFeesAndSubscriptions, otherAndCapitalAllowancesQuestion = None, otherAndCapitalAllowances = None)

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val replayTextSelector = "#main-content > div > div > form > div > label > p"
    val hintTextSelector = "#amount-hint"
    val amountFieldSelector = "#amount"
    val poundPrefixSelector = ".govuk-input__prefix"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedHintText: String
    val currencyPrefix: String
    val continueButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val expectedErrorIncorrectFormat: String
    val expectedErrorOverMaximum: String

    def expectedReplayText(amount: BigDecimal): String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for professional fees and subscriptions?"
    val expectedHeading = "How much do you want to claim for professional fees and subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount you want to claim for professional fees and subscriptions"
    val expectedErrorIncorrectFormat = "Enter the amount you want to claim for professional fees and subscriptions in the correct format"
    val expectedErrorOverMaximum = "The amount you want to claim for professional fees and subscriptions must be less than £100,000,000,000"

    def expectedReplayText(amount: BigDecimal): String = s"You told us you want to claim £$amount for professional fees and subscriptions. Tell us if this has changed."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for professional fees and subscriptions?"
    val expectedHeading = "How much do you want to claim for professional fees and subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount you want to claim for professional fees and subscriptions"
    val expectedErrorIncorrectFormat = "Enter the amount you want to claim for professional fees and subscriptions in the correct format"
    val expectedErrorOverMaximum = "The amount you want to claim for professional fees and subscriptions must be less than £100,000,000,000"

    def expectedReplayText(amount: BigDecimal): String = s"You told us you want to claim £$amount for professional fees and subscriptions. Tell us if this has changed."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for professional fees and subscriptions for your client?"
    val expectedHeading = "How much do you want to claim for professional fees and subscriptions for your client?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount you want to claim for your client’s professional fees and subscriptions"
    val expectedErrorIncorrectFormat = "Enter the amount you want to claim for your client’s professional fees and subscriptions in the correct format"
    val expectedErrorOverMaximum = "The amount you want to claim for your client’s professional fees and subscriptions must be less than £100,000,000,000"

    def expectedReplayText(amount: BigDecimal): String = s"You told us you want to claim £$amount for your client’s professional fees and subscriptions. Tell us if this has changed."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for professional fees and subscriptions for your client?"
    val expectedHeading = "How much do you want to claim for professional fees and subscriptions for your client?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount you want to claim for your client’s professional fees and subscriptions"
    val expectedErrorIncorrectFormat = "Enter the amount you want to claim for your client’s professional fees and subscriptions in the correct format"
    val expectedErrorOverMaximum = "The amount you want to claim for your client’s professional fees and subscriptions must be less than £100,000,000,000"

    def expectedReplayText(amount: BigDecimal): String = s"You told us you want to claim £$amount for your client’s professional fees and subscriptions. Tell us if this has changed."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the professional fees and subscriptions expenses amount page with an empty amount field" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropExpensesDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              ExpensesCYAModel(expensesViewModel(profFeesAndSubscriptions = None))), aUserRequest)
            urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          elementNotOnPageCheck(replayTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, "")
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(professionalFeesExpensesAmountUrl(taxYearEOY), formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the professional fees and subscriptions expenses amount page with pre-filled cya data" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropExpensesDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
              anExpensesCYAModel.copy(expensesViewModel(Some(newAmount)))), aUserRequest)
            urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedReplayText(newAmount), replayTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, newAmount.toString())
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(professionalFeesExpensesAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the professional fees and subscriptions expenses amount page with pre-filled data from prior submission" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropExpensesDB()
            val allEmploymentData = anAllEmploymentData.copy(hmrcExpenses = None)
            userDataStub(anIncomeTaxUserData.copy(Some(allEmploymentData)), nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
            urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, amount.toString())
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(professionalFeesExpensesAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to tax overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
        urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      s"has an SEE OTHER($SEE_OTHER) status" in {

        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to check employment expenses page" when {

      "there is no expenses cya data" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has an SEE OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }

      "professionSubscriptionsQuestion is set to Some(false)" which {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropExpensesDB()
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
            anExpensesCYAModel.copy(expensesViewModel(profFeesAndSubscriptions = None).copy(professionalSubscriptionsQuestion = Some(false)))), aUserRequest)
          urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }
        s"has an SEE OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }
    }

  }


  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error" when {
          "the form is submitted with no entry" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropExpensesDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
              urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), body = "", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(user.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldValueCheck(amountFieldName, amountFieldSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(professionalFeesExpensesAmountUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Some(amountFieldName))
          }

          "the form is submitted with an incorrect format" which {
            val form: Map[String, String] = Map(AmountForm.amount -> "abc")

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropExpensesDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
              urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(user.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldValueCheck(amountFieldName, amountFieldSelector, "abc")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(professionalFeesExpensesAmountUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountFieldName))
          }

          "the form is submitted with an amount over the maximum limit" which {
            val form: Map[String, String] = Map(AmountForm.amount -> maxLimit)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropExpensesDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
              urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(user.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldValueCheck(amountFieldName, amountFieldSelector, maxLimit)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(professionalFeesExpensesAmountUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, Some(amountFieldName))
          }
        }
      }
    }
    "redirect to tax overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel), aUserRequest)
        urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYear)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      s"has an SEE OTHER($SEE_OTHER) status" in {

        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to Check Employment Expenses page when there is no cya data" which {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to Professional Subscriptions Question page if professionalSubscriptionsQuestion is None" which {
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          anExpensesCYAModel.copy(expenses = anExpensesViewModel.copy(professionalSubscriptionsQuestion = None))), aUserRequest)
        urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(professionalFeesExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to next page when valid form is submitted" which {
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, ExpensesCYAModel(expensesViewModel())), aUserRequest)
        urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(otherEquipmentExpensesUrl(taxYearEOY)) shouldBe true
      }

      "updates professionalSubscriptions to the new value" in {
        lazy val cyaModel = findExpensesCyaData(taxYearEOY, aUserRequest).get
        cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe Some(newAmount)
      }
    }
  }
}
