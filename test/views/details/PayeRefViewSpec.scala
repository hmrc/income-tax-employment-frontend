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

import controllers.details.routes.PayeRefController
import forms.details.PayeRefForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.details.PayeRefView

class PayeRefViewSpec extends ViewUnitTest {

  private val payeRef = "payeRef"
  private val payeRefValue: String = "123/AA12345"
  private val employmentId = "employmentId"
  private val employerName = "maggie"

  object Selectors {
    val hintTestSelector = "#payeRef-hint"
    val inputSelector = "#payeRef"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#payeRef"
  }

  trait SpecificExpectedResults {
    val hintEmploymentEndedText: String
    val hintEmploymentNotEndedText: String
  }

  trait CommonExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val continueButtonText: String
    val wrongFormatErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedH1: String = "What is maggie’s employer PAYE reference? (optional)"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle: String = s"Error: $expectedH1"
    val continueButtonText = "Continue"
    val wrongFormatErrorText: String = "Enter PAYE reference in the correct format"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedH1: String = "What is maggie‘s employer PAYE reference? (optional)"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val continueButtonText = "Yn eich blaen"
    val wrongFormatErrorText: String = "Enter PAYE reference in the correct format"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val hintEmploymentEndedText: String = "This is a 3 digit tax office number, a forward slash, and a tax office employer reference, like 123/AB45678. " +
      "It may be called ‘Employer PAYE reference’ or ‘PAYE reference’. It will be on your P45."
    val hintEmploymentNotEndedText: String = "This is a 3 digit tax office number, a forward slash, and a tax office employer reference, like 123/AB45678. " +
      "It may be called ‘Employer PAYE reference’ or ‘PAYE reference’. It will be on your P60."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val hintEmploymentEndedText: String = "This is a 3 digit tax office number, a forward slash, and a tax office employer reference, like 123/AB45678. " +
      "It may be called ‘Employer PAYE reference’ or ‘PAYE reference’. It will be on your client‘s P45."
    val hintEmploymentNotEndedText: String = "This is a 3 digit tax office number, a forward slash, and a tax office employer reference, like 123/AB45678. " +
      "It may be called ‘Employer PAYE reference’ or ‘PAYE reference’. It will be on your client‘s P60."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val hintEmploymentEndedText: String = "This is a 3 digit tax office number, a forward slash, and a tax office employer reference, like 123/AB45678. " +
      "It may be called ‘Employer PAYE reference’ or ‘PAYE reference’. It will be on your P45."
    val hintEmploymentNotEndedText: String = "This is a 3 digit tax office number, a forward slash, and a tax office employer reference, like 123/AB45678. " +
      "It may be called ‘Employer PAYE reference’ or ‘PAYE reference’. It will be on your P60."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val hintEmploymentEndedText: String = "This is a 3 digit tax office number, a forward slash, and a tax office employer reference, like 123/AB45678. " +
      "It may be called ‘Employer PAYE reference’ or ‘PAYE reference’. It will be on your client‘s P45."
    val hintEmploymentNotEndedText: String = "This is a 3 digit tax office number, a forward slash, and a tax office employer reference, like 123/AB45678. " +
      "It may be called ‘Employer PAYE reference’ or ‘PAYE reference’. It will be on your client‘s P60."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  private val form = PayeRefForm.payeRefForm
  private val underTest = inject[PayeRefView]

  userScenarios.foreach { user =>
    import Selectors._
    import user.commonExpectedResults._
    import user.specificExpectedResults._

    s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
      "render What's the PAYE reference of xxx? page with a pre-filled form when there is a previous PAYE Ref defined" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(form.fill(value = payeRefValue), taxYear = taxYearEOY, employerName, employmentId, employmentEnded = true)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, user.isWelsh)
        labelH1Check(expectedH1)
        textOnPageCheck(get.hintEmploymentEndedText, hintTestSelector)
        inputFieldValueCheck(PayeRefForm.payeRef, inputSelector, payeRefValue)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(PayeRefController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(user.isWelsh)
      }

      "render What's the PAYE reference of xxx? page with an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(form, taxYear = taxYearEOY, employerName, employmentId, employmentEnded = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, user.isWelsh)
        labelH1Check(expectedH1)
        textOnPageCheck(get.hintEmploymentNotEndedText, hintTestSelector)
        inputFieldValueCheck(PayeRefForm.payeRef, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(PayeRefController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(user.isWelsh)
      }

      "render the page with a form error when the input is in the wrong" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val invalidPaye = "123/abc " + employmentId + "<Q>"
        val htmlFormat = underTest(form.bind(Map(payeRef -> invalidPaye)), taxYear = taxYearEOY, employerName, employmentId, employmentEnded = true)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedErrorTitle, user.isWelsh)
        labelH1Check(expectedH1)
        inputFieldValueCheck(PayeRefForm.payeRef, inputSelector, invalidPaye)
        errorSummaryCheck(wrongFormatErrorText, expectedErrorHref)
        welshToggleCheck(user.isWelsh)
      }
    }
  }
}
