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
import forms.details.EmployerPayrollIdForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.details.EmployerPayrollIdView

class EmployerPayrollIdViewSpec extends ViewUnitTest {

  private val employmentId = "001"

  object Selectors {
    val paragraph0Selector = "#main-content > div > div > p.govuk-body:first-of-type"
    val paragraph2Selector = "p.govuk-body:nth-child(3)"
    val paragraph3Selector = "p.govuk-body:nth-child(4)"
    val paragraph4Selector = "p.govuk-body:nth-child(5)"
    val hintTextSelector = "#payrollId-hint"
    val inputSelector = "#payrollId"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#payrollId"

    def bulletSelector(bulletNumber: Int): String =
      s"#main-content > div > div > ul > li:nth-child($bulletNumber)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String
    val wrongFormatErrorText: String
    val tooLongErrorText: String
    val paragraph1: String
    val paragraph2: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val hintText: String
    val bullet1: String
    val bullet2: String
    val bullet3: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Employment details for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, 123456"
    val bullet1: String = "upper and lower case letters (a to z)"
    val bullet2: String = "numbers"
    val bullet3: String = "the special characters: .,-()/=!\"%&*;<>'+:\\?"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val continueButtonText = "Yn eich blaen"
    val hintText = "Er enghraifft, 123456"
    val bullet1: String = "llythrennau mawr a bach (a i z)"
    val bullet2: String = "rhifau"
    val bullet3: String = "y cymeriadau arbennig: .,-()/=!\"%&*;<>'+:\\?"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your payroll ID for this employment?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedH1: String = "What’s your payroll ID for this employment?"
    val wrongFormatErrorText: String = "Enter your payroll ID in the correct format"
    val tooLongErrorText: String = "Your payroll ID must be 38 characters or fewer"
    val paragraph1: String = "Your payroll ID must be 38 characters or fewer. It can include:"
    val paragraph2: String = "You can find this on your payslip or on your P60. It’s also known as a ‘payroll number’."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your client’s payroll ID for this employment?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedH1: String = "What’s your client’s payroll ID for this employment?"
    val wrongFormatErrorText: String = "Enter your client’s payroll ID in the correct format"
    val tooLongErrorText: String = "Your client’s payroll ID must be 38 characters or fewer"
    val paragraph1: String = "Your client’s payroll ID must be 38 characters or fewer. It can include:"
    val paragraph2: String = "You can find this on your client’s payslip or on their P60. It’s also known as a ‘payroll number’."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Beth yw’ch ID cyflogres am y gyflogaeth hon?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedH1: String = "Beth yw’ch ID cyflogres am y gyflogaeth hon?"
    val wrongFormatErrorText: String = "Nodwch eich ID cyflogres yn y fformat cywir"
    val tooLongErrorText: String = "Mae’n rhaid i’ch ID cyflogres fod yn 38 o gymeriadau neu lai"
    val paragraph1: String = "Mae’n rhaid i’ch ID cyflogres fod yn 38 o gymeriadau neu lai. Gall gynnwys y canlynol:"
    val paragraph2: String = "Mae hwn i’w weld ar eich slip cyflog neu’ch P60. Mae hefyd yn cael ei alw’n ‘rhif cyflogres’."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Beth yw ID cyflogres eich cleient ar gyfer y gyflogaeth hon?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedH1: String = "Beth yw ID cyflogres eich cleient ar gyfer y gyflogaeth hon?"
    val wrongFormatErrorText: String = "Nodwch ID cyflogres eich cleient yn y fformat cywir"
    val tooLongErrorText: String = "Mae’n rhaid i ID cyflogres eich cleient fod yn 38 o gymeriadau neu lai."
    val paragraph1: String = "Mae’n rhaid i ID cyflogres eich cleient fod yn 38 o gymeriadau neu lai. Gall gynnwys y canlynol:"
    val paragraph2: String = "Mae hwn i’w weld ar slip cyflog eich cleient neu ar ei P60. Mae hefyd yn cael ei alw’n ‘rhif cyflogres’."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val form = EmployerPayrollIdForm
  private val underTest = inject[EmployerPayrollIdView]

  userScenarios.foreach { user =>
    import Selectors._
    import user.commonExpectedResults._
    import user.specificExpectedResults._

    s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
      "should render the page with the correct content when theres no previous payroll id" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(form.employerPayrollIdForm(user.isAgent), taxYear = taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.paragraph1, paragraph0Selector)
        textOnPageCheck(bullet1, bulletSelector(1))
        textOnPageCheck(bullet2, bulletSelector(2))
        textOnPageCheck(bullet3, bulletSelector(3))
        textOnPageCheck(get.paragraph2, paragraph3Selector)
        textOnPageCheck(hintText, hintTextSelector)
        inputFieldValueCheck(EmployerPayrollIdForm.payrollId, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayrollIdController.show(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(user.isWelsh)
      }

      "should render the page with a pre-filled form when previous payroll id is defined" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(form.employerPayrollIdForm(user.isAgent).fill(value = "123456"), taxYear = taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.paragraph1, paragraph0Selector)
        textOnPageCheck(bullet1, bulletSelector(1))
        textOnPageCheck(bullet2, bulletSelector(2))
        textOnPageCheck(bullet3, bulletSelector(3))
        textOnPageCheck(get.paragraph2, paragraph3Selector)
        textOnPageCheck(hintText, hintTextSelector)
        inputFieldValueCheck(EmployerPayrollIdForm.payrollId, inputSelector, "123456")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayrollIdController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(user.isWelsh)
      }

      "render the page with a form error when the input is too long" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val tooLonPayrollId = "a" * 39
        val htmlFormat = underTest(form.employerPayrollIdForm(user.isAgent).bind(Map(EmployerPayrollIdForm.payrollId -> tooLonPayrollId)), taxYear = taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.paragraph1, paragraph2Selector)
        textOnPageCheck(bullet1, bulletSelector(1))
        textOnPageCheck(bullet2, bulletSelector(2))
        textOnPageCheck(bullet3, bulletSelector(3))
        textOnPageCheck(get.paragraph2, paragraph4Selector)
        textOnPageCheck(hintText, hintTextSelector)
        inputFieldValueCheck(EmployerPayrollIdForm.payrollId, inputSelector, tooLonPayrollId)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayrollIdController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(user.isWelsh)

        errorSummaryCheck(get.tooLongErrorText, expectedErrorHref)
        errorAboveElementCheck(get.tooLongErrorText)
      }

      "render the page with a form error when the input is in the wrong format" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val payrollId = "$11223"
        val htmlFormat = underTest(form.employerPayrollIdForm(user.isAgent).bind(Map(EmployerPayrollIdForm.payrollId -> payrollId)), taxYear = taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, user.isWelsh)
        h1Check(get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(get.paragraph1, paragraph2Selector)
        textOnPageCheck(bullet1, bulletSelector(1))
        textOnPageCheck(bullet2, bulletSelector(2))
        textOnPageCheck(bullet3, bulletSelector(3))
        textOnPageCheck(get.paragraph2, paragraph4Selector)
        textOnPageCheck(hintText, hintTextSelector)
        inputFieldValueCheck(EmployerPayrollIdForm.payrollId, inputSelector, payrollId)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerPayrollIdController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(user.isWelsh)

        errorSummaryCheck(get.wrongFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(get.wrongFormatErrorText)
      }
    }
  }
}
