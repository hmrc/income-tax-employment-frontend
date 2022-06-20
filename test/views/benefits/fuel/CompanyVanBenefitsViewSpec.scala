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

import controllers.benefits.fuel.routes.CompanyVanBenefitsController
import forms.YesNoForm
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.fuel.CompanyVanBenefitsView

class CompanyVanBenefitsViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"

  object Selectors {
    val yesRadioButtonSelector = "#value"
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val yesText: String
    val noText: String
    val continueButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yesText = "Yes"
    val noText = "No"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yesText = "Iawn"
    val noText = "Na"
    val continueButtonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get a company van benefit?"
    val expectedHeading = "Did you get a company van benefit?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Select yes if you got a company van benefit"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch fuddiant fan cwmni?"
    val expectedHeading = "A gawsoch fuddiant fan cwmni?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Dewiswch ëIawní os cawsoch fuddiant fan cwmni"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get a company van benefit?"
    val expectedHeading = "Did your client get a company van benefit?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Select yes if your client got a company van benefit"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient fuddiant fan cwmni?"
    val expectedHeading = "A gafodd eich cleient fuddiant fan cwmni?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Dewiswch ëIawní os cafodd eich cleient fuddiant fan cwmni"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new FuelFormsProvider().companyVanForm(isAgent)

  private lazy val underTest = inject[CompanyVanBenefitsView]

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
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        formPostLinkCheck(CompanyVanBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'yes' radio button prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        radioButtonCheck(yesText, radioNumber = 1, checked = true)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.continueButtonText, continueButtonSelector)
        formPostLinkCheck(CompanyVanBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when a user submits an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorMessage, yesRadioButtonSelector)

        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
