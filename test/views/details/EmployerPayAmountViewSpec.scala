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

package views.details

import controllers.details.routes.EmployerPayAmountController
import forms.details.EmploymentDetailsFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.details.EmployerPayAmountView

class qEmployerPayAmountViewSpec extends ViewUnitTest {

  private val employerName: String = "maggie"
  private val amount: BigDecimal = 100
  private val employmentId = "employmentId"

  object Selectors {
    val contentSelector = "#main-content > div > div > p.govuk-body"
    val hintTestSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#amount"
  }

  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedContentNewAccount: String
    val emptyErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val hintText: String
    val wrongFormatErrorText: String
    val maxAmountErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment details for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £193.52"
    val wrongFormatErrorText: String = "Enter the amount paid in the correct format"
    val maxAmountErrorText: String = "The amount paid must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val continueButtonText = "Yn eich blaen"
    val hintText = "Er enghraifft, £193.52"
    val wrongFormatErrorText: String = "Nodwch y swm a dalwyd yn y fformat cywir"
    val maxAmountErrorText: String = "Maeín rhaid iír swm a dalwyd fod yn llai na £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "How much did maggie pay you?"
    val expectedTitle: String = "How much did your employer pay you?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentNewAccount: String = "Enter the gross amount. This can usually be found on your P60."
    val emptyErrorText: String = "Enter the amount you were paid"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "How much did maggie pay your client?"
    val expectedTitle: String = "How much did your client’s employer pay them?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentNewAccount: String = "Enter the gross amount. This can usually be found on your client’s P60."
    val emptyErrorText: String = "Enter the amount your client was paid"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "Faint y gwnaeth maggie ei dalu i chi?"
    val expectedTitle: String = "Faint y gwnaeth eich cyflogwr ei dalu i chi?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedContentNewAccount: String = "Nodwch y swm gros. Mae hwn iíw weld fel arfer ar eich P60."
    val emptyErrorText: String = "Nodwch swm a dalwyd i chi"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "Faint y gwnaeth maggie ei dalu iích cleient?"
    val expectedTitle: String = "Faint y gwnaeth cyflogwr eich cleient ei dalu iddo?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedContentNewAccount: String = "Nodwch y swm gros. Fel arfer, mae hwn i’w weld ar P60 eich cleient."
    val emptyErrorText: String = "Nodwch y swm a dalwyd iích cleient"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val formProvider = new EmploymentDetailsFormsProvider()

  private val underTest = inject[EmployerPayAmountView]

  userScenarios.foreach { user =>
    import Selectors._
    import user.commonExpectedResults._
    import user.specificExpectedResults._

    s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
      "render How much did xxx pay you? page where preAmount is None and the form is empty" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, formProvider.employerPayAmountForm(user.isAgent), employerName, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(user.isWelsh)

        titleCheck(get.expectedTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContentNewAccount, contentSelector)
        textOnPageCheck(hintText, hintTestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }

      "render How much did xxx pay you? page with an amount in the paragraph text when preAmount is defined" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, formProvider.employerPayAmountForm(user.isAgent), employerName, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(user.isWelsh)

        titleCheck(get.expectedTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(hintText, hintTestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }

      "render How much did xxx pay you? page where the form is filled and an amount in the paragraph text when preAmount is defined" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, formProvider.employerPayAmountForm(user.isAgent).fill(amount), employerName, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(user.isWelsh)

        titleCheck(get.expectedTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(hintText, hintTestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "100")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }

      "render How much did xxx pay you? page with an error message when an empty form is submitted" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, formProvider.employerPayAmountForm(user.isAgent).bind(Map("amount" -> "")), employerName, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(user.isWelsh)

        titleCheck(get.expectedErrorTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContentNewAccount, contentSelector)
        textOnPageCheck(hintText, hintTestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)

        errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(get.emptyErrorText, Some("amount"))
      }

      "render How much did xxx pay you? page with an error message when an a is submitted with an invalid amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, formProvider.employerPayAmountForm(user.isAgent).bind(Map("amount" -> "abc123")), employerName, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(user.isWelsh)

        titleCheck(get.expectedErrorTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContentNewAccount, contentSelector)
        textOnPageCheck(hintText, hintTestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "abc123")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)

        errorSummaryCheck(wrongFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(wrongFormatErrorText, Some("amount"))
      }

      "render How much did xxx pay you? page with an error message when an a is submitted with an amount over the max limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val overMaxAmount = "100,000,000,000"
        val htmlFormat = underTest(taxYearEOY, formProvider.employerPayAmountForm(user.isAgent).bind(Map("amount" -> overMaxAmount)), employerName, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(user.isWelsh)

        titleCheck(get.expectedErrorTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContentNewAccount, contentSelector)
        textOnPageCheck(hintText, hintTestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, overMaxAmount)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)

        errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(maxAmountErrorText, Some("amount"))
      }
    }
  }
}
