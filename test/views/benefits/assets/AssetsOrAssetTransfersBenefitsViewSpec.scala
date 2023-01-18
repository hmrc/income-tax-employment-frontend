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

package views.benefits.assets

import controllers.benefits.assets.routes.AssetsOrAssetTransfersBenefitsController
import forms.YesNoForm
import forms.benefits.assets.AssetsFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.assets.AssetsOrAssetTransfersBenefitsView

class AssetsOrAssetTransfersBenefitsViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"

  object Selectors {
    val paragraphSelector: Int => String = (index: Int) => s"#main-content > div > div > p.govuk-body:nth-child($index)"
    val bullet1Selector: String = "#main-content > div > div > ul.govuk-list > li:nth-child(1)"
    val bullet2Selector: String = "#main-content > div > div > ul.govuk-list > li:nth-child(2)"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedIncludesParagraph: String
    val expectedBullet2: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedDescriptionParagraph: String
    val expectedBullet1: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any assets from this company?"
    val expectedHeading = "Did you get any assets from this company?"
    val expectedIncludesParagraph = "Include assets that your employer let you:"
    val expectedBullet2 = "keep for yourself"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you got assets"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch unrhyw asedion gan y cwmni hwn?"
    val expectedHeading = "A gawsoch unrhyw asedion gan y cwmni hwn?"
    val expectedIncludesParagraph = "Dylech gynnwys asedion y gwnaeth eich cyflogwr eu rhoi dros dro i chi:"
    val expectedBullet2 = "i’w cadw eich hun"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os cawsoch asedion"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any assets from this company?"
    val expectedHeading = "Did your client get any assets from this company?"
    val expectedIncludesParagraph = "Include assets that their employer let them:"
    val expectedBullet2 = "keep for themselves"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client got assets"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient unrhyw asedion gan y cwmni hwn?"
    val expectedHeading = "A gafodd eich cleient unrhyw asedion gan y cwmni hwn?"
    val expectedIncludesParagraph = "Dylech gynnwys asedion y gwnaeth ei gyflogwr eu rhoi dros dro iddo:"
    val expectedBullet2 = "i’w cadw i’w hun"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd eich cleient asedion"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedDescriptionParagraph = "Assets are things like computers, televisions or bicycles."
    val expectedBullet1 = "use"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val expectedDescriptionParagraph = "Asedion yw pethau fel cyfrifiaduron, setiau teledu neu feiciau."
    val expectedBullet1 = "defnyddio"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new AssetsFormsProvider().assetsOrAssetTransfersForm(isAgent)

  private lazy val underTest = inject[AssetsOrAssetTransfersBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render 'assets from company' page with the correct content with no pre-filling" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(expectedDescriptionParagraph, paragraphSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedIncludesParagraph, paragraphSelector(3))
        textOnPageCheck(expectedBullet1, bullet1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bullet2Selector)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(AssetsOrAssetTransfersBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render 'assets from company' page with the correct content with yes pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(expectedDescriptionParagraph, paragraphSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedIncludesParagraph, paragraphSelector(3))
        textOnPageCheck(expectedBullet1, bullet1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bullet2Selector)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(AssetsOrAssetTransfersBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      s"return an error when a user submits an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption)
        textOnPageCheck(expectedDescriptionParagraph, paragraphSelector(3))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedIncludesParagraph, paragraphSelector(4))
        textOnPageCheck(expectedBullet1, bullet1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedBullet2, bullet2Selector)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(AssetsOrAssetTransfersBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText, Some("value"))
      }
    }
  }
}
