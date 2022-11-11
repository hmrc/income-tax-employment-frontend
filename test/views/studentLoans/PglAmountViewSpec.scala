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

package views.studentLoans

import forms.studentLoans.StudentLoansFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.studentLoans.PglAmountView

class PglAmountViewSpec extends ViewUnitTest {

  private val employerName = "Whiterun Guards"
  val employmentId: String = "1234567890-0987654321"

  val pglDeductionAmount: BigDecimal = 200

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val inputSelector = "#amount"
    val hintTextSelector = "#amount-hint"
    val paragraphSelector = "#main-content > div > div > p"
    val headingSelector = "#main-content > div > div > header > h1"
    val errorSummarySelector = "#main-content > div > div > div.govuk-error-summary > div > ul > li > a"
    val errorMessageSelector = "#amount-error"
  }

  trait CommonExpectedResults {
    val title: String
    val expectedH1: String
    val expectedCaption: String
    val expectedButtonText: String
    val expectedParagraphText: String
    val hintText: String
    val inputFieldName: String
    val errorSummaryText: String
    val noEntryError: String
    val invalidFormatError: String
    val maxAmountExceededError: String
    val expectedErrorTitle: String
  }

  object ExpectedResultsIndividualEN extends CommonExpectedResults {
    override val expectedCaption: String = s"Student loans for 6 April ${taxYearEOY-1} to 5 April $taxYearEOY"
    override val expectedButtonText: String = "Continue"
    override val hintText: String = "For example, £193.52"
    override val title: String = "How much postgraduate loan did you repay?"
    override val expectedH1: String = "How much postgraduate loan did you repay while employed by Whiterun Guards?"
    override val expectedParagraphText: String = "You can check your payslips or P60 for repayments made."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Enter the amount of postgraduate loan you repaid while employed by Whiterun Guards"
    override val noEntryError: String = "Enter the amount of postgraduate loan you repaid while employed by Whiterun Guards"
    override val invalidFormatError: String = "Enter the amount of postgraduate loan in the correct format"
    override val maxAmountExceededError: String = "Enter an amount less than £100,000,000,000"
    override val expectedErrorTitle: String = s"Error: $title"
  }

  object ExpectedResultsIndividualCY extends CommonExpectedResults {
    override val expectedCaption: String = s"Benthyciadau Myfyrwyr ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    override val expectedButtonText: String = "Yn eich blaen"
    override val hintText: String = "Er enghraifft, £193.52"
    override val title: String = "Faint o fenthyciad ôl-raddedig a wnaethoch ei ad-dalu?"
    override val expectedH1: String = "Faint o fenthyciad ôl-raddedig a wnaethoch ei ad-dalu tra’ch bod wedi’ch cyflogi gan Whiterun Guards?"
    override val expectedParagraphText: String = "Gallwch wirio’ch slipiau cyflog neu’ch P60 ar gyfer ad-daliadau a wnaed."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Nodwch swm y benthyciad ôl-raddedig a ad-dalwyd gennych tra’ch bod wedi’ch cyflogi gan Whiterun Guards"
    override val noEntryError: String = "Nodwch swm y benthyciad ôl-raddedig a ad-dalwyd gennych tra’ch bod wedi’ch cyflogi gan Whiterun Guards"
    override val invalidFormatError: String = "Nodwch swm y benthyciad ôl-raddedig yn y fformat cywir"
    override val maxAmountExceededError: String = "Nodwch swm sy’n llai na £100,000,000,000"
    override val expectedErrorTitle: String = s"Gwall: $title"
  }

  object ExpectedResultsAgentEN extends CommonExpectedResults {
    override val expectedCaption: String = s"Student loans for 6 April ${taxYearEOY-1} to 5 April $taxYearEOY"
    override val expectedButtonText: String = "Continue"
    override val hintText: String = "For example, £193.52"
    override val title: String = "How much postgraduate loan did your client repay?"
    override val expectedH1: String = "How much postgraduate loan did your client repay while employed by Whiterun Guards?"
    override val expectedParagraphText: String = "You can check your client’s payslips or P60 for repayments made."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Enter the amount of postgraduate loan your client repaid while employed by Whiterun Guards"
    override val noEntryError: String = "Enter the amount of postgraduate loan your client repaid while employed by Whiterun Guards"
    override val invalidFormatError: String = "Enter the amount of postgraduate loan in the correct format"
    override val maxAmountExceededError: String = "Enter an amount less than £100,000,000,000"
    override val expectedErrorTitle: String = s"Error: $title"
  }

  object ExpectedResultsAgentCY extends CommonExpectedResults {
    override val expectedCaption: String = s"Benthyciadau Myfyrwyr ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    override val expectedButtonText: String = "Yn eich blaen"
    override val hintText: String = "Er enghraifft, £193.52"
    override val title: String = "Faint o fenthyciad ôl-raddedig a wnaeth eich cleient ei ad-dalu?"
    override val expectedH1: String = "Faint o fenthyciad ôl-raddedig a wnaeth eich cleient ei ad-dalu tra ei fod wedi’i gyflogi gan Whiterun Guards?"
    override val expectedParagraphText: String = "Gallwch wirio slipiau cyflog neu P60 eich cleient ar gyfer ad-daliadau a wnaed."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Nodwch swm y benthyciad ôl-raddedig a ad-dalwyd gan eich cleient tra ei fod wedi’i gyflogi gan Whiterun Guards"
    override val noEntryError: String = "Nodwch swm y benthyciad ôl-raddedig a ad-dalwyd gan eich cleient tra ei fod wedi’i gyflogi gan Whiterun Guards"
    override val invalidFormatError: String = "Nodwch swm y benthyciad ôl-raddedig yn y fformat cywir"
    override val maxAmountExceededError: String = "Nodwch swm sy’n llai na £100,000,000,000"
    override val expectedErrorTitle: String = s"Gwall: $title"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedResultsIndividualEN),
    UserScenario(isWelsh = false, isAgent = true, ExpectedResultsAgentEN),
    UserScenario(isWelsh = true, isAgent = false, ExpectedResultsIndividualCY),
    UserScenario(isWelsh = true, isAgent = true, ExpectedResultsAgentCY)
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new StudentLoansFormsProvider().pglAmountForm(employerName = employerName, isAgent)

  private lazy val underTest = inject[PglAmountView]

  userScenarios.foreach { scenarioData =>

    s"The language is ${welshTest(scenarioData.isWelsh)} and the request is from an ${scenarioData.isAgent}" should {

      "render the postgraduate amount page" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(scenarioData.isAgent), employmentId, employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(scenarioData.commonExpectedResults.title, scenarioData.isWelsh)
        h1Check(scenarioData.commonExpectedResults.expectedH1)
        captionCheck(scenarioData.commonExpectedResults.expectedCaption)
        textOnPageCheck(scenarioData.commonExpectedResults.expectedParagraphText, Selectors.paragraphSelector)
        textOnPageCheck(scenarioData.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck(scenarioData.commonExpectedResults.inputFieldName, Selectors.inputSelector, "")

        buttonCheck(scenarioData.commonExpectedResults.expectedButtonText, Selectors.continueButtonSelector)
      }

      "render the undergraduate amount page when the form has been pre filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(scenarioData.isAgent).fill(value = pglDeductionAmount), employmentId, employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(scenarioData.commonExpectedResults.title, scenarioData.isWelsh)
        h1Check(scenarioData.commonExpectedResults.expectedH1)
        captionCheck(scenarioData.commonExpectedResults.expectedCaption)
        textOnPageCheck(scenarioData.commonExpectedResults.expectedParagraphText, Selectors.paragraphSelector)
        textOnPageCheck(scenarioData.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("amount", Selectors.inputSelector, pglDeductionAmount.toString())

        buttonCheck(scenarioData.commonExpectedResults.expectedButtonText, Selectors.continueButtonSelector)
      }

      "render the undergraduate loans repayment amount page when there is an invalid format entry in the amount field" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(scenarioData.isAgent).bind(Map("amount" -> "abc")), employmentId, employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(scenarioData.commonExpectedResults.expectedErrorTitle, scenarioData.isWelsh)
        h1Check(scenarioData.commonExpectedResults.expectedH1)
        captionCheck(scenarioData.commonExpectedResults.expectedCaption)
        textOnPageCheck(scenarioData.commonExpectedResults.expectedParagraphText, Selectors.paragraphSelector)
        textOnPageCheck(scenarioData.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("amount", Selectors.inputSelector, "abc")

        buttonCheck(scenarioData.commonExpectedResults.expectedButtonText, Selectors.continueButtonSelector)

        errorSummaryCheck(scenarioData.commonExpectedResults.invalidFormatError, Selectors.inputSelector)
        errorAboveElementCheck(scenarioData.commonExpectedResults.invalidFormatError)
      }

      "render the undergraduate loans repayment amount page when there the entry is too large in the amount field" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(scenarioData.isAgent).bind(Map("amount" -> "100,000,000,000")), employmentId, employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(scenarioData.commonExpectedResults.expectedErrorTitle, scenarioData.isWelsh)
        h1Check(scenarioData.commonExpectedResults.expectedH1)
        captionCheck(scenarioData.commonExpectedResults.expectedCaption)
        textOnPageCheck(scenarioData.commonExpectedResults.expectedParagraphText, Selectors.paragraphSelector)
        textOnPageCheck(scenarioData.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("amount", Selectors.inputSelector, "100,000,000,000")

        buttonCheck(scenarioData.commonExpectedResults.expectedButtonText, Selectors.continueButtonSelector)

        errorSummaryCheck(scenarioData.commonExpectedResults.maxAmountExceededError, Selectors.inputSelector)
        errorAboveElementCheck(scenarioData.commonExpectedResults.maxAmountExceededError)
      }

      "render the undergraduate loans repayment amount page when there is no entry in the amount field" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(scenarioData.isAgent).bind(Map("amount" -> "")), employmentId, employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(scenarioData.commonExpectedResults.expectedErrorTitle, scenarioData.isWelsh)
        h1Check(scenarioData.commonExpectedResults.expectedH1)
        captionCheck(scenarioData.commonExpectedResults.expectedCaption)
        textOnPageCheck(scenarioData.commonExpectedResults.expectedParagraphText, Selectors.paragraphSelector)
        textOnPageCheck(scenarioData.commonExpectedResults.hintText, Selectors.hintTextSelector)
        inputFieldValueCheck("amount", Selectors.inputSelector, "")

        buttonCheck(scenarioData.commonExpectedResults.expectedButtonText, Selectors.continueButtonSelector)

        errorSummaryCheck(scenarioData.commonExpectedResults.noEntryError, Selectors.inputSelector)
        errorAboveElementCheck(scenarioData.commonExpectedResults.noEntryError)
      }
    }
  }
}
