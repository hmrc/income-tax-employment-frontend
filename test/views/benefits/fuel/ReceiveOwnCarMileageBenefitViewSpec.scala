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

package views.benefits.fuel

import controllers.benefits.fuel.routes.ReceiveOwnCarMileageBenefitController
import forms.YesNoForm
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.fuel.ReceiveOwnCarMileageBenefitView

class ReceiveOwnCarMileageBenefitViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val p1Selector = "#main-content > div > div > p:nth-of-type(1)"
    val p2Selector = "#main-content > div > div > p:nth-of-type(2)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedP1: String
    val expectedP2: String
    val expectedErrorTitle: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get a mileage benefit for using your own car for work?"
    val expectedH1 = "Did you get a mileage benefit for using your own car for work?"
    val expectedP1 = "We only need to know about payments made above our ‘approved amount’. If you have payments above the ‘approved amount’, they should be recorded in section E of your P11D form."
    val expectedP2 = "Check with your employer if you are unsure."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got a mileage benefit for using your own car for work"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get a mileage benefit for using their own car for work?"
    val expectedH1 = "Did your client get a mileage benefit for using their own car for work?"
    val expectedP1: String = "We only need to know about payments made above our ‘approved amount’. " +
      "If your client has payments above the ‘approved amount’, they should be recorded in section E of their P11D form."
    val expectedP2 = "Check with your client’s employer if you are unsure."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got a mileage benefit for using their own car for work"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch fuddiant milltiroedd am ddefnyddio eich car eich hun ar gyfer gwaith?"
    val expectedH1 = "A gawsoch fuddiant milltiroedd am ddefnyddio eich car eich hun ar gyfer gwaith?"
    val expectedP1: String = "Dim ond taliadau sy’n uwch na’n ‘swm cymeradwy’ y mae angen i ni wybod amdanynt. Os oes gennych daliadau " +
      "sy’n uwch na’r ‘swm cymeradwy’, dylid eu cofnodi yn adran E o’ch ffurflen P11D."
    val expectedP2 = "Gwiriwch â’ch cyflogwr os nad ydych yn siŵr."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cawsoch fuddiant milltiroedd am ddefnyddio’ch car eich hun ar gyfer gwaith"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient fuddiant milltiroedd am ddefnyddio ei gar ei hun ar gyfer gwaith?"
    val expectedH1 = "A gafodd eich cleient fuddiant milltiroedd am ddefnyddio ei gar ei hun ar gyfer gwaith?"
    val expectedP1: String = "Dim ond taliadau sy’n uwch na’n ‘swm cymeradwy’ y mae angen i ni wybod amdanynt. Os oes gan eich " +
      "cleient daliadau sy’n uwch na’r ‘swm cymeradwy’, dylid eu cofnodi yn adran E o’i ffurflen P11D."
    val expectedP2 = "Gwiriwch â chyflogwr eich cleient os nad ydych yn siŵr."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cafodd eich cleient fuddiant milltiroedd am ddefnyddio ei gar ei hun ar gyfer gwaith"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new FuelFormsProvider().receiveOwnCarMileageForm(isAgent)

  private lazy val underTest = inject[ReceiveOwnCarMileageBenefitView]

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
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, p1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP2, p2Selector)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(ReceiveOwnCarMileageBenefitController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'yes' radio button prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, p1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP2, p2Selector)
        radioButtonCheck(yesText, radioNumber = 1, checked = true)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(ReceiveOwnCarMileageBenefitController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'no' radio button prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = false), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, p1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP2, p2Selector)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = true)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(ReceiveOwnCarMileageBenefitController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when a user submits an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))

        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
