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

package views.expenses

import controllers.expenses.routes._
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import views.html.expenses.CheckEmploymentExpensesView

class CheckEmploymentExpensesViewSpec extends ViewUnitTest {

  object Selectors {
    val contentSelector = "#main-content > div > div > p"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
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
    val otherAndCapitalAllowancesQuestion = "Other equipment"
    val otherAndCapitalAllowancesAmount = "Amount for other equipment"
    val continueButtonText = "Save and continue"
    val returnToEmploymentSummaryText: String = "Return to PAYE employment"

    val fieldNames: Seq[String] = Seq(
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
    val changeText: String = "Newid"
    val employmentExpenses = "Treuliau cyflogaeth"
    val jobExpensesQuestion = "Costau teithio busnes ac aros dros nos"
    val jobExpensesAmount = "Swm ar gyfer costau teithio busnes ac aros dros nos"
    val flatRateJobExpensesQuestion = "Gwisgoedd unffurf, dillad gwaith, neu offer"
    val flatRateJobExpensesAmount = "Swm ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer"
    val professionalSubscriptionsQuestion = "Ffioedd a thanysgrifiadau proffesiynol"
    val professionalSubscriptionsAmount = "Swm ar gyfer ffioedd a thanysgrifiadau proffesiynol"
    val otherAndCapitalAllowancesQuestion = "Offer eraill"
    val otherAndCapitalAllowancesAmount = "Swm ar gyfer offer eraill"
    val continueButtonText = "Cadw ac yn eich blaen"
    val returnToEmploymentSummaryText: String = "Yn ôl i ‘Cyflogaeth TWE’"

    val fieldNames: Seq[String] = Seq(
      "Treuliau cyflogaeth",
      "Costau teithio busnes ac aros dros nos",
      "Swm ar gyfer costau teithio busnes ac aros dros nos",
      "Gwisgoedd unffurf, dillad gwaith, neu offer",
      "Swm ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer",
      "Ffioedd a thanysgrifiadau proffesiynol",
      "Swm ar gyfer ffioedd a thanysgrifiadau proffesiynol",
      "Offer eraill",
      "Swm ar gyfer offer eraill",
    )
    val yes: String = "Iawn"
    val no: String = "Na"

    def expectedCaption(taxYear: Int = taxYear): String = s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
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
    val expectedH1 = "Gwiriwch eich treuliau cyflogaeth"
    val expectedTitle = "Gwiriwch eich treuliau cyflogaeth"
    val expectedContent = "Bydd eich treuliau cyflogaeth yn seiliedig ar yr wybodaeth sydd eisoes gennym amdanoch."
    val expensesHiddenText = "Newidiwch os ydych am hawlio treuliau cyflogaeth"
    val jobExpensesHiddenText = "Newidiwch os ydych am hawlio ar gyfer costau teithio busnes ac aros dros nos"
    val jobExpensesAmountHiddenText = "Newidiwch y swm rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos"
    val flatRateHiddenText = "Newidiwch os ydych am hawlio gwisgoedd unffurf, dillad gwaith neu offer"
    val flatRateAmountHiddenText = "Newidiwch y swm rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer"
    val profSubscriptionsHiddenText = "Newidiwch os ydych am hawlio ffioedd a thanysgrifiadau proffesiynol"
    val profSubscriptionsAmountHiddenText = "Newidiwch y swm rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol"
    val otherEquipmentHiddenText = "Newidiwch y swm rydych am ei hawlio ar gyfer offer eraill"
    val otherEquipmentAmountHiddenText = "Newidiwch y swm rydych am ei hawlio am offer eraill"

    def expectedInsetText(taxYear: Int = taxYear): String = s"Does dim modd i chi ddiweddaru’ch treuliau cyflogaeth tan 6 Ebrill $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Gwiriwch dreuliau cyflogaeth eich cleient"
    val expectedTitle = "Gwiriwch dreuliau cyflogaeth eich cleient"
    val expectedContent = "Mae treuliau cyflogaeth eich cleient yn seiliedig ar yr wybodaeth sydd eisoes gennym amdano."
    val expensesHiddenText = "Newidiwch os ydych am hawlio treuliau cyflogaeth ar gyfer eich cleient"
    val jobExpensesHiddenText = "Newidiwch os ydych am hawlio ar gyfer costau teithio busnes ac aros dros nos ar gyfer eich cleient"
    val jobExpensesAmountHiddenText = "Newidiwch y swm rydych am ei hawlio ar gyfer costau teithio busnes ac aros dros nos eich cleient"
    val flatRateHiddenText = "Newidiwch os ydych am hawlio gwisgoedd unffurf, dillad gwaith neu offer ar gyfer eich cleient"
    val flatRateAmountHiddenText = "Newidiwch y swm rydych am ei hawlio ar gyfer gwisgoedd unffurf, dillad gwaith neu offer eich cleient"
    val profSubscriptionsHiddenText = "Newidiwch os ydych am hawlio ffioedd a thanysgrifiadau proffesiynol ar gyfer eich cleient"
    val profSubscriptionsAmountHiddenText = "Newidiwch y swm rydych am ei hawlio ar gyfer ffioedd a thanysgrifiadau proffesiynol eich cleient"
    val otherEquipmentHiddenText = "Newidiwch os ydych am hawlio ar gyfer offer eraill ar gyfer eich cleient"
    val otherEquipmentAmountHiddenText = "Newidiwch y swm rydych am ei hawlio ar gyfer offer eraill eich cleient"

    def expectedInsetText(taxYear: Int = taxYear): String = s"Does dim modd i chi ddiweddaru treuliau cyflogaeth eich cleient tan 6 Ebrill $taxYear."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[CheckEmploymentExpensesView]

  ".show" when {
    import Selectors._
    userScenarios.foreach { userScenario =>
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "return the page when professional subscriptions amount are not defined (In Year)" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYear, anExpensesViewModel.copy(isUsingCustomerData = false,
            professionalSubscriptions = None,
            professionalSubscriptionsQuestion = Some(false)),
            isInYear = true)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(userScenario.commonExpectedResults.expectedCaption())
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedContent, contentSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedInsetText(), insetTextSelector)
          welshToggleCheck(userScenario.isWelsh)


          textOnPageCheck(userScenario.commonExpectedResults.fieldNames.head, summaryListRowFieldNameSelector(1))
          textOnPageCheck(userScenario.commonExpectedResults.yes, summaryListRowFieldAmountSelector(1), "for section question")
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(1), summaryListRowFieldNameSelector(2))
          textOnPageCheck(userScenario.commonExpectedResults.yes, summaryListRowFieldAmountSelector(2), "for jobExpensesQuestion")
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(2), summaryListRowFieldNameSelector(3))
          textOnPageCheck("£200", summaryListRowFieldAmountSelector(3))
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(3), summaryListRowFieldNameSelector(4))
          textOnPageCheck(userScenario.commonExpectedResults.yes, summaryListRowFieldAmountSelector(4), "for flatRateJobExpensesQuestion")
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(4), summaryListRowFieldNameSelector(5))
          textOnPageCheck("£300", summaryListRowFieldAmountSelector(5))
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(5), summaryListRowFieldNameSelector(6))
          textOnPageCheck(userScenario.commonExpectedResults.no, summaryListRowFieldAmountSelector(6), "for professionalSubscriptionsQuestion")
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(7), summaryListRowFieldNameSelector(7))
          textOnPageCheck(userScenario.commonExpectedResults.yes, summaryListRowFieldAmountSelector(7), "otherAndCapitalAllowancesQuestion")
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(8), summaryListRowFieldNameSelector(8))
          textOnPageCheck("£600", summaryListRowFieldAmountSelector(8))
          buttonCheck(userScenario.commonExpectedResults.returnToEmploymentSummaryText, returnToEmploymentSummarySelector)
        }
        "return a fully populated page when all the fields are populated at the end of the year(not using customer data)" which {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, anExpensesViewModel.copy(isUsingCustomerData = false), isInYear = false)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          val commonResults = userScenario.commonExpectedResults
          val specificResults = userScenario.specificExpectedResults.get

          titleCheck(specificResults.expectedTitle, userScenario.isWelsh)
          h1Check(specificResults.expectedH1)
          captionCheck(commonResults.expectedCaption(taxYear - 1))
          textOnPageCheck(specificResults.expectedContent, contentSelector)
          welshToggleCheck(userScenario.isWelsh)
          buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(CheckEmploymentExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${userScenario.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", EmploymentExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${userScenario.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", BusinessTravelOvernightExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£200", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${userScenario.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", TravelAndOvernightAmountController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${userScenario.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", UniformsOrToolsExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.flatRateJobExpensesAmount, "£300", summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${userScenario.commonExpectedResults.changeText} ${specificResults.flatRateAmountHiddenText}", UniformsOrToolsExpensesAmountController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.yes, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${userScenario.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", ProfessionalFeesAndSubscriptionsExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.professionalSubscriptionsAmount, "£400", summaryListRowFieldNameSelector(7), summaryListRowFieldAmountSelector(7),
            changeLinkSelector(7), s"${userScenario.commonExpectedResults.changeText} ${specificResults.profSubscriptionsAmountHiddenText}", ProfFeesAndSubscriptionsExpensesAmountController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.yes, summaryListRowFieldNameSelector(8), summaryListRowFieldAmountSelector(8),
            changeLinkSelector(8), s"${userScenario.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", OtherEquipmentController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesAmount, "£600", summaryListRowFieldNameSelector(9), summaryListRowFieldAmountSelector(9),
            changeLinkSelector(9), s"${userScenario.commonExpectedResults.changeText} ${specificResults.otherEquipmentAmountHiddenText}", OtherEquipmentAmountController.submit(taxYearEOY).url)
        }

        "return a empty populated page when all the fields are empty at the end of the year" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, anExpensesViewModel.copy(
            claimingEmploymentExpenses = false,
            jobExpensesQuestion = None,
            jobExpenses = None,
            flatRateJobExpensesQuestion = None,
            flatRateJobExpenses = None,
            professionalSubscriptionsQuestion = None,
            professionalSubscriptions = None,
            otherAndCapitalAllowancesQuestion = None,
            otherAndCapitalAllowances = None,
            businessTravelCosts = None,
            hotelAndMealExpenses = None,
            vehicleExpenses = None,
            mileageAllowanceRelief = None),
            isInYear = false)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          val commonResults = userScenario.commonExpectedResults
          val specificResults = userScenario.specificExpectedResults.get

          titleCheck(specificResults.expectedTitle, userScenario.isWelsh)
          h1Check(specificResults.expectedH1)
          captionCheck(commonResults.expectedCaption(taxYear - 1))
          welshToggleCheck(userScenario.isWelsh)
          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.no, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${userScenario.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", EmploymentExpensesController.submit(taxYearEOY).url)
        }

        "return a fully populated page with correct paragraph text when all the fields are populated and there are multiple employments" which {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYear, anExpensesViewModel.copy(isUsingCustomerData = false), isInYear = true)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedContent, contentSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedInsetText(taxYear), insetTextSelector)
          welshToggleCheck(userScenario.isWelsh)

          textOnPageCheck(userScenario.commonExpectedResults.fieldNames.head, summaryListRowFieldNameSelector(1))
          textOnPageCheck(userScenario.commonExpectedResults.yes, summaryListRowFieldAmountSelector(1), "for section question")
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(1), summaryListRowFieldNameSelector(2))
          textOnPageCheck(userScenario.commonExpectedResults.yes, summaryListRowFieldAmountSelector(2), "for jobExpensesQuestion")
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(2), summaryListRowFieldNameSelector(3))
          textOnPageCheck("£200", summaryListRowFieldAmountSelector(3))
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(3), summaryListRowFieldNameSelector(4))
          textOnPageCheck(userScenario.commonExpectedResults.yes, summaryListRowFieldAmountSelector(4), "for flatRateJobExpensesQuestion")
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(4), summaryListRowFieldNameSelector(5))
          textOnPageCheck("£300", summaryListRowFieldAmountSelector(5))
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(5), summaryListRowFieldNameSelector(6))
          textOnPageCheck(userScenario.commonExpectedResults.yes, summaryListRowFieldAmountSelector(6), "for professionalSubscriptionsQuestion")
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(6), summaryListRowFieldNameSelector(7))
          textOnPageCheck("£400", summaryListRowFieldAmountSelector(7))
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(7), summaryListRowFieldNameSelector(8))
          textOnPageCheck(userScenario.commonExpectedResults.yes, summaryListRowFieldAmountSelector(8), "otherAndCapitalAllowancesQuestion")
          textOnPageCheck(userScenario.commonExpectedResults.fieldNames(8), summaryListRowFieldNameSelector(9))
          textOnPageCheck("£600", summaryListRowFieldAmountSelector(9))
          buttonCheck(userScenario.commonExpectedResults.returnToEmploymentSummaryText, returnToEmploymentSummarySelector)
        }

        "return a fully populated page with correct paragraph text when there are multiple employments and its end of year" which {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val commonResults = userScenario.commonExpectedResults
          val specificResults = userScenario.specificExpectedResults.get

          val htmlFormat = underTest(taxYearEOY, anExpensesViewModel.copy(isUsingCustomerData = false), isInYear = false)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear - 1))
          buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(CheckEmploymentExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${userScenario.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", EmploymentExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${userScenario.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", BusinessTravelOvernightExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£200", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${userScenario.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", TravelAndOvernightAmountController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${userScenario.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", UniformsOrToolsExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.flatRateJobExpensesAmount, "£300", summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${userScenario.commonExpectedResults.changeText} ${specificResults.flatRateAmountHiddenText}", UniformsOrToolsExpensesAmountController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.yes, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${userScenario.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", ProfessionalFeesAndSubscriptionsExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.professionalSubscriptionsAmount, "£400", summaryListRowFieldNameSelector(7), summaryListRowFieldAmountSelector(7),
            changeLinkSelector(7), s"${userScenario.commonExpectedResults.changeText} ${specificResults.profSubscriptionsAmountHiddenText}", ProfFeesAndSubscriptionsExpensesAmountController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.yes, summaryListRowFieldNameSelector(8), summaryListRowFieldAmountSelector(8),
            changeLinkSelector(8), s"${userScenario.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", OtherEquipmentController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesAmount, "£600", summaryListRowFieldNameSelector(9), summaryListRowFieldAmountSelector(9),
            changeLinkSelector(9), s"${userScenario.commonExpectedResults.changeText} ${specificResults.otherEquipmentAmountHiddenText}", OtherEquipmentAmountController.submit(taxYearEOY).url)
        }

        "return a partly populated page with only relevant data and the others default to 'No' at the end of the year " which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val commonResults = userScenario.commonExpectedResults
          val specificResults = userScenario.specificExpectedResults.get

          val htmlFormat = underTest(taxYearEOY, anExpensesViewModel.copy(
            claimingEmploymentExpenses = true,
            jobExpensesQuestion = Some(true),
            jobExpenses = Some(2),
            flatRateJobExpensesQuestion = Some(false),
            flatRateJobExpenses = None,
            professionalSubscriptionsQuestion = Some(false),
            professionalSubscriptions = None,
            otherAndCapitalAllowancesQuestion = Some(false),
            otherAndCapitalAllowances = None,
            businessTravelCosts = Some(1),
            hotelAndMealExpenses = None,
            vehicleExpenses = None,
            mileageAllowanceRelief = None,
            submittedOn = None,
            isUsingCustomerData = false), isInYear = false)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear - 1))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedContent, contentSelector)
          welshToggleCheck(userScenario.isWelsh)
          buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(CheckEmploymentExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)

          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.yes, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${userScenario.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", EmploymentExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.jobExpensesQuestion, commonResults.yes, summaryListRowFieldNameSelector(2), summaryListRowFieldAmountSelector(2),
            changeLinkSelector(2), s"${userScenario.commonExpectedResults.changeText} ${specificResults.jobExpensesHiddenText}", BusinessTravelOvernightExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.jobExpensesAmount, "£2", summaryListRowFieldNameSelector(3), summaryListRowFieldAmountSelector(3),
            changeLinkSelector(3), s"${userScenario.commonExpectedResults.changeText} ${specificResults.jobExpensesAmountHiddenText}", TravelAndOvernightAmountController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.flatRateJobExpensesQuestion, commonResults.no, summaryListRowFieldNameSelector(4), summaryListRowFieldAmountSelector(4),
            changeLinkSelector(4), s"${userScenario.commonExpectedResults.changeText} ${specificResults.flatRateHiddenText}", UniformsOrToolsExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.professionalSubscriptionsQuestion, commonResults.no, summaryListRowFieldNameSelector(5), summaryListRowFieldAmountSelector(5),
            changeLinkSelector(5), s"${userScenario.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", ProfessionalFeesAndSubscriptionsExpensesController.submit(taxYearEOY).url)
          changeAmountRowCheck(commonResults.otherAndCapitalAllowancesQuestion, commonResults.no, summaryListRowFieldNameSelector(6), summaryListRowFieldAmountSelector(6),
            changeLinkSelector(6), s"${userScenario.commonExpectedResults.changeText} ${specificResults.otherEquipmentHiddenText}", OtherEquipmentController.submit(taxYearEOY).url)
        }

        "show an empty view when there is no prior data at the end of the year" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val commonResults = userScenario.commonExpectedResults
          val specificResults = userScenario.specificExpectedResults.get

          val htmlFormat = underTest(taxYearEOY, anExpensesViewModel.copy(
            claimingEmploymentExpenses = false,
            jobExpensesQuestion = None,
            jobExpenses = None,
            flatRateJobExpensesQuestion = None,
            flatRateJobExpenses = None,
            professionalSubscriptionsQuestion = None,
            professionalSubscriptions = None,
            otherAndCapitalAllowancesQuestion = None,
            otherAndCapitalAllowances = None,
            businessTravelCosts = None,
            hotelAndMealExpenses = None,
            vehicleExpenses = None,
            mileageAllowanceRelief = None,
            isUsingCustomerData = false), isInYear = false)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(specificResults.expectedTitle, userScenario.isWelsh)
          h1Check(specificResults.expectedH1)
          captionCheck(commonResults.expectedCaption(taxYear - 1))
          welshToggleCheck(userScenario.isWelsh)
          changeAmountRowCheck(commonResults.employmentExpenses, commonResults.no, summaryListRowFieldNameSelector(1), summaryListRowFieldAmountSelector(1),
            changeLinkSelector(1), s"${userScenario.commonExpectedResults.changeText} ${specificResults.expensesHiddenText}", EmploymentExpensesController.submit(taxYearEOY).url)
        }
      }
    }
  }
}
