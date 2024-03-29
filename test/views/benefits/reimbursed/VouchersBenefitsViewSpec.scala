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

package views.benefits.reimbursed

import controllers.benefits.reimbursed.routes.VouchersBenefitsController
import forms.YesNoForm
import forms.benefits.reimbursed.ReimbursedFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.reimbursed.VouchersBenefitsView

class VouchersBenefitsViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"

  object Selectors {
    val paragraphSelector: String = "#main-content > div > div > p"
    val bullet1Selector: String = "#main-content > div > div > ul.govuk-list > li:nth-child(1)"
    val bullet2Selector: String = "#main-content > div > div > ul.govuk-list > li:nth-child(2)"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val paragraph: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedBullet1: String
    val expectedBullet2: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get vouchers or credit cards?"
    val expectedHeading = "Did you get vouchers or credit cards?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you got vouchers or credit cards"
    val paragraph = "We only need to know about vouchers you can exchange for:"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch dalebau neu gardiau credyd?"
    val expectedHeading = "A gawsoch dalebau neu gardiau credyd?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os cawsoch dalebau neu gardiau credyd"
    val paragraph = "Dim ond talebau y gallwch eu cyfnewid y mae angen i ni wybod amdanynt:"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get vouchers or credit cards?"
    val expectedHeading = "Did your client get vouchers or credit cards?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client got vouchers or credit cards"
    val paragraph = "We only need to know about vouchers your client can exchange for:"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient dalebau neu gardiau credyd?"
    val expectedHeading = "A gafodd eich cleient dalebau neu gardiau credyd?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd eich cleient dalebau neu gardiau credyd"
    val paragraph = "Dim ond talebau y gall eich cleient eu cyfnewid y mae angen i ni wybod amdanynt:"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedBullet1 = "goods and services"
    val expectedBullet2 = "a sum of money substantially less than the cost to provide them"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val bullet1 = "goods and services"
    val bullet2 = "a sum of money substantially less than the cost to provide them"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val expectedBullet1 = "nwyddau a gwasanaethau"
    val expectedBullet2 = "swm o arian sy’n sylweddol is na’r gost i’w darparu"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
    val bullet1 = "nwyddau a gwasanaethau"
    val bullet2 = "swm o arian sy’n sylweddol is na’r gost i’w darparu"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new ReimbursedFormsProvider().vouchersForm(isAgent)

  private lazy val underTest = inject[VouchersBenefitsView]

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
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.paragraph, paragraphSelector)
        textOnPageCheck(expectedBullet1, bullet1Selector)
        textOnPageCheck(expectedBullet2, bullet2Selector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(VouchersBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'yes' radio button prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.paragraph, paragraphSelector)
        textOnPageCheck(expectedBullet1, bullet1Selector)
        textOnPageCheck(expectedBullet2, bullet2Selector)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(VouchersBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'no' radio button prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = false), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.paragraph, paragraphSelector)
        textOnPageCheck(expectedBullet1, bullet1Selector)
        textOnPageCheck(expectedBullet2, bullet2Selector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(VouchersBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when a user submits an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.paragraph, paragraphSelector)
        textOnPageCheck(expectedBullet1, bullet1Selector)
        textOnPageCheck(expectedBullet2, bullet2Selector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(VouchersBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText, Some("value"))
      }
    }
  }
}
