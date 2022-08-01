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

package views.benefits.fuel

import controllers.benefits.fuel.routes.CarFuelBenefitsAmountController
import forms.AmountForm
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import views.html.benefits.fuel.CarFuelBenefitsAmountView

class CarFuelBenefitsAmountViewSpec extends ViewUnitTest {

  private val poundPrefixText = "£"
  private val amountInputName = "amount"
  private val employmentId = anEmploymentSource.employmentId
  private val carFuelAmount: BigDecimal = 200


  object Selectors {
    def paragraphTextSelector(index: Int): String = s"#main-content > div > div > p:nth-child($index)"

    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#amount"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedContent: String
    val emptyErrorText: String
    val wrongFormatErrorText: String
    val maxAmountErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val hintText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £193.52"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val continueButtonText = "Yn eich blaen"
    val hintText = "Er enghraifft, £193.52"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "How much was your total company car fuel benefit?"
    val expectedTitle: String = "How much was your total company car fuel benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your P11D form in section F, box 10."
    val emptyErrorText: String = "Enter your company car fuel benefit amount"
    val wrongFormatErrorText: String = "Enter your company car fuel benefit amount in the correct format"
    val maxAmountErrorText: String = "Your company car fuel benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "How much was your client’s total company car fuel benefit?"
    val expectedTitle: String = "How much was your client’s total company car fuel benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your client’s P11D form in section F, box 10."
    val emptyErrorText: String = "Enter your client’s company car fuel benefit amount"
    val wrongFormatErrorText: String = "Enter your client’s company car fuel benefit amount in the correct format"
    val maxAmountErrorText: String = "Your client’s company car fuel benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "Faint oedd cyfanswm eich buddiant tanwydd ar gyfer car cwmni?"
    val expectedTitle: String = "Faint oedd cyfanswm eich buddiant tanwydd ar gyfer car cwmni?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedContent: String = "Maeír wybodaeth hon ar gael yn adran F, blwch 10 ar eich ffurflen P11D."
    val emptyErrorText: String = "Nodwch swm eich buddiant tanwydd ar gyfer car cwmni"
    val wrongFormatErrorText: String = "Nodwch swm eich buddiant tanwydd ar gyfer car cwmni yn y fformat cywir"
    val maxAmountErrorText: String = "Maeín rhaid i fuddiant tanwydd ar gyfer eich car cwmni fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "Faint oedd cyfanswm y buddiant tanwydd ar gyfer car cwmni eich cleient?"
    val expectedTitle: String = "Faint oedd cyfanswm y buddiant tanwydd ar gyfer car cwmni eich cleient?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedContent: String = "Maeír wybodaeth hon ar gael yn adran F, blwch 10 ar ffurflen P11D eich cleient."
    val emptyErrorText: String = "Nodwch swm buddiant tanwydd ar gyfer car cwmni eich cleient"
    val wrongFormatErrorText: String = "Nodwch swm buddiant tanwydd ar gyfer car cwmni eich cleient yn y fformat cywir"
    val maxAmountErrorText: String = "Maeín rhaid i fuddiant tanwydd ar gyfer car cwmni eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new FuelFormsProvider().carFuelAmountForm(isAgent)

  private lazy val underTest = inject[CarFuelBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    import userScenario.specificExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render amount page with no prefilled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedTitle, userScenario.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContent, paragraphTextSelector(index = 2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(CarFuelBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedTitle, userScenario.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContent, paragraphTextSelector(index = 2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(CarFuelBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with an error when theres no input" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContent, paragraphTextSelector(index = 3))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(CarFuelBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
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
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContent, paragraphTextSelector(index = 3))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(CarFuelBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(get.wrongFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(get.wrongFormatErrorText)
      }

      "render page with error when a form is submitted and the amount is over the maximum limit" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContent, paragraphTextSelector(index = 3))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "100,000,000,000")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(CarFuelBenefitsAmountController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(get.maxAmountErrorText)
      }
    }
  }
}
