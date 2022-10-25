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
import play.api.http.Status.BAD_REQUEST
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.utilities.TelephoneBenefitsView

class TelephoneBenefitsViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"

  object Selectors {
    val paragraphSelector: String = "#main-content > div > div > p"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorText: String
    val expectedParagraphText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get a benefit for using a telephone?"
    val expectedHeading = "Did you get a benefit for using a telephone?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you got a benefit for using a telephone"
    val expectedParagraphText: String = "These are telephone costs paid by your employer that are not exempt from tax."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch fuddiant ar gyfer ddefnyddio Ffôn?"
    val expectedHeading = "A gawsoch fuddiant ar gyfer ddefnyddio Ffôn?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os cawsoch fuddiant ar gyfer defnyddio ffôn"
    val expectedParagraphText: String = "Costau Ffôn yw’r rhain sy’n cael eu talu gan eich cyflogwr ac sydd ddim wedi’u heithrio rhag treth."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get a benefit for using a telephone?"
    val expectedHeading = "Did your client get a benefit for using a telephone?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client got a benefit for using a telephone"
    val expectedParagraphText: String = "These are telephone costs paid by their employer that are not exempt from tax."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient fuddiant ar gyfer defnyddio Ffôn?"
    val expectedHeading = "A gafodd eich cleient fuddiant ar gyfer defnyddio Ffôn?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd eich cleient fuddiant ar gyfer defnyddio ffôn"
    val expectedParagraphText: String = "Costau Ffôn yw’r rhain sy’n cael eu talu gan ei gyflogwr ac sydd ddim wedi’u heithrio rhag treth."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
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

  private def form(isAgent: Boolean): Form[Boolean] = new UtilitiesFormsProvider().telephoneBenefitsForm(isAgent)

  private lazy val underTest = inject[TelephoneBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render 'Did you get a benefit for using a telephone?' page with the correct content with no pre-filling" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.utilities.routes.TelephoneBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render 'Did you get a benefit for using a telephone?' page with the correct content with cya data and the yes value pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphSelector)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.utilities.routes.TelephoneBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {
        "the value is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.benefits.utilities.routes.TelephoneBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText, Some("value"))
        }
      }
    }
  }
}
