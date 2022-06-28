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

package views.benefits.utilities

import forms.YesNoForm
import forms.benefits.utilities.UtilitiesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.utilities.ProfessionalSubscriptionsBenefitsView

class ProfessionalSubscriptionsBenefitsViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"

  object Selectors {
    val yesRadioSelector = "#value"
    val continueButtonSelector = "#main-content > div > div > form > button"
    val formSelector = "#main-content > div > div > form"

    def paragraphTextSelector(index: Int): String = s"#main-content > div > div > p:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val continueButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedParagraphText: String
    val checkWithEmployerText: String
    val expectedErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val yesText = "Iawn"
    val noText = "Na"
    val continueButtonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did your employer cover costs for any professional fees or subscriptions?"
    val expectedHeading = "Did your employer cover costs for any professional fees or subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedParagraphText: String = "Your employer may have covered fees you must pay to be able to do your job. " +
      "This includes annual subscriptions to approved professional bodies that are relevant to your work."
    val checkWithEmployerText = "Check with your employer if you are unsure."
    val expectedErrorMessage = "Select yes if your employer covered costs for any professional fees or subscriptions"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth eich cyflogwr dalu costau am unrhyw ffioedd neu danysgrifiadau proffesiynol?"
    val expectedHeading = "A wnaeth eich cyflogwr dalu costau am unrhyw ffioedd neu danysgrifiadau proffesiynol?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedParagraphText: String = "Maeín bosibl bod eich cyflogwr wedi taluír ffioedd y mae’n rhaid i chi eu talu er mwyn gallu gwneud eich swydd. " +
      "Mae hyn yn cynnwys tanysgrifiadau blynyddol i gyrff proffesiynol cymeradwy syín berthnasol iích gwaith."
    val checkWithEmployerText = "Gwiriwch ‚ích cyflogwr os nad ydych yn si?r."
    val expectedErrorMessage = "Dewiswch ‘Iawn’ os talodd eich cyflogwr y costau am unrhyw ffioedd neu danysgrifiadau proffesiynol"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s employer cover costs for any professional fees or subscriptions?"
    val expectedHeading = "Did your client’s employer cover costs for any professional fees or subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedParagraphText: String = "Your client’s employer may have covered fees they must pay to be able to do their job. " +
      "This includes annual subscriptions to approved professional bodies that are relevant to their work."
    val checkWithEmployerText = "Check with your client’s employer if you are unsure."
    val expectedErrorMessage = "Select yes if your client’s employer covered costs for any professional fees or subscriptions"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth cyflogwr eich cleient daluír costau am unrhyw ffioedd neu danysgrifiadau proffesiynol?"
    val expectedHeading = "A wnaeth cyflogwr eich cleient daluír costau am unrhyw ffioedd neu danysgrifiadau proffesiynol?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedParagraphText: String = "Maeín bosibl bod cyflogwr eich cleient wedi talu ffioedd y maeín rhaid iddo eu talu er mwyn gallu gwneud ei waith. " +
      "Mae hyn yn cynnwys tanysgrifiadau blynyddol i gyrff proffesiynol cymeradwy syín berthnasol iíw waith."
    val checkWithEmployerText = "Gwiriwch ‚ chyflogwr eich cleient os nad ydych yn si?r."
    val expectedErrorMessage = "Dewiswch ‘Iawn’ os talodd cyflogwr eich cleient y costau am unrhyw ffioedd neu danysgrifiadau proffesiynol"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new UtilitiesFormsProvider().professionalSubscriptionsBenefitsForm(isAgent)

  private lazy val underTest = inject[ProfessionalSubscriptionsBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the employer professional subscriptions page with no pre-filled radio buttons" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector(index = 2))
        textOnPageCheck(userScenario.specificExpectedResults.get.checkWithEmployerText, paragraphTextSelector(index = 3))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.utilities.routes.ProfessionalSubscriptionsBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the employer professional subscriptions page with the yes radio button pre-filled and the user has cya data and prior benefits" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector(index = 2))
        textOnPageCheck(userScenario.specificExpectedResults.get.checkWithEmployerText, paragraphTextSelector(index = 3))
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.utilities.routes.ProfessionalSubscriptionsBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "render the employer professional subscriptions page with the no radio button pre-filled and the user has cya data but no prior benefits" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(false), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector(index = 2))
        textOnPageCheck(userScenario.specificExpectedResults.get.checkWithEmployerText, paragraphTextSelector(index = 3))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.utilities.routes.ProfessionalSubscriptionsBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when a form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector(index = 3))
        textOnPageCheck(userScenario.specificExpectedResults.get.checkWithEmployerText, paragraphTextSelector(index = 4))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, yesRadioSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Some("value"))
        formPostLinkCheck(controllers.benefits.utilities.routes.ProfessionalSubscriptionsBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
      }
    }
  }
}
