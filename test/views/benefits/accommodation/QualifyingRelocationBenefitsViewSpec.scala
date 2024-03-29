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

import controllers.benefits.accommodation.routes.QualifyingRelocationBenefitsController
import forms.YesNoForm
import forms.benefits.accommodation.AccommodationFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.benefits.pages.QualifyingRelocationBenefitsAmountPageBuilder.aQualifyingRelocationBenefitsAmountPage
import support.builders.models.benefits.pages.QualifyingRelocationBenefitsPageBuilder.aQualifyingRelocationBenefitsPage
import views.html.benefits.accommodation.QualifyingRelocationBenefitsView

class QualifyingRelocationBenefitsViewSpec extends ViewUnitTest {

  private val employmentId = aQualifyingRelocationBenefitsAmountPage.employmentId

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
    val expectedContent: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val expectedExample1: String
    val yesText: String
    val noText: String
  }

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val contentSelector = "#main-content > div > div > p"
    val yesSelector = "#value"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedExample1 = "This does not include the cost of using the NHS after coming into the UK."
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val expectedExample1 = "Nid yw hyn yn cynnwys cost defnyddio’r GIG ar ôl dod i’r DU."
    val yesText = "Iawn"
    val noText = "Na"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any qualifying relocation benefits?"
    val expectedH1 = "Did you get any qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got qualifying relocation benefits"
    val expectedContent = "These are costs that your employer has paid to help you with relocation, including bridging loans and legal fees."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any qualifying relocation benefits?"
    val expectedH1 = "Did your client get any qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got qualifying relocation benefits"
    val expectedContent = "These are costs that their employer has paid to help them with relocation, including bridging loans and legal fees."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch unrhyw fuddiant adleoli cymwys?"
    val expectedH1 = "A gawsoch unrhyw fuddiant adleoli cymwys?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cawsoch unrhyw fuddiannau adleoli cymwys"
    val expectedContent = "Mae’r rhain yn gostau y mae eich cyflogwr wedi’u talu i’ch helpu i adleoli, gan gynnwys benthyciadau pontio a ffioedd cyfreithiol."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient unrhyw fuddiannau adleoli cymwys?"
    val expectedH1 = "A gafodd eich cleient unrhyw fuddiannau adleoli cymwys?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cafodd eich cleient fuddiannau adleoli cymwys"
    val expectedContent = "Mae’r rhain yn gostau y mae ei gyflogwr wedi’u talu i’w helpu i adleoli, gan gynnwys benthyciadau pontio a ffioedd cyfreithiol."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def yesNoForm(isAgent: Boolean): Form[Boolean] = new AccommodationFormsProvider().qualifyingRelocationForm(isAgent)

  private lazy val underTest = inject[QualifyingRelocationBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled radio buttons" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aQualifyingRelocationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(QualifyingRelocationBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        textOnPageCheck(userScenario.specificExpectedResults.get.expectedContent + " " + userScenario.commonExpectedResults.expectedExample1, contentSelector)

        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
      }

      "render page with the 'yes' radio button pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aQualifyingRelocationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent).fill(value = true))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(QualifyingRelocationBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        textOnPageCheck(userScenario.specificExpectedResults.get.expectedContent + " " + userScenario.commonExpectedResults.expectedExample1, contentSelector)

        radioButtonCheck(yesText, radioNumber = 1, checked = true)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
      }

      "render page with the 'no' radio button pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aQualifyingRelocationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent).fill(value = false))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(QualifyingRelocationBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        textOnPageCheck(userScenario.specificExpectedResults.get.expectedContent + " " + userScenario.commonExpectedResults.expectedExample1, contentSelector)

        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = true)
      }

      "render page with error when a form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aQualifyingRelocationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
        formPostLinkCheck(QualifyingRelocationBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)

        textOnPageCheck(userScenario.specificExpectedResults.get.expectedContent + " " + userScenario.commonExpectedResults.expectedExample1, contentSelector)
      }
    }
  }
}
