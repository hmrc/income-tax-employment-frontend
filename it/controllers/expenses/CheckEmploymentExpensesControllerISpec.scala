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


import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import controllers.employment.routes.EmploymentSummaryController
import builders.models.UserBuilder.aUserRequest
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.expenses.ExpensesBuilder.anExpenses
import builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserData
import builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import controllers.expenses.routes._
import models.IncomeTaxUserData
import models.employment.AllEmploymentData
import models.expenses.Expenses
import models.expenses.createUpdate.CreateUpdateExpensesRequest
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CheckEmploymentExpensesControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper {

  private def url(taxYearToUse: Int = taxYear): String = s"$appUrl/$taxYearToUse/expenses/check-employment-expenses"

  object Selectors {
    val headingSelector = "#main-content > div > div > header > h1"
    val subHeadingSelector = "#main-content > div > div > header > p"
    val contentSelector = "#main-content > div > div > p.govuk-body"
    val insetEOYSelector = "#main-content > div > div > p.govuk-inset-text"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val summaryListSelector = "#main-content > div > div > dl"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"

    def summaryListRowFieldNameSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dt"

    def summaryListRowFieldAmountSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__value"

    def changeLinkSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd > a"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedContentSingle: String
    val expectedContentMultiple: String
    val expectedInsetMultiple: String
    val hmrcOnlyInfo: String
    val expensesHiddenText: String
    val jobExpensesHiddenText: String
    val jobExpensesAmountHiddenText: String
    val flatRateHiddenText: String
    val flatRateAmountHiddenText: String
    val profSubscriptionsHiddenText: String
    val profSubscriptionsAmountHiddenText: String
    val otherEquipmentHiddenText: String
    val otherEquipmentAmountHiddenText: String

    def expectedInsetText(taxYear: Int = taxYear): String
  }

  trait CommonExpectedResults {
    val changeText: String
    val employmentExpenses: String
    val jobExpensesQuestion: String
    val jobExpensesAmount: String
    val flatRateJobExpensesQuestion: String
    val flatRateJobExpensesAmount: String
    val professionalSubscriptionsQuestion: String
    val professionalSubscriptionsAmount: String
    val otherAndCapitalAllowancesQuestionInYear: String
    val otherAndCapitalAllowancesQuestion: String
    val otherAndCapitalAllowancesAmount: String
    val fieldNames: Seq[String]
    val yes: String
    val no: String
    val continueButtonText: String
    val continueButtonLink: String

    def expectedCaption(taxYear: Int = taxYear): String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val changeText: String = "Change"
    val employmentExpenses = "Employment expenses"
    val jobExpensesQuestion = "Business travel and overnight stays"
    val jobExpensesAmount = "Amount for business travel and overnight stays"
    val flatRateJobExpensesQuestion = "Uniforms, work clothes, or tools"
    val flatRateJobExpensesAmount = "Amount for uniforms, work clothes, or tools"
    val professionalSubscriptionsQuestion = "Professional fees and subscriptions"
    val professionalSubscriptionsAmount = "Amount for professional fees and subscriptions"
    val otherAndCapitalAllowancesQuestionInYear = "Other expenses"
    val otherAndCapitalAllowancesQuestion = "Other equipment"
    val otherAndCapitalAllowancesAmount = "Amount for other equipment"
    val continueButtonText = "Save and continue"
    val continueButtonLink = "/update-and-submit-income-tax-return/employment-income/2021/expenses/check-employment-expenses"
    val fieldNames = Seq(
      "Business travel and overnight stays",
      "Uniforms, work clothes, or tools",
      "Professional fees and subscriptions",
      "Other expenses",
    )
    val yes: String = "Yes"
    val no: String = "No"

    def expectedCaption(taxYear: Int = taxYear): String = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val changeText: String = "Change"
    val employmentExpenses = "Employment expenses"
    val jobExpensesQuestion = "Business travel and overnight stays"
    val jobExpensesAmount = "Amount for business travel and overnight stays"
    val flatRateJobExpensesQuestion = "Uniforms, work clothes, or tools"
    val flatRateJobExpensesAmount = "Amount for uniforms, work clothes, or tools"
    val professionalSubscriptionsQuestion = "Professional fees and subscriptions"
    val professionalSubscriptionsAmount = "Amount for professional fees and subscriptions"
    val otherAndCapitalAllowancesQuestionInYear = "Other expenses"
    val otherAndCapitalAllowancesQuestion = "Other equipment"
    val otherAndCapitalAllowancesAmount = "Amount for other equipment"
    val continueButtonText = "Save and continue"
    val continueButtonLink = "/update-and-submit-income-tax-return/employment-income/2021/expenses/check-employment-expenses"

    val fieldNames = Seq(
      "Business travel and overnight stays",
      "Uniforms, work clothes, or tools",
      "Professional fees and subscriptions",
      "Other expenses",
    )
    val yes: String = "Yes"
    val no: String = "No"

    def expectedCaption(taxYear: Int = taxYear): String = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your employment expenses"
    val expectedTitle = "Check your employment expenses"
    val expectedContentSingle = "Your employment expenses are based on the information we already hold about you."
    val expectedContentMultiple: String = "Your employment expenses are based on the information we already hold about you. " +
      "This is a total of expenses from all employment in the tax year."
    val expectedInsetMultiple = "Your employment expenses is a total of all employment in the tax year."
    val hmrcOnlyInfo = "Your employment expenses are based on the information we already hold about you."
    val expensesHiddenText = "Change if you want to claim employment expenses"
    val jobExpensesHiddenText = "Change if you want to claim business travel and overnight stays"
    val jobExpensesAmountHiddenText = "Change the amount you want to claim for business travel and overnight stays"
    val flatRateHiddenText = "Change if you want to claim uniforms, work clothes, or tools"
    val flatRateAmountHiddenText = "Change the amount you want to claim for uniforms, work clothes, or tools"
    val profSubscriptionsHiddenText = "Change if you want to claim professional fees and subscriptions"
    val profSubscriptionsAmountHiddenText = "Change the amount you want to claim for professional fees and subscriptions"
    val otherEquipmentHiddenText = "Change if you want to claim for other equipment"
    val otherEquipmentAmountHiddenText = "Change the amount you want to claim for other equipment"

    def expectedInsetText(taxYear: Int = taxYear): String = s"You cannot update your employment expenses until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment expenses"
    val expectedTitle = "Check your client’s employment expenses"
    val expectedContentSingle = "Your client’s employment expenses are based on the information we already hold about them."
    val expectedContentMultiple: String = "Your client’s employment expenses are based on the information we already hold about them. " +
      "This is a total of expenses from all employment in the tax year."
    val expectedInsetMultiple = "Your client’s employment expenses is a total of all employment in the tax year."
    val hmrcOnlyInfo = "Your client’s employment expenses are based on the information we already hold about them."
    val expensesHiddenText = "Change if you want to claim employment expenses for your client"
    val jobExpensesHiddenText = "Change if you want to claim business travel and overnight stays for your client"
    val jobExpensesAmountHiddenText = "Change the amount you want to claim for your client’s business travel and overnight stays"
    val flatRateHiddenText = "Change if you want to claim uniforms, work clothes, or tools for your client"
    val flatRateAmountHiddenText = "Change the amount you want to claim for your client’s uniforms, work clothes, or tools"
    val profSubscriptionsHiddenText = "Change if you want to claim professional fees and subscriptions for your client"
    val profSubscriptionsAmountHiddenText = "Change the amount you want to claim for your client’s professional fees and subscriptions"
    val otherEquipmentHiddenText = "Change if you want to claim for other equipment for your client"
    val otherEquipmentAmountHiddenText = "Change the amount you want to claim for your client’s other equipment"

    def expectedInsetText(taxYear: Int = taxYear): String = s"You cannot update your client’s employment expenses until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Check your employment expenses"
    val expectedTitle = "Check your employment expenses"
    val expectedContentSingle = "Your employment expenses are based on the information we already hold about you."
    val expectedContentMultiple: String = "Your employment expenses are based on the information we already hold about you. " +
      "This is a total of expenses from all employment in the tax year."
    val expectedInsetMultiple = "Your employment expenses is a total of all employment in the tax year."
    val hmrcOnlyInfo = "Your employment expenses are based on the information we already hold about you."
    val expensesHiddenText = "Change if you want to claim employment expenses"
    val jobExpensesHiddenText = "Change if you want to claim business travel and overnight stays"
    val jobExpensesAmountHiddenText = "Change the amount you want to claim for business travel and overnight stays"
    val flatRateHiddenText = "Change if you want to claim uniforms, work clothes, or tools"
    val flatRateAmountHiddenText = "Change the amount you want to claim for uniforms, work clothes, or tools"
    val profSubscriptionsHiddenText = "Change if you want to claim professional fees and subscriptions"
    val profSubscriptionsAmountHiddenText = "Change the amount you want to claim for professional fees and subscriptions"
    val otherEquipmentHiddenText = "Change if you want to claim for other equipment"
    val otherEquipmentAmountHiddenText = "Change the amount you want to claim for other equipment"

    def expectedInsetText(taxYear: Int = taxYear): String = s"You cannot update your employment expenses until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment expenses"
    val expectedTitle = "Check your client’s employment expenses"
    val expectedContentSingle = "Your client’s employment expenses are based on the information we already hold about them."
    val expectedContentMultiple: String = "Your client’s employment expenses are based on the information we already hold about them. " +
      "This is a total of expenses from all employment in the tax year."
    val expectedInsetMultiple = "Your client’s employment expenses is a total of all employment in the tax year."
    val hmrcOnlyInfo = "Your client’s employment expenses are based on the information we already hold about them."
    val expensesHiddenText = "Change if you want to claim employment expenses for your client"
    val jobExpensesHiddenText = "Change if you want to claim business travel and overnight stays for your client"
    val jobExpensesAmountHiddenText = "Change the amount you want to claim for your client’s business travel and overnight stays"
    val flatRateHiddenText = "Change if you want to claim uniforms, work clothes, or tools for your client"
    val flatRateAmountHiddenText = "Change the amount you want to claim for your client’s uniforms, work clothes, or tools"
    val profSubscriptionsHiddenText = "Change if you want to claim professional fees and subscriptions for your client"
    val profSubscriptionsAmountHiddenText = "Change the amount you want to claim for your client’s professional fees and subscriptions"
    val otherEquipmentHiddenText = "Change if you want to claim for other equipment for your client"
    val otherEquipmentAmountHiddenText = "Change the amount you want to claim for your client’s other equipment"

    def expectedInsetText(taxYear: Int = taxYear): String = s"You cannot update your client’s employment expenses until 6 April $taxYear."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  private val multipleEmployments: AllEmploymentData = anAllEmploymentData.copy(Seq(
    anEmploymentSource,
    anEmploymentSource.copy(employmentId = "002")
  ))
  private val partExpenses: Expenses = Expenses(Some(1), Some(2))

  object Hrefs {
    val dummyHref = s"/update-and-submit-income-tax-return/employment-income/${taxYear - 1}/expenses/check-employment-expenses"
    val claimExpensesHref = s"/update-and-submit-income-tax-return/employment-income/${taxYear - 1}/expenses/claim-employment-expenses"
    val businessTravelOvernightExpensesHref = s"/update-and-submit-income-tax-return/employment-income/${taxYear - 1}/expenses/business-travel-and-overnight-expenses"
    val uniformsOrToolsExpensesHref = s"/update-and-submit-income-tax-return/employment-income/${taxYear - 1}/expenses/uniforms-work-clothes-or-tools"
    val uniformsOrToolsExpensesAmountHref = s"/update-and-submit-income-tax-return/employment-income/${taxYear - 1}/expenses/amount-for-uniforms-work-clothes-or-tools"
    val professionalFeesAndSubscriptionsHref = s"/update-and-submit-income-tax-return/employment-income/${taxYear - 1}/expenses/professional-fees-and-subscriptions"
    val travelAndOvernightAmountHref = s"/update-and-submit-income-tax-return/employment-income/${taxYear - 1}/expenses/travel-amount"
    val professionalFeesSubscriptionsAmountHref = s"/update-and-submit-income-tax-return/employment-income/${taxYear - 1}/expenses/amount-for-professional-fees-and-subscriptions"
    val otherEquipmentHref = s"/update-and-submit-income-tax-return/employment-income/${taxYear - 1}/expenses/other-equipment"
    val otherEquipmentAmountHref = s"/update-and-submit-income-tax-return/employment-income/${taxYear - 1}/expenses/amount-for-other-equipment"
  }

  ".show" when {
    import Hrefs._
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return a fully populated page when all the fields are populated" which {
          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            urlGet(url(), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption())
          textOnPageCheck(user.specificExpectedResults.get.expectedContentSingle, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText(), insetTextSelector)
          welshToggleCheck(user.isWelsh)

          textOnPageCheck(user.commonExpectedResults.fieldNames.head, summaryListRowFieldNameSelector(1))
          textOnPageCheck("£200", summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListRowFieldNameSelector(2))
          textOnPageCheck("£300", summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.fieldNames(2), summaryListRowFieldNameSelector(3))
          textOnPageCheck("£400", summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.fieldNames(3), summaryListRowFieldNameSelector(4))
          textOnPageCheck("£600", summaryListRowFieldAmountSelector(4))
        }

        "return a fully populated page when all the fields are populated at the end of the year" which {
          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
            urlGet(url(taxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }
          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specificResults.expectedTitle)
          h1Check(specificResults.expectedH1)
          captionCheck(commonResults.expectedCaption(taxYear - 1))
          textOnPageCheck(specificResults.expectedContentSingle, contentSelector)
          welshToggleCheck(user.isWelsh)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(user.commonExpectedResults.continueButtonLink, continueButtonFormSelector)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimExpensesHref)
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", businessTravelOvernightExpensesHref)
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£200", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", travelAndOvernightAmountHref)
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", uniformsOrToolsExpensesHref)
          changeAmountRowCheck(commonResults.flatRateJobExpensesAmount, "£300", summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateAmountHiddenText}", uniformsOrToolsExpensesAmountHref)
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.yes, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", professionalFeesAndSubscriptionsHref)
          changeAmountRowCheck(commonResults.professionalSubscriptionsAmount, "£400", summaryListRowFieldNameSelector(7), summaryListRowFieldAmountSelector(7),
            changeLinkSelector(7), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsAmountHiddenText}", professionalFeesSubscriptionsAmountHref)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.yes, summaryListRowFieldNameSelector(8), summaryListRowFieldAmountSelector(8),
            changeLinkSelector(8), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", otherEquipmentHref)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesAmount, "£600", summaryListRowFieldNameSelector(9), summaryListRowFieldAmountSelector(9),
            changeLinkSelector(9), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentAmountHiddenText}", otherEquipmentAmountHref)
        }

        "return a empty populated page when all the fields are empty at the end of the year" which {
          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None))), nino, taxYear - 1)
            urlGet(url(taxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specificResults.expectedTitle)
          h1Check(specificResults.expectedH1)
          captionCheck(commonResults.expectedCaption(taxYear - 1))
          welshToggleCheck(user.isWelsh)
          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.no, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimExpensesHref)
        }

        "return a empty populated page when there is no prior data at the end of the year" which {
          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(), nino, taxYear - 1)
            urlGet(url(taxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))
          welshToggleCheck(user.isWelsh)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(user.commonExpectedResults.continueButtonLink, continueButtonFormSelector)
          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.no, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimExpensesHref)

        }

        "return a fully populated page when all the fields are populated at the end of the year when there is CYA data" which {
          def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
            anExpensesUserData.copy(isPriorSubmission = isPrior, hasPriorExpenses = hasPriorExpenses, expensesCya = expensesCyaModel)

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel.copy(
              anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined))), aUserRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
            urlGet(url(taxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))
          textOnPageCheck(user.specificExpectedResults.get.expectedContentSingle, contentSelector)
          welshToggleCheck(user.isWelsh)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(user.commonExpectedResults.continueButtonLink, continueButtonFormSelector)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimExpensesHref)
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", businessTravelOvernightExpensesHref)
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£200", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", travelAndOvernightAmountHref)
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", uniformsOrToolsExpensesHref)
          changeAmountRowCheck(commonResults.flatRateJobExpensesAmount, "£300", summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateAmountHiddenText}", uniformsOrToolsExpensesAmountHref)
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.yes, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", professionalFeesAndSubscriptionsHref)
          changeAmountRowCheck(commonResults.professionalSubscriptionsAmount, "£400", summaryListRowFieldNameSelector(7), summaryListRowFieldAmountSelector(7),
            changeLinkSelector(7), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsAmountHiddenText}", professionalFeesSubscriptionsAmountHref)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.yes, summaryListRowFieldNameSelector(8), summaryListRowFieldAmountSelector(8),
            changeLinkSelector(8), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", otherEquipmentHref)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesAmount, "£600", summaryListRowFieldNameSelector(9), summaryListRowFieldAmountSelector(9),
            changeLinkSelector(9), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentAmountHiddenText}", otherEquipmentAmountHref)
        }

        "return a fully populated page with correct paragraph text when all the fields are populated and there are multiple employments" which {
          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYear)
            urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContentMultiple, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText(taxYear), insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.fieldNames.head, summaryListRowFieldNameSelector(1))
          textOnPageCheck("£200", summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListRowFieldNameSelector(2))
          textOnPageCheck("£300", summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.fieldNames(2), summaryListRowFieldNameSelector(3))
          textOnPageCheck("£400", summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.fieldNames(3), summaryListRowFieldNameSelector(4))
          textOnPageCheck("£600", summaryListRowFieldAmountSelector(4))
        }

        "return a fully populated page with correct paragraph text when there are multiple employments and its end of year" which {
          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYear - 1)
            urlGet(url(taxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }
          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetMultiple, insetEOYSelector)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(user.commonExpectedResults.continueButtonLink, continueButtonFormSelector)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimExpensesHref)
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", businessTravelOvernightExpensesHref)
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£200", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", travelAndOvernightAmountHref)
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", uniformsOrToolsExpensesHref)
          changeAmountRowCheck(commonResults.flatRateJobExpensesAmount, "£300", summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateAmountHiddenText}", uniformsOrToolsExpensesAmountHref)
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.yes, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", professionalFeesAndSubscriptionsHref)
          changeAmountRowCheck(commonResults.professionalSubscriptionsAmount, "£400", summaryListRowFieldNameSelector(7), summaryListRowFieldAmountSelector(7),
            changeLinkSelector(7), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsAmountHiddenText}", professionalFeesSubscriptionsAmountHref)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.yes, summaryListRowFieldNameSelector(8), summaryListRowFieldAmountSelector(8),
            changeLinkSelector(8), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", otherEquipmentHref)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesAmount, "£600", summaryListRowFieldNameSelector(9), summaryListRowFieldAmountSelector(9),
            changeLinkSelector(9), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentAmountHiddenText}", otherEquipmentAmountHref)
        }

        "return a partly populated page with only relevant data and the others default to 'No' at the end of the year " which {
          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            val employmentData = anAllEmploymentData.copy(hmrcExpenses = Some(anEmploymentExpenses.copy(expenses = Some(partExpenses))))
            userDataStub(anIncomeTaxUserData.copy(Some(employmentData)), nino, taxYear - 1)
            urlGet(url(taxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))
          textOnPageCheck(user.specificExpectedResults.get.expectedContentSingle, contentSelector)
          welshToggleCheck(user.isWelsh)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(user.commonExpectedResults.continueButtonLink, continueButtonFormSelector)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimExpensesHref)
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", businessTravelOvernightExpensesHref)
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£2", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", travelAndOvernightAmountHref)
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.no, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", uniformsOrToolsExpensesHref)
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.no, summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", professionalFeesAndSubscriptionsHref)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.no, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", otherEquipmentHref)
        }
      }
    }

    "redirect to overview page when theres no expenses" in {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None))), nino, taxYear)
        urlGet(url(), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        dropExpensesDB()
        unauthorisedAgentOrIndividual(isAgent = false)
        urlGet(url())
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

  ".submit" when {
    def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
      ExpensesUserData(sessionId, mtditid, nino, taxYear - 1, isPriorSubmission = isPrior, hasPriorExpenses, expensesCyaModel)

    "return a redirect when in year" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlPost(url(), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect when at the end of the year when no cya data" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlPost(url(taxYear - 1), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has a url of expenses show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYear - 1).url)
      }
    }

    "redirect to the missing section if the expense questions are incomplete when submitting CYA data at the end of the year" which {

      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined).copy(professionalSubscriptionsQuestion = None))), aUserRequest)

        urlPost(url(taxYear - 1), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has a url of expenses show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ProfessionalFeesAndSubscriptionsExpensesController.show(taxYear - 1).url)
      }
    }

    "redirect to the first missing section if there are more than one incomplete expense questions when submitting CYA data at the end of the year" which {

      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          ExpensesCYAModel(anExpensesViewModel.copy(
            professionalSubscriptionsQuestion = None, otherAndCapitalAllowancesQuestion = None))), aUserRequest)

        urlPost(url(taxYear - 1), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has a url of expenses show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ProfessionalFeesAndSubscriptionsExpensesController.show(taxYear - 1).url)
      }
    }

    "create the model to update the data and return the correct redirect when no customer data and cya data submitted cya data is different from hmrc expense" which {

      implicit lazy val result: WSResponse = {

        val newAmount = BigDecimal(10000.99)
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined).copy(
            professionalSubscriptions = Some(newAmount)))), aUserRequest)

        val model = CreateUpdateExpensesRequest(
          Some(true), anExpenses.copy(professionalSubscriptions = Some(newAmount))
        )

        stubPutWithHeadersCheck(s"/income-tax-expenses/income-tax/nino/$nino/sources\\?taxYear=2021", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(url(taxYear - 1), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an SEE OTHER status and cyaData cleared as data was submitted" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EmploymentSummaryController.show(taxYear - 1).url)
        findExpensesCyaData(taxYear - 1, aUserRequest) shouldBe None
      }

    }

    "create the model to update the data and return redirect when there is no customer expenses and nothing has changed in relation to hmrc expenses" which {

      implicit lazy val result: WSResponse = {

        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined))), aUserRequest)

        urlPost(url(taxYear - 1), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an SEE OTHER status and cyaData not cleared as no changes were made" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EmploymentSummaryController.show(taxYear - 1).url)
        findExpensesCyaData(taxYear - 1, aUserRequest) shouldBe defined
      }

    }

    "create the model to update the data and return redirect when there are no hmrc expenses and nothing has changed in relation to customer expenses" which {

      implicit lazy val result: WSResponse = {

        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None, customerExpenses = Some(anEmploymentExpenses)))), nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined))), aUserRequest)

        urlPost(url(taxYear - 1), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an SEE OTHER status and cyaData not cleared as no changes were made" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EmploymentSummaryController.show(taxYear - 1).url)
        findExpensesCyaData(taxYear - 1, aUserRequest) shouldBe defined
      }

    }

  }

}
