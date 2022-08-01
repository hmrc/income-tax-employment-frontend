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

package views.benefits.travel

import forms.AmountForm
import forms.benefits.travel.TravelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.travel.IncidentalCostsBenefitsAmountView

class IncidentalCostsBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"
  private val prefilledAmount: BigDecimal = 200
  private val amountInputName = "amount"

  object Selectors {
    val optionalParagraphSelector = "#main-content > div > div > p"
    val hintTextSelector = "#amount-hint"
    val currencyPrefixSelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val inputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
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
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"

    val expectedHintText = "Er enghraifft, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much did you get in total for incidental overnight costs?"
    val expectedHeading = "How much did you get in total for incidental overnight costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount you got for incidental overnight costs"
    val expectedErrorIncorrectFormat = "Enter the amount you got for incidental overnight costs in the correct format"
    val expectedErrorOverMaximum = "Your incidental overnight costs must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint y cawsoch i gyd ar gyfer m‚n gostau dros nos?"
    val expectedHeading = "Faint y cawsoch i gyd ar gyfer m‚n gostau dros nos?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch y swm a gawsoch ar gyfer m‚n gostau dros nos"
    val expectedErrorIncorrectFormat = "Nodwch y swm a gawsoch chi ar gyfer m‚n gostau dros nos yn y fformat cywir"
    val expectedErrorOverMaximum = "Maeín rhaid iích m‚n gostau dros nos fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much did your client get in total for incidental overnight costs?"
    val expectedHeading = "How much did your client get in total for incidental overnight costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount your client got for incidental overnight costs"
    val expectedErrorIncorrectFormat = "Enter the amount your client got for incidental overnight costs in the correct format"
    val expectedErrorOverMaximum = "Your client’s incidental overnight costs must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint y cafodd eich cleient i gyd ar gyfer m‚n gostau dros nos?"
    val expectedHeading = "Faint y cafodd eich cleient i gyd ar gyfer m‚n gostau dros nos?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch y swm a gafodd eich cleient ar gyfer m‚n gostau dros nos"
    val expectedErrorIncorrectFormat = "Nodwch y swm a gafodd eich cleient ar gyfer m‚n gostau dros nos yn y fformat cywir"
    val expectedErrorOverMaximum = "Maeín rhaid i f‚n gostau dros nos eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  private def form(isAgent: Boolean): Form[BigDecimal] = new TravelFormsProvider().incidentalCostsBenefitsAmountForm(isAgent)

  private lazy val underTest = inject[IncidentalCostsBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the 'incidental overnight expenses amount' page with no pre-filled amount field" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        elementsNotOnPageCheck(optionalParagraphSelector)
        hintTextCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.travel.routes.IncidentalCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the 'incidental overnight expenses amount' page with the amount field pre-filled when there's cya data and prior benefits exist" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).fill(prefilledAmount), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        hintTextCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, prefilledAmount.toString())
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.travel.routes.IncidentalCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render the 'incidental overnight expenses amount' page with the amount field pre-filled when there's no cya data and prior benefits exist" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        hintTextCheck(expectedHintText, hintTextSelector)
        textOnPageCheck(currencyPrefix, currencyPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.travel.routes.IncidentalCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error" when {
        "a form is submitted with no entry" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          hintTextCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(continueButtonText, continueButtonSelector)
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, inputSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, Some(amountInputName))
          formPostLinkCheck(controllers.benefits.travel.routes.IncidentalCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

          welshToggleCheck(userScenario.isWelsh)
        }

        "a form is submitted with an incorrect format" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "abc")), employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          hintTextCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "abc")
          buttonCheck(continueButtonText, continueButtonSelector)
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, inputSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountInputName))
          formPostLinkCheck(controllers.benefits.travel.routes.IncidentalCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

          welshToggleCheck(userScenario.isWelsh)
        }

        "a form is submitted and the amount is over the maximum limit" which {
          val overMaxAmount = "100,000,000,000,000,000,000"
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> overMaxAmount)), employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          hintTextCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, overMaxAmount)
          buttonCheck(continueButtonText, continueButtonSelector)
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, inputSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, Some(amountInputName))
          formPostLinkCheck(controllers.benefits.travel.routes.IncidentalCostsBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)

          welshToggleCheck(userScenario.isWelsh)
        }
      }
    }
  }
}
