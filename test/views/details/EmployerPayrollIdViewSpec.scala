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

import controllers.details.routes.EmployerPayrollIdController
import forms.details.{EmployerPayrollIdForm, EmploymentDetailsFormsProvider}
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.benefits.pages.EmployerPayrollIdPageBuilder.anEmployerPayrollIdPage
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import views.html.details.EmployerPayrollIdView

class EmployerPayrollIdViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"
  private val employerName = anEmploymentDetails.employerName

  object Selectors {
    val hintTextSelector = "#payrollId-hint"
    val inputSelector = "#payrollId"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#payrollId"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val hintTextP45: String
    val hintTextP60: String
  }

  trait CommonExpectedResults {
    val continueButtonText: String
    val expectedTooManyCharactersError: String

    def expectedInvalidCharactersError(invalidCharacters: String): String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val continueButtonText = "Continue"
    val expectedTooManyCharactersError = "Payroll ID must be 38 characters or fewer"

    def expectedInvalidCharactersError(invalidCharacters: String): String = s"Payroll ID must not include $invalidCharacters"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val continueButtonText = "Yn eich blaen"
    val expectedTooManyCharactersError = "Payroll ID must be 38 characters or fewer"

    def expectedInvalidCharactersError(invalidCharacters: String): String = s"Payroll ID must not include $invalidCharacters"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = s"What’s your payroll ID for $employerName? (optional)"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val hintTextP45: String = "You can find this on your payslips or P45. It’s also known as a ‘payroll number’."
    val hintTextP60: String = "You can find this on your payslips or P60. It’s also known as a ‘payroll number’."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = s"What’s your client’s payroll ID for $employerName? (optional)"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val hintTextP45: String = "You can find this on your client’s payslips or P45. It’s also known as a ‘payroll number’."
    val hintTextP60: String = "You can find this on your client’s payslips or P60. It’s also known as a ‘payroll number’."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = s"What’s your payroll ID for $employerName? (optional)"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val hintTextP45: String = "You can find this on your payslips or P45. It’s also known as a ‘payroll number’."
    val hintTextP60: String = "You can find this on your payslips or P60. It’s also known as a ‘payroll number’."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = s"What’s your client’s payroll ID for $employerName? (optional)"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val hintTextP45: String = "You can find this on your client’s payslips or P45. It’s also known as a ‘payroll number’."
    val hintTextP60: String = "You can find this on your client’s payslips or P60. It’s also known as a ‘payroll number’."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def payrollIdForm(): Form[String] = new EmploymentDetailsFormsProvider().employerPayrollIdForm()

  private val underTest = inject[EmployerPayrollIdView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    import userScenario.specificExpectedResults._

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "should render the page with the correct content when theres no previous payroll id" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = anEmployerPayrollIdPage.copy(isAgent = userScenario.isAgent, employmentEnded = true, form = payrollIdForm())
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedTitle, userScenario.isWelsh)
        labelH1Check(get.expectedTitle)
        textOnPageCheck(get.hintTextP60, hintTextSelector)
        inputFieldValueCheck(EmployerPayrollIdForm.payrollId, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayrollIdController.show(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "should render the page with a pre-filled form when previous payroll id is defined" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = anEmployerPayrollIdPage.copy(isAgent = userScenario.isAgent, employmentEnded = false, form = payrollIdForm().fill(value = "123456"))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedTitle, userScenario.isWelsh)
        labelH1Check(get.expectedTitle)
        textOnPageCheck(get.hintTextP45, hintTextSelector)
        inputFieldValueCheck(EmployerPayrollIdForm.payrollId, inputSelector, "123456")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayrollIdController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the page with a form error when the input is too long" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val tooLonPayrollId = "a" * 39
        val formWithError = payrollIdForm().bind(Map(EmployerPayrollIdForm.payrollId -> tooLonPayrollId))
        val pageModel = anEmployerPayrollIdPage.copy(isAgent = userScenario.isAgent, employmentEnded = true, form = formWithError)
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        labelH1Check(get.expectedTitle)
        textOnPageCheck(get.hintTextP60, hintTextSelector)
        inputFieldValueCheck(EmployerPayrollIdForm.payrollId, inputSelector, tooLonPayrollId)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayrollIdController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(expectedTooManyCharactersError, expectedErrorHref)
        errorAboveElementCheck(expectedTooManyCharactersError)
      }

      "render the page with a form error when the input is in the wrong format" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val payrollId = "$11223"
        val formWithError = payrollIdForm().bind(Map(EmployerPayrollIdForm.payrollId -> payrollId))
        val pageModel = anEmployerPayrollIdPage.copy(isAgent = userScenario.isAgent, employmentEnded = false, form = formWithError)
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, userScenario.isWelsh)
        labelH1Check(get.expectedTitle)
        textOnPageCheck(get.hintTextP45, hintTextSelector)
        inputFieldValueCheck(EmployerPayrollIdForm.payrollId, inputSelector, payrollId)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayrollIdController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(expectedInvalidCharactersError("$"), expectedErrorHref)
        errorAboveElementCheck(expectedInvalidCharactersError("$"))
      }
    }
  }
}
