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

package views.details

import forms.details.EmployerNameForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.details.EmployerNameView

class EmployerNameViewSpec extends ViewUnitTest {

  private val employerName = "some-name"
  private val employmentId = "employmentId"
  private val amountInputName = "name"

  object Selectors {
    val inputSelector: String = "#name"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
  }

  trait CommonExpectedResults {
    val expectedButtonText: String
    val expectedErrorCharLimit: String
    val expectedErrorWrongFormat: String => String
  }

  object ExpectedIndividual extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your employer?"
    val expectedH1 = "What’s the name of your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your employer"
  }

  object ExpectedAgent extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your client’s employer?"
    val expectedH1 = "What’s the name of your client’s employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your client’s employer"
  }

  object CommonExpected extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedErrorCharLimit = "Employer’s name must be 74 characters or fewer"
    val expectedErrorWrongFormat: String => String = (invalidChars: String) => s"Employer’s name must not include $invalidChars"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpected, Some(ExpectedIndividual)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpected, Some(ExpectedAgent)))
  }

  private def form(isAgent: Boolean): Form[String] = EmployerNameForm.employerNameForm(isAgent)

  private val underTest = inject[EmployerNameView]

  userScenarios.foreach { userScenario =>
    s"Request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the 'name of your employer' page with the correct content and empty fields" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors.inputSelector

        inputFieldValueCheck(amountInputName, inputSelector, "")
      }

      "render the 'name of your employer' page with the correct content and pre-popped input field" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(employerName), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors.inputSelector

        inputFieldValueCheck(amountInputName, inputSelector, employerName)
      }

      s"render the page with a form error" when {
        "the submitted data is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(EmployerNameForm.employerName -> "")), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          import Selectors.inputSelector

          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, inputSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry)
        }

        "the submitted data is in the wrong format" which {
          val wrongFormat: String = "~name~"
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(EmployerNameForm.employerName -> wrongFormat)), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          import Selectors.inputSelector

          errorSummaryCheck(userScenario.commonExpectedResults.expectedErrorWrongFormat("~"), inputSelector)
          errorAboveElementCheck(userScenario.commonExpectedResults.expectedErrorWrongFormat("~"))
        }

        "the submitted data is too long" which {
          val charLimit = 74
          val veryLongName = "a" * charLimit + 1

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(EmployerNameForm.employerName -> veryLongName)), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          import Selectors._
          import userScenario.commonExpectedResults._

          errorSummaryCheck(expectedErrorCharLimit, inputSelector)
          errorAboveElementCheck(expectedErrorCharLimit)
        }
      }
    }
  }
}
