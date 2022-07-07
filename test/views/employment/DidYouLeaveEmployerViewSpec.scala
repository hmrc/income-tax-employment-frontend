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

package views.employment

import controllers.employment.routes.DidYouLeaveEmployerController
import forms.employment.EmploymentDetailsFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.employment.DidYouLeaveEmployerView

class DidYouLeaveEmployerViewSpec extends ViewUnitTest {

  private val employerName: String = "HMRC"
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
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you leave this employer in the tax year?"
    val expectedH1 = "Did you leave HMRC in the tax year?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you left HMRC in the tax year"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaethoch adael y cyflogwr hwn yn y flwyddyn dreth?"
    val expectedH1 = "A wnaethoch adael HMRC yn y flwyddyn dreth?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os gwnaethoch adael HMRC yn y flwyddyn dreth"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client leave this employer in the tax year?"
    val expectedH1 = "Did your client leave HMRC in the tax year?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client left HMRC in the tax year"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A adawodd eich cleient y cyflogwr hwn yn y flwyddyn dreth?"
    val expectedH1 = "A adawodd eich cleient HMRC yn y flwyddyn dreth?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os gadawodd eich cleient HMRC yn y flwyddyn dreth"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  private val formProvider = new EmploymentDetailsFormsProvider()

  private val underTest = inject[DidYouLeaveEmployerView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the 'did you leave employer' page with the correct content and the radio buttons unselected" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(formProvider.didYouLeaveForm(userScenario.isAgent, employerName), taxYearEOY, employmentId, employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        welshToggleCheck(userScenario.isWelsh)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(DidYouLeaveEmployerController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }

      "render the 'did you leave employer' page with the correct content and 'yes' radio button selected" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(formProvider.didYouLeaveForm(userScenario.isAgent, employerName).fill(true),taxYearEOY, employmentId, employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        welshToggleCheck(userScenario.isWelsh)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(DidYouLeaveEmployerController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }

      "render the 'did you leave employer' page with the correct content and 'no' radio button selected" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(formProvider.didYouLeaveForm(userScenario.isAgent, employerName).fill(false),taxYearEOY, employmentId, employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        welshToggleCheck(userScenario.isWelsh)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(DidYouLeaveEmployerController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }

      "render the 'did you leave employer' page with the form error when an empty form is submitted" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(formProvider.didYouLeaveForm(userScenario.isAgent, employerName).bind(Map("value" -> "")),taxYearEOY, employmentId, employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        welshToggleCheck(userScenario.isWelsh)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(DidYouLeaveEmployerController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
      }
    }
  }
}
