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

package views.benefits.travel

import forms.YesNoForm
import forms.benefits.travel.TravelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.http.Status.BAD_REQUEST
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.travel.IncidentalOvernightCostEmploymentBenefitsView

class IncidentalOvernightCostEmploymentBenefitsViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentIdo"

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"

    def paragraphTextSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
    val costInformation: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
    val allowanceInformation: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any incidental overnight costs?"
    val expectedH1 = "Did you get any incidental overnight costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got incidental overnight costs"
    val costInformation = "These are personal costs you incurred while travelling overnight on business."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch unrhyw mân gostau dros nos?"
    val expectedH1 = "A gawsoch unrhyw mân gostau dros nos?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cawsoch chi mân gostau dros nos"
    val costInformation = "Mae’r rhain yn gostau personol yr ysgwyddwyd arnoch wrth deithio dros nos ar fusnes."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any incidental overnight costs?"
    val expectedH1 = "Did your client get any incidental overnight costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got incidental overnight costs"
    val costInformation = "These are personal costs they incurred while travelling overnight on business."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient unrhyw mân gostau dros nos?"
    val expectedH1 = "A gafodd eich cleient unrhyw mân gostau dros nos?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cafodd eich cleient mân gostau dros nos"
    val costInformation = "Mae’r rhain yn gostau personol yr ysgwyddwyd arno wrth iddo deithio dros nos ar fusnes."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val allowanceInformation: String = "The allowance for travelling within the UK is £5 per night and outside of the UK is £10 per night. We only need to know about costs above the allowance."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
    val allowanceInformation: String = "Y lwfans ar gyfer teithio yn y DU yw £5 y noson a £10 y noson y tu allan i’r DU. Dim ond costau sy’n uwch na’r lwfans y mae angen i ni wybod amdanynt."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new TravelFormsProvider().incidentalOvernightCostEmploymentBenefitsForm(isAgent)

  private lazy val underTest = inject[IncidentalOvernightCostEmploymentBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render 'Did you get any incidental overnight costs?' page with the correct content with no pre-filling" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.costInformation, paragraphTextSelector(index = 1))
        textOnPageCheck(allowanceInformation, paragraphTextSelector(index = 2))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.travel.routes.IncidentalOvernightCostEmploymentBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render 'Did you get any incidental overnight costs?' page with the correct content with cya data and the yes value pre-filled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(true), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption)
        textOnPageCheck(userScenario.specificExpectedResults.get.costInformation, paragraphTextSelector(index = 1))
        textOnPageCheck(allowanceInformation, paragraphTextSelector(index = 2))
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.benefits.travel.routes.IncidentalOvernightCostEmploymentBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {
        "the value is empty" which {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(userScenario.specificExpectedResults.get.costInformation, paragraphTextSelector(index = 1))
          textOnPageCheck(allowanceInformation, paragraphTextSelector(index = 2))
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.benefits.travel.routes.IncidentalOvernightCostEmploymentBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
        }
      }
    }
  }
}
