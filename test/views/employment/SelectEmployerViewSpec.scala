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
import support.builders.models.UserBuilder.aUser
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import utils.ViewUtils
import views.html.employment.SelectEmployerView

class SelectEmployerViewSpec extends ViewUnitTest {

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val formSelector = "#value"
    val orSelector = ".govuk-radios__divider"

    def hintSelector(id: String = ""): String = s"#value$id-item-hint"
  }

  trait SpecificExpectedResults {
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val addNewEmployerText: String
    val orText: String
    val expectedTitle: String
    val expectedH1: String

    def fromText(date: String): String

    def datesText(start: String, end: String): String

    val expectedErrorTitle: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val addNewEmployerText: String = "Add a new employer"
    val orText: String = "or"
    val expectedTitle: String = "Which employer do you want to add?"
    val expectedH1: String = "Which employer do you want to add?"

    def fromText(date: String): String = s"From ${ViewUtils.dateFormatter(date).get}"

    def datesText(start: String, end: String): String = s"${ViewUtils.dateFormatter(start).get} to ${ViewUtils.dateFormatter(end).get}"

    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val addNewEmployerText: String = "Add a new employer"
    val orText: String = "or"
    val expectedTitle: String = "Which employer do you want to add?"
    val expectedH1: String = "Which employer do you want to add?"

    def fromText(date: String): String = s"From ${ViewUtils.dateFormatter(date).get}"

    def datesText(start: String, end: String): String = s"${ViewUtils.dateFormatter(start).get} to ${ViewUtils.dateFormatter(end).get}"

    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedError = "Select your employer or add a new employer"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedError = "Select your employer or add a new employer"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedError = "Select your client’s employer or add a new employer"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedError = "Select your client’s employer or add a new employer"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  implicit val request: UserPriorDataRequest[AnyContent] = UserPriorDataRequest(
    anAllEmploymentData, aUser, individualUserRequest
  )

  private def form(isAgent: Boolean): Form[String] = new SelectEmployerForm().employerListForm(isAgent, Seq("id"))

  val employers = Seq(
    Employer("id", "Emp 1", Some("2020-11-11"), Some("2020-11-11")),
    Employer("id2", "Emp 2", Some("2020-11-11"), None),
    Employer("id3", "Emp 3", None, None)
  )

  private lazy val underTest = inject[SelectEmployerView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "Render the select employer page with no pre-filled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employers, form(userScenario.isAgent))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedTitle)
        h1Check(userScenario.commonExpectedResults.expectedH1)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        radioButtonCheck(employers.head.name, radioNumber = 1, checked = false)
        radioButtonCheck(employers(1).name, radioNumber = 2, checked = false)
        radioButtonCheck(employers(2).name, radioNumber = 3, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.addNewEmployerText, radioNumber = 4, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(SelectEmployerController.submit(taxYearEOY).url, continueButtonFormSelector)
        textOnPageCheck(userScenario.commonExpectedResults.orText, orSelector)
        textOnPageCheck(userScenario.commonExpectedResults.datesText(employers.head.startDate.get, employers.head.leaveDate.get), hintSelector())
        textOnPageCheck(userScenario.commonExpectedResults.fromText(employers(1).startDate.get), hintSelector("-2"))
        welshToggleCheck(userScenario.isWelsh)
      }

      "Render the select employer page with 'Add a new employer' as pre-filled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employers, form(userScenario.isAgent).fill(SessionValues.ADD_A_NEW_EMPLOYER))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedTitle)
        h1Check(userScenario.commonExpectedResults.expectedH1)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        radioButtonCheck(employers.head.name, radioNumber = 1, checked = false)
        radioButtonCheck(employers(1).name, radioNumber = 2, checked = false)
        radioButtonCheck(employers(2).name, radioNumber = 3, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.addNewEmployerText, radioNumber = 4, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.commonExpectedResults.orText, orSelector)
        formPostLinkCheck(SelectEmployerController.submit(taxYearEOY).url, continueButtonFormSelector)
        textOnPageCheck(userScenario.commonExpectedResults.datesText(employers.head.startDate.get, employers.head.leaveDate.get), hintSelector())
        textOnPageCheck(userScenario.commonExpectedResults.fromText(employers(1).startDate.get), hintSelector("-2"))
        welshToggleCheck(userScenario.isWelsh)
      }

      "Render with empty form validation error" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employers, form(userScenario.isAgent).bind(Map("value" -> "")))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle)
        h1Check(userScenario.commonExpectedResults.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        radioButtonCheck(employers.head.name, radioNumber = 1, checked = false)
        radioButtonCheck(employers(1).name, radioNumber = 2, checked = false)
        radioButtonCheck(employers(2).name, radioNumber = 3, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.addNewEmployerText, radioNumber = 4, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        textOnPageCheck(userScenario.commonExpectedResults.orText, orSelector)
        formPostLinkCheck(SelectEmployerController.submit(taxYearEOY).url, continueButtonFormSelector)
        textOnPageCheck(userScenario.commonExpectedResults.datesText(employers.head.startDate.get, employers.head.leaveDate.get), hintSelector())
        textOnPageCheck(userScenario.commonExpectedResults.fromText(employers(1).startDate.get), hintSelector("-2"))
        welshToggleCheck(userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.formSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
      }
    }
  }
}