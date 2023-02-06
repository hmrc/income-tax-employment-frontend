/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.benefits.fuel.routes.CompanyCarBenefitsAmountController
import forms.AmountForm
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.fuel.CompanyCarBenefitsAmountView

class CompanyCarBenefitsAmountViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"
  private val carAmount: BigDecimal = 100
  private val maxLimit: String = "100,000,000,000"
  private val amountInputName = "amount"

  object Selectors {
    def paragraphTextSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"

    val hintTextSelector = "#amount-hint"
    val inputSelector = "#amount"
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val hintText: String
    val continueButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraphText: String
    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedInvalidFormatErrorMessage: String
    val expectedMaxLengthErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val hintText = "For example, £193.52"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"

    val hintText = "Er enghraifft, £193.52"
    val continueButtonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your total company car benefit?"
    val expectedHeading = "How much was your total company car benefit?"
    val expectedParagraphText = "You can find this information on your P11D form in section F, box 9."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your company car benefit amount"
    val expectedInvalidFormatErrorMessage = "Enter your company car benefit amount in the correct format"
    val expectedMaxLengthErrorMessage = "Your company car benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint oedd cyfanswm eich buddiant car cwmni?"
    val expectedHeading = "Faint oedd cyfanswm eich buddiant car cwmni?"
    val expectedParagraphText = "Mae’r wybodaeth hon ar gael yn adran F, blwch 9 ar eich ffurflen P11D."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch swm eich buddiant car cwmni"
    val expectedInvalidFormatErrorMessage = "Nodwch swm eich buddiant car cwmni yn y fformat cywir"
    val expectedMaxLengthErrorMessage = "Mae’n rhaid i’ch buddiant car cwmni fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s total company car benefit?"
    val expectedHeading = "How much was your client’s total company car benefit?"
    val expectedParagraphText = "You can find this information on your client’s P11D form in section F, box 9."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your client’s company car benefit amount"
    val expectedInvalidFormatErrorMessage = "Enter your client’s company car benefit amount in the correct format"
    val expectedMaxLengthErrorMessage = "Your client’s company car benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint oedd cyfanswm buddiant car cwmni eich cleient?"
    val expectedHeading = "Faint oedd cyfanswm buddiant car cwmni eich cleient?"
    val expectedParagraphText = "Mae’r wybodaeth hon ar gael yn adran F, blwch 9 ar ffurflen P11D eich cleient."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch swm buddiant car cwmni eich cleient"
    val expectedInvalidFormatErrorMessage = "Nodwch swm buddiant car cwmni’ch cleient yn y fformat cywir"
    val expectedMaxLengthErrorMessage = "Mae’n rhaid i fuddiant car cwmni eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[BigDecimal] = new FuelFormsProvider().companyCarAmountForm(isAgent)

  private lazy val underTest = inject[CompanyCarBenefitsAmountView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      import Selectors._
      "render page with no-prefilled amount box" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector(1))
        textOnPageCheck(userScenario.commonExpectedResults.hintText, hintTextSelector)
        inputFieldValueCheck(amountInputName, inputSelector, value = "")
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        formPostLinkCheck(CompanyCarBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilling when there previous amount" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector(index = 1))
        textOnPageCheck(userScenario.commonExpectedResults.hintText, hintTextSelector)
        inputFieldValueCheck(amountInputName, inputSelector, carAmount.toString())
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        formPostLinkCheck(CompanyCarBenefitsAmountController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error" when {
        "a form is submitted with no entry" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(AmountForm.amount -> "")), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, inputSelector)
          inputFieldValueCheck(amountInputName, inputSelector, value = "")

          welshToggleCheck(userScenario.isWelsh)
        }

        "a form is submitted with an incorrectly formatted amount" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(AmountForm.amount -> "123.33.33")), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedInvalidFormatErrorMessage, inputSelector)
          inputFieldValueCheck(amountInputName, inputSelector, value = "123.33.33")

          welshToggleCheck(userScenario.isWelsh)
        }

        "a form is submitted and the amount is over the maximum limit" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000")), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedMaxLengthErrorMessage, inputSelector)
          inputFieldValueCheck(amountInputName, inputSelector, maxLimit)

          welshToggleCheck(userScenario.isWelsh)
        }
      }
    }
  }
}
