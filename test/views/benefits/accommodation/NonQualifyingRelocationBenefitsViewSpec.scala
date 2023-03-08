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

package views.benefits.accommodation

import controllers.benefits.accommodation.routes.NonQualifyingRelocationBenefitsController
import forms.YesNoForm
import forms.benefits.accommodation.AccommodationFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.benefits.pages.NonQualifyingRelocationBenefitsPageBuilder.aNonQualifyingRelocationBenefitsPage
import views.html.benefits.accommodation.NonQualifyingRelocationBenefitsView

class NonQualifyingRelocationBenefitsViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"

  object Selectors {
    val yesSelector = "#value"
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
    val contentSelector = "#main-content > div > div > p"
    val contentExample1Selector = "#main-content > div > div > ul > li:nth-child(1)"
    val contentExample2Selector = "#main-content > div > div > ul > li:nth-child(2)"
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
    val expectedExample1: String
    val expectedExample2: String
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
    val expectedTitle = "Did you get any non-qualifying relocation benefits?"
    val expectedH1 = "Did you get any non-qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got non-qualifying relocation benefits"
    val expectedContent = "These are relocation costs that your employer has paid for, or reimbursed you for. Examples include:"
    val expectedExample1 = "mortgage or housing payments if you’re moving to a more expensive area"
    val expectedExample2 = "compensation if you lose money when selling your home"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch unrhyw fuddiannau adleoli anghymwys?"
    val expectedH1 = "A gawsoch unrhyw fuddiannau adleoli anghymwys?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cawsoch fuddiannau adleoli anghymwys"
    val expectedContent = "Costau adleoli yw’r rhain y mae eich cyflogwr wedi talu amdanynt, neu wedi eich ad-dalu amdanynt. Mae enghreifftiau’n cynnwys:"
    val expectedExample1 = "taliadau morgais neu dai os ydych chi’n symud i ardal ddrutach"
    val expectedExample2 = "iawndal os byddwch yn colli arian wrth werthu eich cartref"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any non-qualifying relocation benefits?"
    val expectedH1 = "Did your client get any non-qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got non-qualifying relocation benefits"
    val expectedContent = "These are relocation costs that their employer has paid for, or reimbursed them for. Examples include:"
    val expectedExample1 = "mortgage or housing payments if they’re moving to a more expensive area"
    val expectedExample2 = "compensation if they lose money when selling their home"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient unrhyw fuddiannau adleoli anghymwys?"
    val expectedH1 = "A gafodd eich cleient unrhyw fuddiannau adleoli anghymwys?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cafodd eich cleient fuddiannau adleoli anghymwys"
    val expectedContent = "Costau adleoli yw’r rhain y mae ei gyflogwr wedi talu amdanynt, neu wedi’u had-dalu amdanynt. Mae enghreifftiau’n cynnwys:"
    val expectedExample1 = "taliadau morgais neu dai os yw’n symud i ardal ddrutach"
    val expectedExample2 = "iawndal os yw’n colli arian wrth werthu ei gartref"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def yesNoForm(isAgent: Boolean): Form[Boolean] = new AccommodationFormsProvider().nonQualifyingRelocationForm(isAgent)

  private lazy val underTest = inject[NonQualifyingRelocationBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    import userScenario.specificExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled radio buttons" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aNonQualifyingRelocationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContent, contentSelector)
        textOnPageCheck(get.expectedExample1, contentExample1Selector)
        textOnPageCheck(get.expectedExample2, contentExample2Selector)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonQualifyingRelocationBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)

        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
      }

      "render page with the 'yes' radio button pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aNonQualifyingRelocationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent).fill(value = true))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContent, contentSelector)
        textOnPageCheck(get.expectedExample1, contentExample1Selector)
        textOnPageCheck(get.expectedExample2, contentExample2Selector)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonQualifyingRelocationBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)

        radioButtonCheck(yesText, radioNumber = 1, checked = true)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
      }

      s"render page with error when a form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aNonQualifyingRelocationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.expectedContent, contentSelector)
        textOnPageCheck(get.expectedExample1, contentExample1Selector)
        textOnPageCheck(get.expectedExample2, contentExample2Selector)
        welshToggleCheck(userScenario.isWelsh)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(NonQualifyingRelocationBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)

        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
      }
    }
  }
}
