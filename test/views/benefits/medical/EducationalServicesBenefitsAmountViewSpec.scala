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

package views.benefits.medical

import controllers.benefits.medical.routes.EducationalServicesBenefitsAmountController
import forms.AmountForm
import forms.benefits.medical.MedicalFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.medical.EducationalServicesBenefitsAmountView

class EducationalServicesBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  object Selectors {
    val contentSelector = "#main-content > div > div > p"
    val hintTextSelector = "#amount-hint"
    val inputSelector = "#amount"
    val poundPrefixSelector = ".govuk-input__prefix"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#amount"
  }


  trait CommonExpectedResults {
    val expectedCaption: String
    val amountHint: String
    val continue: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val amountHint: String = "For example, £193.52"
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "Er enghraifft, £193.52"
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val continue = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for educational services?"
    val expectedHeading: String = "How much did you get in total for educational services?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount you got for educational services"
    val invalidFormatErrorText: String = "Enter the amount you got for educational services in the correct format"
    val maxAmountErrorText: String = "Your educational services benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y cawsoch i gydaddysg ar gyfer gwasanaethau addysg?"
    val expectedHeading: String = "Faint y cawsoch i gydaddysg ar gyfer gwasanaethau addysg?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch y swm a gawsoch ar gyfer gwasanaethau addysg"
    val invalidFormatErrorText: String = "Nodwch y swm a gawsoch ar gyfer gwasanaethau addysg yn y fformat cywir"
    val maxAmountErrorText: String = "Mae’n rhaid i’ch buddiant ar gyfer gwasanaethau addysg fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for educational services?"
    val expectedHeading: String = "How much did your client get in total for educational services?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount your client got for educational services"
    val invalidFormatErrorText: String = "Enter the amount your client got for educational services in the correct format"
    val maxAmountErrorText: String = "Your client’s educational services benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y cafodd eich cleient i gyd ar gyfer gwasanaethau addysg?"
    val expectedHeading: String = "Faint y cafodd eich cleient i gyd ar gyfer gwasanaethau addysg?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch y swm a gafodd eich cleient ar gyfer gwasanaethau addysg"
    val invalidFormatErrorText: String = "Nodwch y swm a gafodd eich cleient ar gyfer gwasanaethau addysg yn y fformat cywir"
    val maxAmountErrorText: String = "Mae’n rhaid i fuddiant eich cleient ar gyfer gwasanaethau addysg fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new MedicalFormsProvider().educationalServicesAmountForm(isAgent)

  private lazy val underTest = inject[EducationalServicesBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    import userScenario.specificExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render amount page with not prefilled form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        elementNotOnPageCheck(contentSelector)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(EducationalServicesBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there is previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "300")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "300")
        buttonCheck(continue, continueButtonSelector)
        formPostLinkCheck(EducationalServicesBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "should render the amount page with empty value error text when there is no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(EducationalServicesBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(get.emptyErrorText)
      }

      "render page with error when a form is submitted with incorrectly formatted amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(EducationalServicesBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(get.invalidFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(get.invalidFormatErrorText)
      }

      "render page with error when a form is submitted and the amount is over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption)
        textOnPageCheck(userScenario.commonExpectedResults.amountHint, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "100,000,000,000")
        buttonCheck(userScenario.commonExpectedResults.continue, continueButtonSelector)
        formPostLinkCheck(EducationalServicesBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(get.maxAmountErrorText)
      }
    }
  }
}
