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
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.expenses.ExpensesBuilder.anExpenses
import builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserData
import builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import common.SessionValues
import helpers.SessionCookieCrumbler.getSessionMap
import models.IncomeTaxUserData
import models.employment.AllEmploymentData
import models.expenses.Expenses
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import models.requests.CreateUpdateExpensesRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CheckEmploymentExpensesControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  object Selectors {
    val headingSelector = "#main-content > div > div > header > h1"
    val subHeadingSelector = "#main-content > div > div > header > p"
    val contentSelector = "#main-content > div > div > p.govuk-body"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val summaryListSelector = "#main-content > div > div > dl"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
    val returnToEmploymentSummarySelector = "#returnToEmploymentSummaryBtn"

    def summaryListRowFieldNameSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dt"

    def summaryListRowFieldAmountSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__value"

    def changeLinkSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd > a"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedContent: String
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
    val returnToEmploymentSummaryText: String

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
    val returnToEmploymentSummaryText: String = "Return to PAYE employment"
    val fieldNames = Seq(
      "Employment expenses",
      "Business travel and overnight stays",
      "Amount for business travel and overnight stays",
      "Uniforms, work clothes, or tools",
      "Amount for uniforms, work clothes, or tools",
      "Professional fees and subscriptions",
      "Amount for professional fees and subscriptions",
      "Other equipment",
      "Amount for other equipment",
    )
    val yes: String = "Yes"
    val no: String = "No"

    def expectedCaption(taxYear: Int = taxYear): String = s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
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
    val returnToEmploymentSummaryText: String = "Return to PAYE employment"

    val fieldNames = Seq(
      "Employment expenses",
      "Business travel and overnight stays",
      "Amount for business travel and overnight stays",
      "Uniforms, work clothes, or tools",
      "Amount for uniforms, work clothes, or tools",
      "Professional fees and subscriptions",
      "Amount for professional fees and subscriptions",
      "Other equipment",
      "Amount for other equipment",
    )
    val yes: String = "Yes"
    val no: String = "No"

    def expectedCaption(taxYear: Int = taxYear): String = s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your employment expenses"
    val expectedTitle = "Check your employment expenses"
    val expectedContent = "Your employment expenses are based on the information we already hold about you."
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
    val expectedContent = "Your client’s employment expenses are based on the information we already hold about them."
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
    val expectedContent = "Your employment expenses are based on the information we already hold about you."
    val expectedSubHeadingContent: String = "Your employment expenses are based on the information we already hold about you."
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
    val expectedContent = "Your client’s employment expenses are based on the information we already hold about them."
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

  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return a fully populated page when all the fields are populated" which {
          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            val employmentExpenses = anEmploymentExpenses.copy(expenses = Some(anExpenses.copy(professionalSubscriptions = None)))
            userDataStub(IncomeTaxUserData(Some(anAllEmploymentData.copy(hmrcExpenses = Some(employmentExpenses)))), nino, taxYear)
            urlGet(fullUrl(checkYourExpensesUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption())
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText(), insetTextSelector)
          welshToggleCheck(user.isWelsh)

          textOnPageCheck(user.commonExpectedResults.fieldNames.head, summaryListRowFieldNameSelector(1))
          textOnPageCheck("Yes", summaryListRowFieldAmountSelector(1), "for section question")
          textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListRowFieldNameSelector(2))
          textOnPageCheck("Yes", summaryListRowFieldAmountSelector(2), "for jobExpensesQuestion")
          textOnPageCheck(user.commonExpectedResults.fieldNames(2), summaryListRowFieldNameSelector(3))
          textOnPageCheck("£200", summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.fieldNames(3), summaryListRowFieldNameSelector(4))
          textOnPageCheck("Yes", summaryListRowFieldAmountSelector(4), "for flatRateJobExpensesQuestion")
          textOnPageCheck(user.commonExpectedResults.fieldNames(4), summaryListRowFieldNameSelector(5))
          textOnPageCheck("£300", summaryListRowFieldAmountSelector(5))
          textOnPageCheck(user.commonExpectedResults.fieldNames(5), summaryListRowFieldNameSelector(6))
          textOnPageCheck("No", summaryListRowFieldAmountSelector(6), "for professionalSubscriptionsQuestion")
          textOnPageCheck(user.commonExpectedResults.fieldNames(7), summaryListRowFieldNameSelector(7))
          textOnPageCheck("Yes", summaryListRowFieldAmountSelector(7), "otherAndCapitalAllowancesQuestion")
          textOnPageCheck(user.commonExpectedResults.fieldNames(8), summaryListRowFieldNameSelector(8))
          textOnPageCheck("£600", summaryListRowFieldAmountSelector(8))
          buttonCheck(user.commonExpectedResults.returnToEmploymentSummaryText, returnToEmploymentSummarySelector)
        }

        "return a fully populated page when all the fields are populated at the end of the year" which {
          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
            urlGet(fullUrl(checkYourExpensesUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }
          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specificResults.expectedTitle)
          h1Check(specificResults.expectedH1)
          captionCheck(commonResults.expectedCaption(taxYear - 1))
          textOnPageCheck(specificResults.expectedContent, contentSelector)
          welshToggleCheck(user.isWelsh)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(checkYourExpensesUrl(taxYearEOY), continueButtonFormSelector)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimEmploymentExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", businessTravelExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£200", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", travelAmountExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", uniformsWorkClothesToolsExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.flatRateJobExpensesAmount, "£300", summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateAmountHiddenText}", uniformsClothesToolsExpensesAmountUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.yes, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", professionalFeesExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.professionalSubscriptionsAmount, "£400", summaryListRowFieldNameSelector(7), summaryListRowFieldAmountSelector(7),
            changeLinkSelector(7), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsAmountHiddenText}", professionalFeesExpensesAmountUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.yes, summaryListRowFieldNameSelector(8), summaryListRowFieldAmountSelector(8),
            changeLinkSelector(8), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", otherEquipmentExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesAmount, "£600", summaryListRowFieldNameSelector(9), summaryListRowFieldAmountSelector(9),
            changeLinkSelector(9), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentAmountHiddenText}", otherEquipmentExpensesAmountUrl(taxYearEOY))
        }

        "return a empty populated page when all the fields are empty at the end of the year" which {
          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None))), nino, taxYear - 1)
            urlGet(fullUrl(checkYourExpensesUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specificResults.expectedTitle)
          h1Check(specificResults.expectedH1)
          captionCheck(commonResults.expectedCaption(taxYear - 1))
          welshToggleCheck(user.isWelsh)
          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.no, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimEmploymentExpensesUrl(taxYearEOY))
        }

        "return a empty populated page when there is no prior data at the end of the year" which {
          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(), nino, taxYear - 1)
            urlGet(fullUrl(checkYourExpensesUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))
          welshToggleCheck(user.isWelsh)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(checkYourExpensesUrl(taxYearEOY), continueButtonFormSelector)
          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.no, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimEmploymentExpensesUrl(taxYearEOY))

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
            urlGet(fullUrl(checkYourExpensesUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentSelector)
          welshToggleCheck(user.isWelsh)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(checkYourExpensesUrl(taxYearEOY), continueButtonFormSelector)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimEmploymentExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", businessTravelExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£200", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", travelAmountExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", uniformsWorkClothesToolsExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.flatRateJobExpensesAmount, "£300", summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateAmountHiddenText}", uniformsClothesToolsExpensesAmountUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.yes, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", professionalFeesExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.professionalSubscriptionsAmount, "£400", summaryListRowFieldNameSelector(7), summaryListRowFieldAmountSelector(7),
            changeLinkSelector(7), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsAmountHiddenText}", professionalFeesExpensesAmountUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.yes, summaryListRowFieldNameSelector(8), summaryListRowFieldAmountSelector(8),
            changeLinkSelector(8), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", otherEquipmentExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesAmount, "£600", summaryListRowFieldNameSelector(9), summaryListRowFieldAmountSelector(9),
            changeLinkSelector(9), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentAmountHiddenText}", otherEquipmentExpensesAmountUrl(taxYearEOY))
        }

        "return a fully populated page with correct paragraph text when all the fields are populated and there are multiple employments" which {
          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYear)
            urlGet(fullUrl(checkYourExpensesUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText(taxYear), insetTextSelector)
          welshToggleCheck(user.isWelsh)

          textOnPageCheck(user.commonExpectedResults.fieldNames.head, summaryListRowFieldNameSelector(1))
          textOnPageCheck("Yes", summaryListRowFieldAmountSelector(1), "for section question")
          textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListRowFieldNameSelector(2))
          textOnPageCheck("Yes", summaryListRowFieldAmountSelector(2), "for jobExpensesQuestion")
          textOnPageCheck(user.commonExpectedResults.fieldNames(2), summaryListRowFieldNameSelector(3))
          textOnPageCheck("£200", summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.fieldNames(3), summaryListRowFieldNameSelector(4))
          textOnPageCheck("Yes", summaryListRowFieldAmountSelector(4), "for flatRateJobExpensesQuestion")
          textOnPageCheck(user.commonExpectedResults.fieldNames(4), summaryListRowFieldNameSelector(5))
          textOnPageCheck("£300", summaryListRowFieldAmountSelector(5))
          textOnPageCheck(user.commonExpectedResults.fieldNames(5), summaryListRowFieldNameSelector(6))
          textOnPageCheck("Yes", summaryListRowFieldAmountSelector(6), "for professionalSubscriptionsQuestion")
          textOnPageCheck(user.commonExpectedResults.fieldNames(6), summaryListRowFieldNameSelector(7))
          textOnPageCheck("£400", summaryListRowFieldAmountSelector(7))
          textOnPageCheck(user.commonExpectedResults.fieldNames(7), summaryListRowFieldNameSelector(8))
          textOnPageCheck("Yes", summaryListRowFieldAmountSelector(8), "otherAndCapitalAllowancesQuestion")
          textOnPageCheck(user.commonExpectedResults.fieldNames(8), summaryListRowFieldNameSelector(9))
          textOnPageCheck("£600", summaryListRowFieldAmountSelector(9))
          buttonCheck(user.commonExpectedResults.returnToEmploymentSummaryText, returnToEmploymentSummarySelector)
        }

        "return a fully populated page with correct paragraph text when there are multiple employments and its end of year" which {
          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYear - 1)
            urlGet(fullUrl(checkYourExpensesUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }
          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(checkYourExpensesUrl(taxYearEOY), continueButtonFormSelector)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimEmploymentExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", businessTravelExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£200", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", travelAmountExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", uniformsWorkClothesToolsExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.flatRateJobExpensesAmount, "£300", summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateAmountHiddenText}", uniformsClothesToolsExpensesAmountUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.yes, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", professionalFeesExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.professionalSubscriptionsAmount, "£400", summaryListRowFieldNameSelector(7), summaryListRowFieldAmountSelector(7),
            changeLinkSelector(7), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsAmountHiddenText}", professionalFeesExpensesAmountUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.yes, summaryListRowFieldNameSelector(8), summaryListRowFieldAmountSelector(8),
            changeLinkSelector(8), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", otherEquipmentExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesAmount, "£600", summaryListRowFieldNameSelector(9), summaryListRowFieldAmountSelector(9),
            changeLinkSelector(9), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentAmountHiddenText}", otherEquipmentExpensesAmountUrl(taxYearEOY))
        }

        "return a partly populated page with only relevant data and the others default to 'No' at the end of the year " which {
          implicit lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            val employmentData = anAllEmploymentData.copy(hmrcExpenses = Some(anEmploymentExpenses.copy(expenses = Some(partExpenses))))
            userDataStub(anIncomeTaxUserData.copy(Some(employmentData)), nino, taxYear - 1)
            urlGet(fullUrl(checkYourExpensesUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          val commonResults = user.commonExpectedResults
          val specificResults = user.specificExpectedResults.get

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentSelector)
          welshToggleCheck(user.isWelsh)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(checkYourExpensesUrl(taxYearEOY), continueButtonFormSelector)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${user.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", claimEmploymentExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", businessTravelExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£2", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${user.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", travelAmountExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.no, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${user.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", uniformsWorkClothesToolsExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.no, summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", professionalFeesExpensesUrl(taxYearEOY))
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.no, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${user.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", otherEquipmentExpensesUrl(taxYearEOY))
        }
      }
    }

    "redirect to overview page when theres no expenses" in {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None))), nino, taxYear)
        urlGet(fullUrl(checkYourExpensesUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }

    "redirect to employment expenses page when no expenses has been added yet (making a new employment journey)" in {
      val customerData = anEmploymentSource

      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None, customerEmploymentData = Seq(customerData)))), nino, taxYear - 1)
        urlGet(fullUrl(checkYourExpensesUrl(taxYear - 1)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1, Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> customerData.employmentId))))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(claimEmploymentExpensesUrl(taxYearEOY)) shouldBe true

    }

    "redirect to 'do you need to add any additional new expenses?' page when there's prior expenses (making a new employment journey)" in {
      val customerData = anEmploymentSource

      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(customerEmploymentData = Seq(customerData)))), nino, taxYear - 1)
        urlGet(fullUrl(checkYourExpensesUrl(taxYear - 1)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1, Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> customerData.employmentId))))
      }

      result.status shouldBe SEE_OTHER
      //TODO: add a redirect for "do you need to add any additional/new expenses?" page when available
      result.header("location").contains(claimEmploymentExpensesUrl(taxYearEOY)) shouldBe true

    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        dropExpensesDB()
        unauthorisedAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(checkYourExpensesUrl(taxYear)))
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
        urlPost(fullUrl(checkYourExpensesUrl(taxYear)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect when at the end of the year when no cya data" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has a url of expenses show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the missing section if the expense questions are incomplete when submitting CYA data at the end of the year" which {

      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined).copy(professionalSubscriptionsQuestion = None))), aUserRequest)

        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has a url of expenses show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(professionalFeesExpensesUrl(taxYearEOY)) shouldBe true
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

        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has a url of expenses show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(professionalFeesExpensesUrl(taxYearEOY)) shouldBe true
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

        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an SEE OTHER status and cyaData cleared as data was submitted" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
        findExpensesCyaData(taxYear - 1, aUserRequest) shouldBe None
        getSessionMap(result, "mdtp").get("TEMP_NEW_EMPLOYMENT_ID") shouldBe None
      }

    }

    "create the model to update the data and return redirect when there is no customer expenses and nothing has changed in relation to hmrc expenses" which {

      implicit lazy val result: WSResponse = {

        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined))), aUserRequest)

        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an SEE OTHER status and cyaData not cleared as no changes were made" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
        findExpensesCyaData(taxYear - 1, aUserRequest) shouldBe defined
        getSessionMap(result, "mdtp").get("TEMP_NEW_EMPLOYMENT_ID") shouldBe None
      }

    }

    "create the model to update the data and return redirect when there are no hmrc expenses and nothing has changed in relation to customer expenses" which {

      implicit lazy val result: WSResponse = {

        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcExpenses = None, customerExpenses = Some(anEmploymentExpenses)))), nino, taxYear - 1)

        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
          ExpensesCYAModel(anExpenses.toExpensesViewModel(anAllEmploymentData.customerExpenses.isDefined))), aUserRequest)

        urlPost(fullUrl(checkYourExpensesUrl(taxYearEOY)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an SEE OTHER status and cyaData not cleared as no changes were made" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
        findExpensesCyaData(taxYear - 1, aUserRequest) shouldBe defined
        getSessionMap(result, "mdtp").get("TEMP_NEW_EMPLOYMENT_ID") shouldBe None
      }

    }

  }

}
