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
    val contentSelector = "#main-content > div > div > p.govuk-body"
    val hintTestSelector = "#payeRef-hint"
    val inputSelector = "#payeRef"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#payeRef"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedContentNewAccount: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedH1: String
    val continueButtonText: String
    val hintText: String
    val wrongFormatErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment details for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedH1: String = "What’s the PAYE reference of maggie?"
    val continueButtonText = "Continue"
    val hintText = "For example, 123/AB456"
    val wrongFormatErrorText: String = "Enter a PAYE reference in the correct format"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val expectedH1: String = "Beth yw cyfeirnod TWE maggie?"
    val continueButtonText = "Yn eich blaen"
    val hintText = "Er enghraifft, 123/AB456"
    val wrongFormatErrorText: String = "Nodwch gyfeirnod TWE yn y fformat cywir"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s the PAYE reference of your employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentNewAccount: String = "You can find this on your P60 or on letters about PAYE. It may be called ‘Employer PAYE reference’ or ‘PAYE reference’."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s the PAYE reference of your client’s employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentNewAccount: String = "You can find this on P60 forms or on letters about PAYE. It may be called ‘Employer PAYE reference’ or ‘PAYE reference’."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Beth yw cyfeirnod TWE eich cyflogwr?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedContentNewAccount: String = "Gallwch ddod o hyd i hwn ar ffurflenni P60 neu ar lythyrau ynghylch TWE. " +
      "Mae’n bosibl y cyfeirir ato fel ‘Cyfeirnod TWE y Cyflogwr’ neu fel ‘Cyfeirnod TWE’."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Beth yw cyfeirnod TWE cyflogwr eich cleient?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedContentNewAccount: String = "Gallwch ddod o hyd i hwn ar ffurflenni P60 neu ar lythyrau ynghylch TWE. " +
      "Mae’n bosibl y cyfeirir ato fel ‘Cyfeirnod TWE y Cyflogwr’ neu fel ‘Cyfeirnod TWE’."
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

        val htmlFormat = underTest(form.fill(value = payeRefValue), taxYear = taxYearEOY, employerName, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedTitle, user.isWelsh)
        h1Check(expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(hintText, hintTestSelector)
        inputFieldValueCheck(PayeRefForm.payeRef, inputSelector, payeRefValue)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(PayeRefController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(user.isWelsh)
      }

      "render What's the PAYE reference of xxx? page with an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val htmlFormat = underTest(form, taxYear = taxYearEOY, employerName, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedTitle, user.isWelsh)
        h1Check(expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(hintText, hintTestSelector)
        textOnPageCheck(get.expectedContentNewAccount, contentSelector)
        inputFieldValueCheck(PayeRefForm.payeRef, inputSelector, "")
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(PayeRefController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(user.isWelsh)
      }

      "render the page with a form error when the input is in the wrong" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val invalidPaye = "123/abc " + employmentId + "<Q>"
        val htmlFormat = underTest(form.bind(Map(payeRef -> invalidPaye)), taxYear = taxYearEOY, employerName, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(get.expectedErrorTitle, user.isWelsh)
        h1Check(expectedH1)
        captionCheck(expectedCaption)
        inputFieldValueCheck(PayeRefForm.payeRef, inputSelector, invalidPaye)
        errorSummaryCheck(wrongFormatErrorText, expectedErrorHref)
        welshToggleCheck(user.isWelsh)
      }
    }
  }
}
