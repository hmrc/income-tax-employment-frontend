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

package views.benefits

import forms.YesNoForm
import forms.benefits.BenefitsFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.ReceiveAnyBenefitsView

class ReceiveAnyBenefitsViewSpec extends ViewUnitTest {

  private val employmentId = "1234567890"

  object Selectors {
    val expectedErrorHref = "#value"
    val paragraphSelector = "#main-content > div > div > p"
    val formSelector = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val continueButton: String
    val expectedCaption: String
    val paragraphText: String
    val yesText: String
    val noText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val paragraphText = "Examples of benefits include company cars or vans, fuel allowance and medical insurance."
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val continueButton: String = "Yn eich blaen"
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val paragraphText = "Enghreifftiau o fuddiannau yw ceir neu faniau cwmni, lwfans tanwydd ac yswiriant meddygol."
    val yesText = "Iawn"
    val noText = "Na"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "Did you get any benefits from this company?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you got any benefits from this company"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "Did your client get any benefits from this company?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client got any benefits from this company"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "A gawsoch unrhyw fuddiannau gan y cwmni hwn?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os cawsoch fuddiannau gan y cwmni hwn"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "A gafodd eich cleient unrhyw fuddiannau gan y cwmni hwn?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd eich cleient unrhyw fuddiannau gan y cwmni hwn"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new BenefitsFormsProvider().receiveAnyBenefitsForm(isAgent)

  private lazy val underTest = inject[ReceiveAnyBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    val specific = userScenario.specificExpectedResults.get
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "return Did you receive any benefits question page" when {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(paragraphText, paragraphSelector)
        buttonCheck(continueButton)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        formPostLinkCheck(controllers.benefits.routes.ReceiveAnyBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
      }

      "return Did you receive any benefits question page with radio button pre-filled if isBenefits received field true" when {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(paragraphText, paragraphSelector)
        buttonCheck(continueButton)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        formPostLinkCheck(controllers.benefits.routes.ReceiveAnyBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
      }

      "return the Did you receive any employments Page with errors when no radio button is selected" when {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(specific.expectedErrorTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(expectedCaption)
        buttonCheck(continueButton)
        errorSummaryCheck(specific.expectedErrorText, expectedErrorHref)
        formPostLinkCheck(controllers.benefits.routes.ReceiveAnyBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
      }
    }
  }
}