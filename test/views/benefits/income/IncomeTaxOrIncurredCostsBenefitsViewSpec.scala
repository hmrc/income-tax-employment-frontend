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

package views.benefits.income

import controllers.benefits.income.routes.IncomeTaxOrIncurredCostsBenefitsController
import forms.YesNoForm
import forms.benefits.income.IncomeFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.income.IncomeTaxOrIncurredCostsBenefitsView

class IncomeTaxOrIncurredCostsBenefitsViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did your employer pay any of your Income Tax or incurred costs?"
    val expectedH1 = "Did your employer pay any of your Income Tax or incurred costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your employer paid any of your Income Tax or incurred costs"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth eich cyflogwr dalu unrhyw gyfran o’ch Treth Incwm neu gostau a ysgwyddwyd?"
    val expectedH1 = "A wnaeth eich cyflogwr dalu unrhyw gyfran o’ch Treth Incwm neu gostau a ysgwyddwyd?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os talodd eich cyflogwr unrhyw gyfran o’ch Treth Incwm neu gostau a ysgwyddwyd"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s employer pay any of their Income Tax or incurred costs?"
    val expectedH1 = "Did your client’s employer pay any of their Income Tax or incurred costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client’s employer paid any of their Income Tax or incurred costs"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth cyflogwr eich cleient dalu unrhyw gyfran o’i Dreth Incwm neu gostau a ysgwyddwyd?"
    val expectedH1 = "A wnaeth cyflogwr eich cleient dalu unrhyw gyfran o’i Dreth Incwm neu gostau a ysgwyddwyd?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os talodd cyflogwr eich cleient unrhyw gyfran o’i Dreth Incwm neu gostau a ysgwyddwyd"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
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

  private def form(isAgent: Boolean): Form[Boolean] = new IncomeFormsProvider().incomeTaxOrIncurredCostsForm(isAgent)

  private lazy val underTest = inject[IncomeTaxOrIncurredCostsBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(IncomeTaxOrIncurredCostsBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'yes' radio button prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        radioButtonCheck(yesText, radioNumber = 1, checked = true)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(IncomeTaxOrIncurredCostsBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'no' radio button prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = false), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(IncomeTaxOrIncurredCostsBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when a user submits an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(IncomeTaxOrIncurredCostsBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
      }
    }
  }
}
