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

import controllers.details.routes.DidYouLeaveEmployerController
import forms.details.EmploymentDetailsFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.benefits.pages.DidYouLeaveEmployerPageBuilder.aDidYouLeaveEmployerPage
import utils.ViewUtils.{translatedDateFormatter, translatedTaxYearEndDateFormatter}
import views.html.details.DidYouLeaveEmployerView

import java.time.LocalDate

class DidYouLeaveEmployerViewSpec extends ViewUnitTest {

  private val formsProvider = new EmploymentDetailsFormsProvider()

  private val employmentId: String = "employmentId"

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    def expectedTitle(taxYear: Int, startDate: LocalDate): String

    def expectedH1(taxYear: Int, startDate: LocalDate): String = expectedTitle(taxYear, startDate)

    def expectedErrorTitle(taxYear: Int, startDate: LocalDate): String

    def expectedError(taxYear: Int, startDate: LocalDate): String
  }

  trait CommonExpectedResults {
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    def expectedTitle(taxYear: Int, startDate: LocalDate): String =
      s"Did you leave between ${translatedDateFormatter(startDate)(defaultMessages)} and ${translatedTaxYearEndDateFormatter(taxYear)(defaultMessages)}?"

    def expectedErrorTitle(taxYear: Int, startDate: LocalDate): String = s"Error: ${expectedTitle(taxYear, startDate)}"

    def expectedError(taxYear: Int, startDate: LocalDate): String =
      s"Select yes if you left between ${translatedDateFormatter(startDate)(defaultMessages)} and ${translatedTaxYearEndDateFormatter(taxYear)(defaultMessages)}"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    def expectedTitle(taxYear: Int, startDate: LocalDate): String =
      s"Did you leave between ${translatedDateFormatter(startDate)(welshMessages)} and ${translatedTaxYearEndDateFormatter(taxYear)(welshMessages)}?"

    def expectedErrorTitle(taxYear: Int, startDate: LocalDate): String = s"Gwall: ${expectedTitle(taxYear, startDate)}"

    def expectedError(taxYear: Int, startDate: LocalDate): String =
      s"Select yes if you left between ${translatedDateFormatter(startDate)(welshMessages)} and ${translatedTaxYearEndDateFormatter(taxYear)(welshMessages)}"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    def expectedTitle(taxYear: Int, startDate: LocalDate): String =
      s"Did your client leave between ${translatedDateFormatter(startDate)(defaultMessages)} and ${translatedTaxYearEndDateFormatter(taxYear)(defaultMessages)}?"

    def expectedErrorTitle(taxYear: Int, startDate: LocalDate): String = s"Error: ${expectedTitle(taxYear, startDate)}"

    def expectedError(taxYear: Int, startDate: LocalDate): String =
      s"Select yes if your client left between ${translatedDateFormatter(startDate)(defaultMessages)} and ${translatedTaxYearEndDateFormatter(taxYear)(defaultMessages)}"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    def expectedTitle(taxYear: Int, startDate: LocalDate): String =
      s"Did your client leave between ${translatedDateFormatter(startDate)(welshMessages)} and ${translatedTaxYearEndDateFormatter(taxYear)(welshMessages)}?"

    def expectedErrorTitle(taxYear: Int, startDate: LocalDate): String = s"Gwall: ${expectedTitle(taxYear, startDate)}"

    def expectedError(taxYear: Int, startDate: LocalDate): String =
      s"Select yes if your client left between ${translatedDateFormatter(startDate)(welshMessages)} and ${translatedTaxYearEndDateFormatter(taxYear)(welshMessages)}"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val underTest = inject[DidYouLeaveEmployerView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    import userScenario.specificExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aDidYouLeaveEmployerPage.copy(isAgent = userScenario.isAgent)
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(get.expectedTitle(taxYearEOY, pageModel.titleFirstDate), userScenario.isWelsh)
        h1Check(get.expectedTitle(taxYearEOY, pageModel.titleFirstDate), isFieldSetH1 = true)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(DidYouLeaveEmployerController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }

      "render page with the 'yes' radio button pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aDidYouLeaveEmployerPage.copy(isAgent = userScenario.isAgent, form = aDidYouLeaveEmployerPage.form.fill(value = true))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(userScenario.isWelsh)

        titleCheck(get.expectedTitle(taxYearEOY, pageModel.titleFirstDate), userScenario.isWelsh)
        h1Check(get.expectedTitle(taxYearEOY, pageModel.titleFirstDate), isFieldSetH1 = true)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(DidYouLeaveEmployerController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }

      "render page with the 'no' radio button pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aDidYouLeaveEmployerPage.copy(isAgent = userScenario.isAgent, form = aDidYouLeaveEmployerPage.form.fill(value = false))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(userScenario.isWelsh)

        titleCheck(get.expectedTitle(taxYearEOY, pageModel.titleFirstDate), userScenario.isWelsh)
        h1Check(get.expectedTitle(taxYearEOY, pageModel.titleFirstDate), isFieldSetH1 = true)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(DidYouLeaveEmployerController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }

      "render page with error when a form is submitted with no entry and start date is before tax year start" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = formsProvider.didYouLeaveForm(userScenario.isAgent, taxYearEOY, aDidYouLeaveEmployerPage.titleFirstDate)
        val pageModel = aDidYouLeaveEmployerPage.copy(isAgent = userScenario.isAgent, form = form.bind(Map("value" -> "")))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(userScenario.isWelsh)

        titleCheck(get.expectedErrorTitle(taxYearEOY, pageModel.titleFirstDate), userScenario.isWelsh)
        h1Check(get.expectedTitle(taxYearEOY, pageModel.titleFirstDate), isFieldSetH1 = true)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(DidYouLeaveEmployerController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)

        errorSummaryCheck(get.expectedError(taxYearEOY, pageModel.titleFirstDate), Selectors.yesSelector)
        errorAboveElementCheck(get.expectedError(taxYearEOY, pageModel.titleFirstDate), Some("value"))
      }
    }
  }
}
