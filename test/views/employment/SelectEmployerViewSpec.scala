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

import common.SessionValues
import controllers.employment.routes.SelectEmployerController
import forms.employment.SelectEmployerForm
import models.employment.Employer
import models.{AuthorisationRequest, UserPriorDataRequest}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import utils.ViewUtils
import views.html.employment.SelectEmployerView

import java.time.LocalDate

class SelectEmployerViewSpec extends ViewUnitTest {

  object Selectors {
    val paragraphTextSelector: String = "#main-content > div > div > p.govuk-body"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val formSelector = "#value"
    val orSelector = ".govuk-radios__divider"

    def hintSelector(id: String = ""): String = s"#value$id-item-hint"
  }

  trait SpecificExpectedResults {
    val expectedParagraphText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val addNewEmployerText: String
    val orText: String
    val expectedTitle: String
    val expectedH1: String
    val startedDateString: LocalDate => String
    val startedDateMissingString: String
    val startedDateBeforeString: Int => String
    val expectedError: String

    def datesText(start: String, end: String): String

    val expectedErrorTitle: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"PAYE employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val addNewEmployerText: String = "Add a new period of employment"
    val orText: String = "or"
    val expectedTitle: String = "Which period of employment do you want to add?"
    val expectedH1: String = "Which period of employment do you want to add?"
    val startedDateString: LocalDate => String = (startedOn: LocalDate) => s"Started " + ViewUtils.translatedDateFormatter(startedOn)(getMessages(isWelsh = false))
    val startedDateMissingString: String = "Start date missing"
    val startedDateBeforeString: Int => String = (taxYear: Int) => s"Started before 6 April $taxYear"
    val expectedError = "Select a period of employment or add a new one"

    def datesText(start: String, end: String): String = s"${ViewUtils.dateFormatter(start).get} to ${ViewUtils.dateFormatter(end).get}"

    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Cyflogaeth TWE ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val addNewEmployerText: String = "Ychwanegwch gyfnod newydd o gyflogaeth"
    val orText: String = "neu"
    val expectedTitle: String = "Pa gyfnod o gyflogaeth ydych am ei ychwanegu?"
    val expectedH1: String = "Pa gyfnod o gyflogaeth ydych am ei ychwanegu?"
    val startedDateString: LocalDate => String = (startedOn: LocalDate) => s"Wedi dechrau ar " + ViewUtils.translatedDateFormatter(startedOn)(getMessages(isWelsh = true))
    val startedDateMissingString: String = "Dyddiad dechrau ar goll"
    val startedDateBeforeString: Int => String = (taxYear: Int) => s"Wedi dechrau cyn 6 Ebrill $taxYear"
    val expectedError = "Dewiswch gyfnod cyflogaeth neu ychwanegwch un newydd"

    def datesText(start: String, end: String): String = s"${ViewUtils.dateFormatter(start).get} i ${ViewUtils.dateFormatter(end).get}"

    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedParagraphText = "The information we hold about you does not include changes you made."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedParagraphText = "Nid yw’r wybodaeth sydd gennym amdanoch yn cynnwys newidiadau a wnaethoch."
    val expectedError = "Select your employer or add a new employer"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedParagraphText = "The information we hold about your client does not include changes you made."
    val expectedError = "Select your client’s employer or add a new employer"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedParagraphText = "Nid yw’r wybodaeth sydd gennym am eich cleient yn cynnwys newidiadau a wnaethoch."
    val expectedError = "Select your client’s employer or add a new employer"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val form: Form[String] = new SelectEmployerForm().employerListForm(Seq("id"))

  private val employers = Seq(
    Employer("id", "Emp 1", Some(s"${taxYearEOY - 1}-11-11"), Some(s"${taxYearEOY - 1}-11-11")),
    Employer("id2", "Emp 2", Some(s"${taxYearEOY - 1}-04-05"), None),
    Employer("id3", "Emp 3", None, None)
  )

  private lazy val underTest = inject[SelectEmployerView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "Render the select employer page with no pre-filled data" which {
        val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val request: UserPriorDataRequest[AnyContent] = UserPriorDataRequest(anAllEmploymentData, authRequest.user, authRequest)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employers, form)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedH1)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector)
        radioButtonCheck(employers.head.name, radioNumber = 1, checked = false)
        radioButtonCheck(employers(1).name, radioNumber = 2, checked = false)
        radioButtonCheck(employers(2).name, radioNumber = 3, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.addNewEmployerText, radioNumber = 4, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(SelectEmployerController.submit(taxYearEOY).url, continueButtonFormSelector)
        textOnPageCheck(userScenario.commonExpectedResults.orText, orSelector)
        textOnPageCheck(userScenario.commonExpectedResults.startedDateString(LocalDate.parse(employers.head.startDate.get)), hintSelector())
        textOnPageCheck(userScenario.commonExpectedResults.startedDateBeforeString(taxYearEOY - 1), hintSelector("-2"))
        textOnPageCheck(userScenario.commonExpectedResults.startedDateMissingString, hintSelector("-3"))
        welshToggleCheck(userScenario.isWelsh)
      }

      "Render the select employer page with 'Add a new employer' as pre-filled data" which {
        val authRequest = getAuthRequest(userScenario.isAgent)
        implicit val request: UserPriorDataRequest[AnyContent] = UserPriorDataRequest(anAllEmploymentData, authRequest.user, authRequest)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employers, form.fill(SessionValues.ADD_A_NEW_EMPLOYER))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedH1)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector)
        radioButtonCheck(employers.head.name, radioNumber = 1, checked = false)
        radioButtonCheck(employers(1).name, radioNumber = 2, checked = false)
        radioButtonCheck(employers(2).name, radioNumber = 3, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.addNewEmployerText, radioNumber = 4, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.commonExpectedResults.orText, orSelector)
        formPostLinkCheck(SelectEmployerController.submit(taxYearEOY).url, continueButtonFormSelector)
        textOnPageCheck(userScenario.commonExpectedResults.startedDateString(LocalDate.parse(employers.head.startDate.get)), hintSelector())
        textOnPageCheck(userScenario.commonExpectedResults.startedDateBeforeString(taxYearEOY - 1), hintSelector("-2"))
        textOnPageCheck(userScenario.commonExpectedResults.startedDateMissingString, hintSelector("-3"))
        welshToggleCheck(userScenario.isWelsh)
      }

      "Render with empty form validation error" which {
        val authRequest = getAuthRequest(userScenario.isAgent)
        implicit val request: UserPriorDataRequest[AnyContent] = UserPriorDataRequest(anAllEmploymentData, authRequest.user, authRequest)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employers, form.bind(Map("value" -> "")))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector)
        radioButtonCheck(employers.head.name, radioNumber = 1, checked = false)
        radioButtonCheck(employers(1).name, radioNumber = 2, checked = false)
        radioButtonCheck(employers(2).name, radioNumber = 3, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.addNewEmployerText, radioNumber = 4, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.commonExpectedResults.orText, orSelector)
        formPostLinkCheck(SelectEmployerController.submit(taxYearEOY).url, continueButtonFormSelector)
        textOnPageCheck(userScenario.commonExpectedResults.startedDateString(LocalDate.parse(employers.head.startDate.get)), hintSelector())
        textOnPageCheck(userScenario.commonExpectedResults.startedDateBeforeString(taxYearEOY - 1), hintSelector("-2"))
        textOnPageCheck(userScenario.commonExpectedResults.startedDateMissingString, hintSelector("-3"))
        welshToggleCheck(userScenario.isWelsh)
        errorSummaryCheck(userScenario.commonExpectedResults.expectedError, Selectors.formSelector)
        errorAboveElementCheck(userScenario.commonExpectedResults.expectedError, Some("value"))
      }
    }
  }
}