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

package views.benefits.accommodation

import controllers.benefits.accommodation.routes.LivingAccommodationBenefitsController
import forms.YesNoForm
import forms.benefits.accommodation.AccommodationFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.benefits.pages.LivingAccommodationBenefitsPageBuilder.aLivingAccommodationBenefitsPage
import views.html.benefits.accommodation.LivingAccommodationBenefitsView

class LivingAccommodationBenefitsViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"

  object Selectors {
    val paragraphSelector = "#main-content > div > div > p"
    val yesSelector = "#value"
    val continueButtonSelector = "#continue"
    val detailsTitleSelector = "#main-content > div > div > form > details > summary > span"
    val detailsText1Selector = "#main-content > div > div > form > details > div > p:nth-child(1)"
    val detailsText2Selector = "#main-content > div > div > form > details > div > p:nth-child(2)"
    val detailsText3Selector = "#main-content > div > div > form > details > div > p:nth-child(3)"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedParagraphText: String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedDetailsTitle: String
    val expectedDetailsText1: String
    val expectedDetailsText3: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
    val expectedDetailsText2: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedParagraphText: String = "Living accommodation is any accommodation that you can live in, whether you live there all " +
      "the time or only occasionally. It includes houses, flats, houseboats, holiday homes and apartments."
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetailsTitle = "More information about living accommodation"
    val expectedDetailsText1: String = "Living accommodation doesn’t include hotel rooms or board and lodgings, where you’re " +
      "dependent on someone else for cooking, cleaning or laundry."
    val expectedDetailsText3: String = "If you think all or part of this amount should be exempt from tax, refer to HS202 Living " +
      "accommodation and follow the working sheet."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedParagraphText: String = "Diffinnir llety fel llety y gallwch fyw ynddo, p’un a ydych yn byw ynddo drwy’r amser neu dim ond rhan o’r amser. " +
      "Mae’n cynnwys tai, fflatiau, cychod preswyl a lletyau gwyliau."
    val yesText = "Iawn"
    val noText = "Na"
    val buttonText = "Yn eich blaen"
    val expectedDetailsTitle = "Rhagor o wybodaeth am lety byw"
    val expectedDetailsText1: String = "Nid yw llety yn cynnwys ystafelloedd mewn gwesty neu ‘board and lodgings’, pan eich bod yn dibynnu ar rywun arall am goginio, glanhau neu olchi dillad."
    val expectedDetailsText3: String = "Os ydych yn credu y dylai’r swm cyfan, neu ran ohono, fod wedi ei eithrio rhag treth, cyfeiriwch at HS202 Living accommodation a dilynwch y daflen waith."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any living accommodation benefits?"
    val expectedHeading = "Did you get any living accommodation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you got living accommodation benefits"
    val expectedDetailsText2: String = "Your employment income should include the value of any living accommodation you or your " +
      "relations get because of your employment."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch unrhyw fuddiannau llety byw?"
    val expectedHeading = "A gawsoch unrhyw fuddiannau llety byw?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ os cawsoch unrhyw fuddiannau llety byw"
    val expectedDetailsText2: String = "Dylai’ch incwm o gyflogaeth gynnwys gwerth unrhyw lety byw rydych chi neu eich perthnasau yn ei gael oherwydd eich cyflogaeth."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any living accommodation benefits?"
    val expectedHeading = "Did your client get any living accommodation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client got living accommodation benefits"
    val expectedDetailsText2: String = "Your client’s employment income should include the value of any living accommodation they " +
      "or their relations get because of their employment."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient unrhyw fuddiannau llety byw?"
    val expectedHeading = "A gafodd eich cleient unrhyw fuddiannau llety byw?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ os cafodd eich cleient fuddiannau llety byw"
    val expectedDetailsText2: String = "Dylai incwm o gyflogaeth eich cleient gynnwys gwerth unrhyw lety y mae ef neu ei berthnasau yn ei gael oherwydd ei gyflogaeth."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def yesNoForm(isAgent: Boolean): Form[Boolean] = new AccommodationFormsProvider().livingAccommodationForm(isAgent)

  private lazy val underTest = inject[LivingAccommodationBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled radio buttons" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aLivingAccommodationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(expectedParagraphText, paragraphSelector)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(LivingAccommodationBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        textOnPageCheck(expectedDetailsTitle, detailsTitleSelector)
        textOnPageCheck(expectedDetailsText1, detailsText1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsText2, detailsText2Selector)
        textOnPageCheck(expectedDetailsText3, detailsText3Selector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with the 'yes' radio button pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aLivingAccommodationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent).fill(value = true))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(expectedParagraphText, paragraphSelector)
        radioButtonCheck(yesText, radioNumber = 1, checked = true)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(LivingAccommodationBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        textOnPageCheck(expectedDetailsTitle, detailsTitleSelector)
        textOnPageCheck(expectedDetailsText1, detailsText1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsText2, detailsText2Selector)
        textOnPageCheck(expectedDetailsText3, detailsText3Selector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with the 'no' radio button pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aLivingAccommodationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent).fill(value = false))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(expectedParagraphText, paragraphSelector)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = true)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(LivingAccommodationBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        textOnPageCheck(expectedDetailsTitle, detailsTitleSelector)
        textOnPageCheck(expectedDetailsText1, detailsText1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsText2, detailsText2Selector)
        textOnPageCheck(expectedDetailsText3, detailsText3Selector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with error when a form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aLivingAccommodationBenefitsPage.copy(isAgent = userScenario.isAgent, form = yesNoForm(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Some("value"))
        formPostLinkCheck(LivingAccommodationBenefitsController.submit(taxYearEOY, employmentId).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
