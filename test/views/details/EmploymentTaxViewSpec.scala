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

import forms.details.EmploymentDetailsFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.details.EmploymentTaxView

class EmploymentTaxViewSpec extends ViewUnitTest {

  object Selectors {
    val pText = "#main-content > div > div > p.govuk-body"
    val hintText = "#amount-hint"
    val continueButton = "#continue"
    val inputAmountField = "#amount"
  }

  private val employerName = "maggie"
  private val employmentId = "employmentId"
  private val amountInputName = "amount"

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hint: String
    val continue: String
    val expectedPTextWithData: String
    val expectedErrorInvalidFormat: String
    val expectedErrorMaxLimit: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val expectedPTextNoData: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hint: String = "For example, £193.52"
    val continue: String = "Continue"
    val expectedPTextWithData: String = s"If £200 was not taken in UK tax, tell us the correct amount."
    val expectedErrorInvalidFormat = "Enter the amount of UK tax in the correct format"
    val expectedErrorMaxLimit = "The amount of UK tax must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val hint: String = "Er enghraifft, £193.52"
    val continue: String = "Yn eich blaen"
    val expectedPTextWithData: String = s"Os na chafodd £200 ei thynnu fel treth y DU, rhowch wybod i ni beth ywír swm cywir."
    val expectedErrorInvalidFormat = "Nodwch y swm o dreth y DU yn y fformat cywir"
    val expectedErrorMaxLimit = "Maeín rhaid iír swm o dreth y DU fod yn llai na £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your earnings?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of UK tax taken from your earnings"
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your P60."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your client’s maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your client’s earnings?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of UK tax taken from your client’s earnings"
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your client’s P60."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = s"Faint o dreth y DU a gafodd ei thynnu oích enillion maggie?"
    val expectedTitle: String = s"Faint o dreth y DU a gafodd ei thynnu oích enillion?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch y swm o dreth y DU a dynnwyd oích enillion"
    val expectedPTextNoData: String = "Fel arfer, maeír swm hwn iíw weld yn adran ëManylion Cyflog a Threth Incwmí eich P60."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = s"Faint o dreth y DU a gafodd ei thynnu o enillion maggie eich cleient?"
    val expectedTitle: String = s"Faint o dreth y DU a gafodd ei thynnu o enillion eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch y swm o dreth y DU a dynnwyd o enillion eich cleient"
    val expectedPTextNoData: String = "Fel arfer, maeír swm hwn iíw weld yn yr adran ëManylion Cyflog a Threth Incwmí ar P60 eich cleient."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val formProvider = new EmploymentDetailsFormsProvider()
  private val underTest = inject[EmploymentTaxView]

  userScenarios.foreach { user =>
    import Selectors._
    import user.commonExpectedResults._
    import user.specificExpectedResults._

    s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
      "render the page where there is prior data and the form is empty" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employerName, formProvider.employmentTaxAmountForm(user.isAgent), Some(200))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
        welshToggleCheck(user.isWelsh)
        textOnPageCheck(user.commonExpectedResults.expectedPTextWithData, pText)
        buttonCheck(user.commonExpectedResults.continue, continueButton)
        textOnPageCheck(user.commonExpectedResults.hint, hintText)
        inputFieldValueCheck(amountInputName, inputAmountField, "")
      }

      "render the page where amount is None and the form is empty" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employerName, formProvider.employmentTaxAmountForm(user.isAgent), None)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        welshToggleCheck(user.isWelsh)
        textOnPageCheck(get.expectedPTextNoData, pText)
        buttonCheck(continue, continueButton)
        textOnPageCheck(hint, hintText)
        inputFieldValueCheck(amountInputName, inputAmountField, "")
      }

      "render the page with a pre-filled form when the cya data (form amount) and prior data are the same" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employerName,
          formProvider.employmentTaxAmountForm(user.isAgent).fill(200), Some(200))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        welshToggleCheck(user.isWelsh)
        textOnPageCheck(user.commonExpectedResults.expectedPTextWithData, pText)
        buttonCheck(continue, continueButton)
        textOnPageCheck(hint, hintText)
        inputFieldValueCheck(amountInputName, inputAmountField, "200")
      }

      "render the page with an error when an empty form is submitted" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employerName,
          formProvider.employmentTaxAmountForm(user.isAgent).bind(Map("amount" -> "")), Some(200))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(user.commonExpectedResults.expectedPTextWithData, pText)
        buttonCheck(user.commonExpectedResults.continue, continueButton)
        textOnPageCheck(user.commonExpectedResults.hint, hintText)
        inputFieldValueCheck(amountInputName, inputAmountField, "")

        errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, inputAmountField)
        errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Some(amountInputName))
        welshToggleCheck(user.isWelsh)
      }

      "render the page with an error when a form is submitted with an invalid amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employerName,
          formProvider.employmentTaxAmountForm(user.isAgent).bind(Map("amount" -> "abc123")), Some(200))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(user.commonExpectedResults.expectedPTextWithData, pText)
        buttonCheck(user.commonExpectedResults.continue, continueButton)
        textOnPageCheck(user.commonExpectedResults.hint, hintText)
        inputFieldValueCheck(amountInputName, inputAmountField, "abc123")

        errorSummaryCheck(expectedErrorInvalidFormat, inputAmountField)
        errorAboveElementCheck(expectedErrorInvalidFormat, Some(amountInputName))
        welshToggleCheck(user.isWelsh)
      }

      "render the page with an error when a form is submitted with an amount over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employerName,
          formProvider.employmentTaxAmountForm(user.isAgent).bind(Map("amount" -> "9999999999999999999999999999")), Some(200))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(user.commonExpectedResults.expectedPTextWithData, pText)
        buttonCheck(user.commonExpectedResults.continue, continueButton)
        textOnPageCheck(user.commonExpectedResults.hint, hintText)
        inputFieldValueCheck(amountInputName, inputAmountField, "9999999999999999999999999999")

        errorSummaryCheck(expectedErrorMaxLimit, inputAmountField)
        errorAboveElementCheck(expectedErrorMaxLimit, Some(amountInputName))
        welshToggleCheck(user.isWelsh)
      }
    }
  }
}
