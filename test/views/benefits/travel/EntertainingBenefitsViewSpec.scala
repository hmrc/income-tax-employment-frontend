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

import forms.YesNoForm
import forms.benefits.travel.TravelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.travel.EntertainingBenefitsView

class EntertainingBenefitsViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"

  object Selectors {
    val yesSelector = "#value"
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
    val contentSelector = "#main-content > div > div > p"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val yesText: String
    val noText: String
    val continueButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
    val expectedContent: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val yesText = "Yes"
    val noText = "No"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val yesText = "Iawn"
    val noText = "Na"
    val continueButtonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any entertainment benefits?"
    val expectedH1 = "Did you get any entertainment benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got any entertainment benefits"
    val expectedContent = "These are entertainment costs that your employer has paid for, or reimbursed you for. For example, eating, drinking and other hospitality."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch unrhyw fuddiannau gwesteia?"
    val expectedH1 = "A gawsoch unrhyw fuddiannau gwesteia?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cawsoch unrhyw fuddiannau gwesteia"
    val expectedContent = "Costau gwesteia ywír rhain y mae eich cyflogwr wedi talu amdanynt, neu wedi eich ad-dalu amdanynt. Er enghraifft, bwyta, yfed a mathau eraill o letygarwch."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any entertainment benefits?"
    val expectedH1 = "Did your client get any entertainment benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got any entertainment benefits"
    val expectedContent = "These are entertainment costs that their employer has paid for, or reimbursed them for. For example, eating, drinking and other hospitality."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient unrhyw fuddiannau gwesteia?"
    val expectedH1 = "A gafodd eich cleient unrhyw fuddiannau gwesteia?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cawsoch unrhyw fuddiannau gwesteia"
    val expectedContent = "Costau gwesteia ywír rhain y mae ei gyflogwr wedi talu amdanynt, neu wediíu had-dalu amdanynt. Er enghraifft, bwyta, yfed a mathau eraill o letygarwch."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def yesNoForm(isAgent: Boolean): Form[Boolean] = new TravelFormsProvider().entertainingBenefitsForm(isAgent)

  private lazy val underTest = inject[EntertainingBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the 'Did you get any entertainment benefits?' page with no pre-filled radio buttons" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedContent, contentSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.travel.routes.EntertainingBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the 'Did you get any entertainment benefits?' page with cya data and 'Yes' radio selected" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(value = true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedContent, contentSelector)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.travel.routes.EntertainingBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error if no value is submitted" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedContent, contentSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.travel.routes.EntertainingBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
      }
    }
  }
}
