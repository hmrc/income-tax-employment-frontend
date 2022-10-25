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

package views.benefits.medical

import controllers.benefits.medical.routes.ChildcareBenefitsController
import forms.YesNoForm
import forms.benefits.medical.MedicalFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.medical.ChildcareBenefitsView

class ChildcareBenefitdsViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"
  private val exemptLink: String = "https://www.gov.uk/expenses-and-benefits-childcare/whats-exempt"

  object Selectors {
    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-child($index)"

    val onlyNeedLinkSelector: String = "#exempt-link"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
    val expectedTheseAre: String
    val expectedCheckWith: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
    val expectedWeOnly: String
    val expectedWeOnlyLink: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get a childcare benefit?"
    val expectedH1 = "Did you get a childcare benefit?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got a childcare benefit"
    val expectedTheseAre = "These are childcare costs your employer paid for. It can include vouchers or commercial childcare costs."
    val expectedCheckWith = "Check with your employer if you are unsure."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch fuddiant gofal plant?"
    val expectedH1 = "A gawsoch fuddiant gofal plant?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cawsoch fuddiant gofal plant"
    val expectedTheseAre = "Costau gofal plant y talodd eich cyflogwr amdanynt yw’r rhain. Gall gynnwys talebau neu gostau gofal plant masnachol."
    val expectedCheckWith = "Gwiriwch â’ch cyflogwr os nad ydych yn siŵr."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get a childcare benefit?"
    val expectedH1 = "Did your client get a childcare benefit?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got a childcare benefit"
    val expectedTheseAre = "These are childcare costs your client’s employer paid for. It can include vouchers or commercial childcare costs."
    val expectedCheckWith = "Check with your client’s employer if you are unsure."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient fuddiant gofal plant?"
    val expectedH1 = "A gafodd eich cleient fuddiant gofal plant?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cafodd eich cleient fuddiant gofal plant"
    val expectedTheseAre = "Costau gofal plant y talodd cyflogwr eich cleient amdanynt yw’r rhain. Gall gynnwys talebau neu gostau gofal plant masnachol."
    val expectedCheckWith = "Gwiriwch â chyflogwr eich cleient os nad ydych yn siŵr."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val expectedWeOnly = "We only need to know about childcare costs above the exempt limit (opens in new tab)."
    val expectedWeOnlyLink = "exempt limit (opens in new tab)."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
    val expectedWeOnly = "Dim ond costau gofal plant sy’n uwch na throthwy’r eithriad y mae angen i ni wybod amdanynt (yn agor tab newydd)."
    val expectedWeOnlyLink = "y mae angen i ni wybod amdanynt (yn agor tab newydd)."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new MedicalFormsProvider().childcareForm(isAgent)

  private lazy val underTest = inject[ChildcareBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedTheseAre, paragraphSelector(index = 2))
        textOnPageCheck(expectedWeOnly, paragraphSelector(3))
        linkCheck(expectedWeOnlyLink, onlyNeedLinkSelector, exemptLink)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCheckWith, paragraphSelector(index = 4))
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(ChildcareBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'yes' radio button prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedTheseAre, paragraphSelector(2))
        textOnPageCheck(expectedWeOnly, paragraphSelector(3))
        linkCheck(expectedWeOnlyLink, onlyNeedLinkSelector, exemptLink)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCheckWith, paragraphSelector(4))
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(ChildcareBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'no' radio button prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = false), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedTheseAre, paragraphSelector(2))
        textOnPageCheck(expectedWeOnly, paragraphSelector(3))
        linkCheck(expectedWeOnlyLink, onlyNeedLinkSelector, exemptLink)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCheckWith, paragraphSelector(4))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(ChildcareBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when a user submits an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedTheseAre, paragraphSelector(3))
        textOnPageCheck(expectedWeOnly, paragraphSelector(4))
        linkCheck(expectedWeOnlyLink, onlyNeedLinkSelector, exemptLink)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedCheckWith, paragraphSelector(5))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(ChildcareBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
      }
    }
  }
}
