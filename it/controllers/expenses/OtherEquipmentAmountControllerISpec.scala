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

import controllers.expenses.routes._
import forms.AmountForm
import models.User
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class OtherEquipmentAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val poundPrefixText = "£"
  val newAmount: BigDecimal = 250
  val maxLimit: String = "100000000000"
  val amountField = "#amount"
  val amountFieldName = "amount"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    ExpensesUserData(sessionId, mtditid, nino, taxYear - 1, isPriorSubmission = isPrior, hasPriorExpenses, expensesCyaModel)

  private def employmentExpensesAmountPageUrl(taxYear: Int) = s"$appUrl/$taxYear/expenses/amount-for-other-equipment"

  val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/expenses/amount-for-other-equipment"

  object Selectors {
    val formSelector = "#main-content > div > div > form"
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val wantToClaimSelector: String = "#previous-amount"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {

    val expectedTitle: String
    val expectedHeading: String

    def expectedPreAmountParagraph(amount: BigDecimal): String

    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedInvalidFormatErrorMessage: String
    val expectedOverMaximumErrorMessage: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val hintText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for buying other equipment?"
    val expectedHeading = "How much do you want to claim for buying other equipment?"
    def expectedPreAmountParagraph(amount: BigDecimal): String = s"You told us you want to claim £$amount for buying other equipment. Tell us if this has changed."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you want to claim for buying other equipment"
    val expectedInvalidFormatErrorMessage = "Enter the amount you want to claim for buying other equipment in the correct format"
    val expectedOverMaximumErrorMessage = "The amount you want to claim for buying other equipment must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for buying other equipment?"
    val expectedHeading = "How much do you want to claim for buying other equipment?"
    def expectedPreAmountParagraph(amount: BigDecimal): String = s"You told us you want to claim £$amount for buying other equipment. Tell us if this has changed."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you want to claim for buying other equipment"
    val expectedInvalidFormatErrorMessage = "Enter the amount you want to claim for buying other equipment in the correct format"
    val expectedOverMaximumErrorMessage = "The amount you want to claim for buying other equipment must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for buying other equipment for your client?"
    val expectedHeading = "How much do you want to claim for buying other equipment for your client?"
    def expectedPreAmountParagraph(amount: BigDecimal): String = s"You told us you want to claim £$amount for buying other equipment for your client. Tell us if this has changed."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you want to claim for your client buying other equipment"
    val expectedInvalidFormatErrorMessage = "Enter the amount you want to claim for your client buying other equipment in the correct format"
    val expectedOverMaximumErrorMessage = "The amount you want to claim for your client buying other equipment must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much do you want to claim for buying other equipment for your client?"
    val expectedHeading = "How much do you want to claim for buying other equipment for your client?"
    def expectedPreAmountParagraph(amount: BigDecimal): String = s"You told us you want to claim £$amount for buying other equipment for your client. Tell us if this has changed."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount you want to claim for your client buying other equipment"
    val expectedInvalidFormatErrorMessage = "Enter the amount you want to claim for your client buying other equipment in the correct format"
    val expectedOverMaximumErrorMessage = "The amount you want to claim for your client buying other equipment must be less than £100,000,000,000"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment expenses for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £600 or £193.54"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment expenses for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £600 or £193.54"
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

        "render 'How much do you want to claim for buying other equipment?' page with the correct content and" +
          " no pre-filled amount when no user data" which {

          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses.copy(otherAndCapitalAllowances = None))))), nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
              fullExpensesCYAModel.copy(expenses = fullExpensesCYAModel.expenses.copy(otherAndCapitalAllowances = None))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(employmentExpensesAmountPageUrl(taxYearEOY), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          elementNotOnPageCheck(wantToClaimSelector)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountField, "")
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'How much do you want to claim for buying other equipment?' page with  pre-filled amount if it has changed" which {

          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              fullExpensesCYAModel.copy(expenses = fullExpensesCYAModel.expenses.copy(otherAndCapitalAllowances = Some(newAmount)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(employmentExpensesAmountPageUrl(taxYearEOY), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedPreAmountParagraph(newAmount), wantToClaimSelector)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountField, newAmount.toString())
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'How much do you want to claim for buying other equipment?' page with with no pre-filled amount if the amount value has not changed" which {

          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYearEOY)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              fullExpensesCYAModel), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(employmentExpensesAmountPageUrl(taxYearEOY), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedPreAmountParagraph(600), wantToClaimSelector)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountField, "")
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

      "Redirect user to the tax overview page when in year" which {

        lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYear)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
            fullExpensesCYAModel), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(employmentExpensesAmountPageUrl(taxYear), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }

      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(user.isAgent)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(employmentExpensesAmountPageUrl(taxYearEOY), user.isWelsh, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
        }
      }

      "redirect to the check your expenses page when there is a otherAndCapitalAllowances amount but the otherAndCapitalAllowancesQuestion is false" when {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
            fullExpensesCYAModel.copy(expenses = fullExpensesCYAModel.expenses.copy(otherAndCapitalAllowancesQuestion = Some(false)))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(employmentExpensesAmountPageUrl(taxYearEOY), user.isWelsh, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
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

        "return an error when the otherAndCapitalAllowances amount is in the wrong format" which {

          lazy val form: Map[String, String] = Map(AmountForm.amount -> "abc")

          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYear)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              fullExpensesCYAModel), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(employmentExpensesAmountPageUrl(taxYearEOY), form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))

          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedPreAmountParagraph(600), wantToClaimSelector)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountField, "abc")
          formPostLinkCheck(continueLink, formSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedInvalidFormatErrorMessage, Some(amountFieldName))
          errorSummaryCheck(user.specificExpectedResults.get.expectedInvalidFormatErrorMessage, amountField)
          welshToggleCheck(user.isWelsh)

        }

        "return an error when no otherAndCapitalAllowances amount is submitted" which {

          lazy val form: Map[String, String] = Map(AmountForm.amount -> "")

          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYear)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              fullExpensesCYAModel), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(employmentExpensesAmountPageUrl(taxYearEOY), form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedPreAmountParagraph(600), wantToClaimSelector)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountField, "")
          formPostLinkCheck(continueLink, formSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, Some(amountFieldName))
          errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, amountField)
          welshToggleCheck(user.isWelsh)

        }

        "return an error when no otherAndCapitalAllowances amount larger than maximum is submitted" which {

          lazy val form: Map[String, String] = Map(AmountForm.amount -> maxLimit)

          lazy val result: WSResponse = {
            dropExpensesDB()
            userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYear)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              fullExpensesCYAModel), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(employmentExpensesAmountPageUrl(taxYearEOY), form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedPreAmountParagraph(600), wantToClaimSelector)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountField, maxLimit)
          formPostLinkCheck(continueLink, formSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedOverMaximumErrorMessage, Some(amountFieldName))
          errorSummaryCheck(user.specificExpectedResults.get.expectedOverMaximumErrorMessage, amountField)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when valid request is made and then" should {

      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

      "redirect to next page and update otherAndCapitalAllowances to the new amount when not in year and not a prior submission" which {

        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

        lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
            fullExpensesCYAModel), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(employmentExpensesAmountPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, userRequest).get

          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
          cyaModel.expensesCya.expenses.jobExpensesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.otherAndCapitalAllowancesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.businessTravelCosts shouldBe Some(100.00)
          cyaModel.expensesCya.expenses.jobExpenses shouldBe Some(200.00)
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe Some(300.00)
          cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe Some(400.00)
          cyaModel.expensesCya.expenses.hotelAndMealExpenses shouldBe Some(500.00)
          cyaModel.expensesCya.expenses.otherAndCapitalAllowances shouldBe Some(newAmount)
          cyaModel.expensesCya.expenses.vehicleExpenses shouldBe Some(700.00)
          cyaModel.expensesCya.expenses.mileageAllowanceRelief shouldBe Some(800.00)
        }
      }

      "redirect to otherAndCapitalAllowances question page if otherAndCapitalAllowancesQuestion is empty" when {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
            fullExpensesCYAModel.copy(expenses = fullExpensesCYAModel.expenses.copy(otherAndCapitalAllowancesQuestion = None))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(employmentExpensesAmountPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(OtherEquipmentController.show(taxYearEOY).url)
        }
      }

      "redirect to 'check your expenses' page when a prior submission and update otherAndCapitalAllowances to the new amount" which {

        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

        lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
            fullExpensesCYAModel), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(employmentExpensesAmountPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER

          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, userRequest).get

          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
          cyaModel.expensesCya.expenses.jobExpensesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.flatRateJobExpensesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.otherAndCapitalAllowancesQuestion shouldBe Some(true)
          cyaModel.expensesCya.expenses.businessTravelCosts shouldBe Some(100.00)
          cyaModel.expensesCya.expenses.jobExpenses shouldBe Some(200.00)
          cyaModel.expensesCya.expenses.flatRateJobExpenses shouldBe Some(300.00)
          cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe Some(400.00)
          cyaModel.expensesCya.expenses.hotelAndMealExpenses shouldBe Some(500.00)
          cyaModel.expensesCya.expenses.otherAndCapitalAllowances shouldBe Some(newAmount)
          cyaModel.expensesCya.expenses.vehicleExpenses shouldBe Some(700.00)
          cyaModel.expensesCya.expenses.mileageAllowanceRelief shouldBe Some(800.00)
        }

      }

      "Redirect user to the tax overview page when in year" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

        lazy val result: WSResponse = {
          dropExpensesDB()
          userDataStub(userData(fullEmploymentsModel(hmrcExpenses = Some(employmentExpenses(fullExpenses)))), nino, taxYear)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
            fullExpensesCYAModel), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(employmentExpensesAmountPageUrl(taxYear), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }

      "Redirect user to the check your expenses page with no cya data in session" which {

        lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(user.isAgent)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(employmentExpensesAmountPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
        }
      }
    }


  }


}
