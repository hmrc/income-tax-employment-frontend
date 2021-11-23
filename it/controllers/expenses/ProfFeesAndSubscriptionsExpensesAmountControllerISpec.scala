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
import controllers.employment.routes.CheckEmploymentExpensesController
import forms.AmountForm
import models.User
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ProfFeesAndSubscriptionsExpensesAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val amount: BigDecimal = 400
  val newAmount: BigDecimal = 100
  val amountFieldName = "amount"
  val expectedErrorHref = "#amount"
  val poundPrefixText = "£"
  val maxLimit: String = "100,000,000,000"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    ExpensesUserData(sessionId, mtditid, nino, taxYear - 1, isPriorSubmission = isPrior, hasPriorExpenses, expensesCyaModel)

  def expensesViewModel(profFeesAndSubscriptions: Option[BigDecimal] = None): ExpensesViewModel =
    ExpensesViewModel(isUsingCustomerData = true, claimingEmploymentExpenses = true, professionalSubscriptions = profFeesAndSubscriptions)

  private def pageUrl(taxYear: Int) = s"$appUrl/$taxYear/expenses/amount-for-professional-fees-and-subscriptions"

  private val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/expenses/amount-for-professional-fees-and-subscriptions"


  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > label > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val replayTextSelector = "#main-content > div > div > form > div > label > p"
    val hintTextSelector = "#amount-hint"
    val currencyPrefixSelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
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
    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHintText = "For example, £600 or £193.54"
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
            userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(expenses.copy(None))))), nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              emptyExpensesCYAModel.copy(expensesViewModel(profFeesAndSubscriptions = None))), userRequest)
            urlGet(pageUrl(taxYearEOY), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          elementNotOnPageCheck(replayTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldCheck(amountFieldName, amountFieldSelector)
          inputFieldValueCheck("", amountFieldSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the professional fees and subscriptions expenses amount page with pre-filled cya data" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropExpensesDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
              fullExpensesCYAModel.copy(expensesViewModel(Some(newAmount)))), userRequest)
            urlGet(pageUrl(taxYearEOY), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedReplayText(newAmount), replayTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldCheck(amountFieldName, amountFieldSelector)
          inputFieldValueCheck(newAmount.toString, amountFieldSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the professional fees and subscriptions expenses amount page with pre-filled data from prior submission" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropExpensesDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
              fullExpensesCYAModel), userRequest)
            urlGet(pageUrl(taxYearEOY), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldCheck(amountFieldName, amountFieldSelector)
          inputFieldValueCheck(amount.toString, amountFieldSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to tax overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          fullExpensesCYAModel), userRequest)
        urlGet(pageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      s"has an SEE OTHER($SEE_OTHER) status" in {

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect to check employment expenses page" when {

      "there is no expenses cya data" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has an SEE OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
        }
      }

      "professionSubscriptionsQuestion is set to Some(false)" which {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropExpensesDB()
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
            fullExpensesCYAModel.copy(expensesViewModel(profFeesAndSubscriptions = None).copy(professionalSubscriptionsQuestion = Some(false)))), userRequest)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }
        s"has an SEE OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
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
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
                fullExpensesCYAModel), userRequest)
              urlPost(pageUrl(taxYearEOY), body = "", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck("", amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, formSelector)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Some(amountFieldName))
          }

          "the form is submitted with an incorrect format" which {
            val form: Map[String, String] = Map(AmountForm.amount -> "abc")

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropExpensesDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
                fullExpensesCYAModel), userRequest)
              urlPost(pageUrl(taxYearEOY), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck("abc", amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, formSelector)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountFieldName))
          }

          "the form is submitted with an amount over the maximum limit" which {
            val form: Map[String, String] = Map(AmountForm.amount -> maxLimit)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropExpensesDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
                fullExpensesCYAModel), userRequest)
              urlPost(pageUrl(taxYearEOY), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedReplayText(amount), replayTextSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(maxLimit, amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, formSelector)
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
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          fullExpensesCYAModel), userRequest)
        urlPost(pageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      s"has an SEE OTHER($SEE_OTHER) status" in {

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect to Check Employment Expenses page" when {

      "valid form is submitted" which {
        val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString)

        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropExpensesDB()
          userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
            fullExpensesCYAModel.copy(expensesViewModel(Some(newAmount)))), userRequest)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
        }

        "updates professionalSubscriptions to the new value" in {
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, userRequest).get
          cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe Some(newAmount)
        }
      }

      "there is no cya data" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropExpensesDB()
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
        }
      }
    }
  }



}