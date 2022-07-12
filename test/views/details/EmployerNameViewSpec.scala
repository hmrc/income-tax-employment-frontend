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

import controllers.details.routes.EmployerNameController
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
    val paragraphTextSelector: String = "#main-content > div > div > p.govuk-body"
    val formatListSelector1: String = "#main-content > div > div > ul > li:nth-child(1)"
    val formatListSelector2: String = "#main-content > div > div > ul > li:nth-child(2)"
    val formatListSelector3: String = "#main-content > div > div > ul > li:nth-child(3)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val expectedErrorWrongFormat: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val expectedErrorCharLimit: String
    val paragraphText: String
    val formatList1: String
    val formatList2: String
    val formatList3: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your employer?"
    val expectedH1 = "What’s the name of your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your employer"
    val expectedErrorWrongFormat = "Enter your employer name in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Beth oedd enwích cyflogwr?"
    val expectedH1 = "Beth oedd enwích cyflogwr?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch enwích cyflogwr"
    val expectedErrorWrongFormat = "Nodwch enwích cyflogwr yn y fformat cywir"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your client’s employer?"
    val expectedH1 = "What’s the name of your client’s employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your client’s employer"
    val expectedErrorWrongFormat = "Enter your client’s employer name in the correct format"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Beth yw enw cyflogwr eich cleient?"
    val expectedH1 = "Beth yw enw cyflogwr eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch enw cyflogwr eich cleient"
    val expectedErrorWrongFormat = "Nodwch enw cyflogwr eich cleient yn y fformat cywir"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedErrorCharLimit = "The employer name must be 74 characters or fewer"
    val paragraphText = "The employer name must be 74 characters or fewer. It can include:"
    val formatList1 = "upper and lower case letters (a to z)"
    val formatList2 = "numbers"
    val formatList3 = "the special characters: & : ’ \\ , . ( ) -"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val expectedErrorCharLimit = "Maeín rhaid i enwír cyflogwr fod yn 74 o gymeriadau neu lai"
    val paragraphText = "Maeín rhaid i enwír cyflogwr fod yn 74 o gymeriadau neu lai. Gall gynnwys y canlynol:"
    val formatList1 = "llythrennau mawr a bach (a i z)"
    val formatList2 = "rhifau"
    val formatList3 = "y cymeriadau arbennig: & : ’ \\ , . ( ) -"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  private def form(isAgent: Boolean): Form[String] = EmployerNameForm.employerNameForm(isAgent)

  private val underTest = inject[EmployerNameView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the 'name of your employer' page with the correct content" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        welshToggleCheck(userScenario.isWelsh)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerNameController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        textOnPageCheck(paragraphText, paragraphTextSelector)
        textOnPageCheck(formatList1, formatListSelector1)
        textOnPageCheck(formatList2, formatListSelector2)
        textOnPageCheck(formatList3, formatListSelector3)
      }

      "render the 'name of your employer' page with the correct content and pre-popped input field" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(employerName), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        welshToggleCheck(userScenario.isWelsh)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        inputFieldValueCheck(amountInputName, inputSelector, employerName)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerNameController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        textOnPageCheck(paragraphText, paragraphTextSelector)
        textOnPageCheck(formatList1, formatListSelector1)
        textOnPageCheck(formatList2, formatListSelector2)
        textOnPageCheck(formatList3, formatListSelector3)
      }

      s"render the page with a form error" when {
        "the submitted data is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(EmployerNameForm.employerName -> "")), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          import Selectors._
          import userScenario.commonExpectedResults._

          welshToggleCheck(userScenario.isWelsh)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)

          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, inputSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry)
        }

        "the submitted data is in the wrong format" which {
          val wrongFormat: String = "~name~"
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(EmployerNameForm.employerName -> wrongFormat)), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          import Selectors._
          import userScenario.commonExpectedResults._

          welshToggleCheck(userScenario.isWelsh)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          inputFieldValueCheck(amountInputName, inputSelector, wrongFormat)
          buttonCheck(expectedButtonText, continueButtonSelector)

          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorWrongFormat, inputSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorWrongFormat)
        }

        "the submitted data is too long" which {
          val charLimit: String = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uD3vujYg00ZtHwo3s0kyOOCEoAK9vuPiP374QKOe9o"

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(EmployerNameForm.employerName -> charLimit)), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          import Selectors._
          import userScenario.commonExpectedResults._

          welshToggleCheck(userScenario.isWelsh)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          inputFieldValueCheck(amountInputName, inputSelector, charLimit)
          buttonCheck(expectedButtonText, continueButtonSelector)

          errorSummaryCheck(expectedErrorCharLimit, inputSelector)
          errorAboveElementCheck(expectedErrorCharLimit)
        }
      }
    }
  }
}
