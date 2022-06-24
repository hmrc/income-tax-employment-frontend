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

import controllers.expenses.routes.UniformsOrToolsExpensesController
import forms.YesNoForm
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.UniformsOrToolsExpensesView

class UniformsOrToolsExpensesViewSpec extends ViewUnitTest {

  private val uniformsAndToolsLink = "https://www.gov.uk/guidance/job-expenses-for-uniforms-work-clothing-and-tools"

  object Selectors {
    val canClaimParagraphSelector: String = "#can-claim-text"
    val canClaimExample1Selector: String = "#main-content > div > div > ul > li:nth-child(1)"
    val canClaimExample2Selector: String = "#main-content > div > div > ul > li:nth-child(2)"
    val flatRateExpenseParagraphSelector: String = "#flat-rate-expense-text"
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
    val expectedTitle = "A ydych am hawlio ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer?"
    val expectedHeading = "A ydych am hawlio ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer?"
    val expectedCanClaimExample1 = "atgyweirio neu ddisodli m‚n offer sydd eu hangen arnoch i wneud eich gwaith"
    val expectedUniformsAndToolsLink = "Gwiriwch i weld a allwch hawlio treuliau cyfradd unffurf ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer (yn agor tab newydd)."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ i hawlio ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer"
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
    val expectedTitle = "A ydych am hawlio ar gyfer gwisgoedd unffurf, dillad gwaith neu offer ar gyfer eich cleient?"
    val expectedHeading = "A ydych am hawlio ar gyfer gwisgoedd unffurf, dillad gwaith neu offer ar gyfer eich cleient?"
    val expectedCanClaimExample1 = "atgyweirio neu ddisodli m‚n offer sydd eu hangen ar eich cleient i wneud ei waith"
    val expectedUniformsAndToolsLink = "Gwiriwch i weld a all eich cleient hawlio treuliau cyfradd unffurf ar gyfer gwisgoedd unffurf, dillad gwaith, neu offer (yn agor tab newydd)."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ i hawlio ar gyfer gwisgoedd unffurf, dillad gwaith neu offer eich cleient"
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
    val expectedCaption = s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val expectedCanClaim = "Maeín bosibl y gallwch hawlio ar gyfer costau:"
    val expectedCanClaimExample2 = "glanhau, atgyweirio neu ddisodli gwisgoedd unffurf neu ddillad gwaith arbenigol"
    val flatRateExpense = "Maeír treuliau hyn yn cael eu talu ar gyfradd y cytunir arni (ëtraul cyfradd unffurfí neu ëdidyniad sefydlogí)."
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new ExpensesFormsProvider().uniformsWorkClothesToolsForm(isAgent)

  private lazy val underTest = inject[UniformsOrToolsExpensesView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled radio buttons" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(expectedCanClaim, canClaimParagraphSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCanClaimExample1, canClaimExample1Selector)
        textOnPageCheck(expectedCanClaimExample2, canClaimExample2Selector)
        textOnPageCheck(flatRateExpense, flatRateExpenseParagraphSelector)
        linkCheck(userScenario.specificExpectedResults.get.expectedUniformsAndToolsLink, uniformsAndToolsLinkSelector, uniformsAndToolsLink)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(UniformsOrToolsExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'Yes' pre-filled and CYA data exists" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = true), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(expectedCanClaim, canClaimParagraphSelector)
        textOnPageCheck(flatRateExpense, flatRateExpenseParagraphSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCanClaimExample1, canClaimExample1Selector)
        textOnPageCheck(expectedCanClaimExample2, canClaimExample2Selector)
        linkCheck(userScenario.specificExpectedResults.get.expectedUniformsAndToolsLink, uniformsAndToolsLinkSelector, uniformsAndToolsLink)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(UniformsOrToolsExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'No' pre-filled and not a prior submission" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = false), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(expectedCanClaim, canClaimParagraphSelector)
        textOnPageCheck(flatRateExpense, flatRateExpenseParagraphSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCanClaimExample1, canClaimExample1Selector)
        textOnPageCheck(expectedCanClaimExample2, canClaimExample2Selector)
        linkCheck(userScenario.specificExpectedResults.get.expectedUniformsAndToolsLink, uniformsAndToolsLinkSelector, uniformsAndToolsLink)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(UniformsOrToolsExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(expectedCanClaim, canClaimParagraphSelector)
        textOnPageCheck(flatRateExpense, flatRateExpenseParagraphSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCanClaimExample1, canClaimExample1Selector)
        textOnPageCheck(expectedCanClaimExample2, canClaimExample2Selector)
        linkCheck(userScenario.specificExpectedResults.get.expectedUniformsAndToolsLink, uniformsAndToolsLinkSelector, uniformsAndToolsLink)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(UniformsOrToolsExpensesController.submit(taxYearEOY).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText, Some("value"))
      }
    }
  }
}
