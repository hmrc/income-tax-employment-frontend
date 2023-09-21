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

package views.offPayrollWorking

import controllers.offPayrollWorking.routes.EmployerOffPayrollWorkingController
import forms.YesNoForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.offPayrollWorking.EmployerOffPayrollWorkingView

class EmployerOffPayrollWorkingViewSpec extends ViewUnitTest {

  object Selectors {
    val paragraph1 = "#employment-opw-paragraph-1"
    val paragraph2 = "#employment-opw-paragraph-2"
    val bullet1 = "#main-content > div > div > ul > li:nth-child(1)"
    val bullet2 = "#main-content > div > div > ul > li:nth-child(2)"
    val radioHeading = "#main-content > div > div > form > div > fieldset > legend"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val continueButton = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val findOutMoreLink = "#employment-OPW-link-1"
    val getHelpLink = "#help"
  }

  trait SpecificExpectedResults {
    val expectedRadioHeading: String
    val expectedParagraph1: String
    val expectedBullet1: String
    val expectedBullet2: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val yesText: String
    val noText: String
    val expectedParagraph2: String
    val expectedLink: String
    val expectedButtonText: String
    val expectedHelpLinkText: String
    val expectedErrorTitle: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"PAYE employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitle: String = "Off-payroll working (IR35)"
    override val expectedHeading: String = "Off-payroll working (IR35)"
    override val yesText: String = "Yes"
    override val noText: String = "No"
    override val expectedParagraph2: String = "This means ABC Digital Ltd:"
    override val expectedLink: String = "Find out more about off-payroll working (opens in a new tab)"
    override val expectedErrorTitle = s"Error: $expectedTitle"
    override val expectedButtonText: String = "Continue"
    override val expectedHelpLinkText: String = "Get help with this page"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Cyflogaeth TWE ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedTitle: String = "Gweithio oddi ar y gyflogres (IR35)"
    override val expectedHeading: String = "Gweithio oddi ar y gyflogres (IR35)"
    override val yesText: String = "Iawn"
    override val noText: String = "Na"
    override val expectedParagraph2: String = "Mae hyn yn golygu:"
    override val expectedLink: String = "Dysgwch ragor am weithio oddi ar y gyflogres (yn agor tab newydd)"
    override val expectedErrorTitle = s"Gwall: $expectedTitle"
    override val expectedButtonText: String = "Yn eich blaen"
    override val expectedHelpLinkText: String = "Help gyda’r dudalen hon"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedRadioHeading: String = "Do you agree with ABC Digital’s decision?"
    override val expectedParagraph1: String = "ABC Digital Ltd has told HMRC you work for them via an intermediary and are subject to the off-payroll rules."
    override val expectedBullet1: String = "treated you as an employee for tax purposes"
    override val expectedBullet2: String = "deducted Income Tax and National Insurance contributions from your fees"
    override val expectedError = "Select yes if you agree with ABC Digital’s decision"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedRadioHeading: String = "A ydych yn cytuno â phenderfyniad ABC Digital Ltd?"
    override val expectedParagraph1: String = "Gwnaeth ABC Digital Ltd roi gwybod i CThEF, drwy gyfryngwr, eich bod yn gyflogai iddynt, ac felly yn destun rheolau oddi ar y gyflogres."
    override val expectedBullet1: String = "gwnaeth ABC Digital Ltd eich trin fel cyflogai at ddibenion treth"
    override val expectedBullet2: String = "gwnaeth ABC Digital Ltd ddidynnu Treth Incwm a chyfraniadau Yswiriant Gwladol o’ch ffioedd"
    override val expectedError = "Dewiswch ‘Iawn’ os ydych yn cytuno â phenderfyniad ABC Digital Ltd"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedRadioHeading: String = "Does your client agree with ABC Digital’s decision?"
    override val expectedParagraph1: String = "ABC Digital Ltd has told HMRC your client works for them via an intermediary and is subject to the off-payroll rules."
    override val expectedBullet1: String = "treated your client as an employee for tax purposes"
    override val expectedBullet2: String = "deducted Income Tax and National Insurance contributions from their fees"
    override val expectedError = "Select yes if your client agrees with ABC Digital’s decision"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedRadioHeading: String = "A yw’ch cleient yn cytuno â phenderfyniad ABC Digital Ltd?"
    override val expectedParagraph1: String = "Gwnaeth ABC Digital Ltd roi gwybod i CThEF, drwy gyfryngwr, fod eich cleient yn gyflogai iddynt, ac felly yn destun rheolau oddi ar y gyflogres."
    override val expectedBullet1: String = "gwnaeth ABC Digital Ltd drin eich cleient fel cyflogai at ddibenion treth"
    override val expectedBullet2: String = "gwnaeth ABC Digital Ltd ddidynnu Treth Incwm a chyfraniadau Yswiriant Gwladol o ffioedd eich cleient"
    override val expectedError = "Dewiswch ‘Iawn’ os yw’ch cleient yn cytuno â phenderfyniad ABC Digital Ltd"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[EmployerOffPayrollWorkingView]
  private def yesNoForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(s"employment.employerOpw.error.${if (isAgent) "agent" else "individual"}")

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "Render the employer off payroll working page " which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent), taxYearEOY)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedHeading)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph1, paragraph1)
        textOnPageCheck(expectedParagraph2, paragraph2)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet1, bullet1)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bullet2)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButton)
        formPostLinkCheck(EmployerOffPayrollWorkingController.submit(taxYearEOY).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "Render with empty form validation error" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph1, paragraph1)
        textOnPageCheck(expectedParagraph2, paragraph2)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet1, bullet1)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bullet2)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButton)
        formPostLinkCheck(EmployerOffPayrollWorkingController.submit(taxYearEOY).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
      }
    }
  }
}
