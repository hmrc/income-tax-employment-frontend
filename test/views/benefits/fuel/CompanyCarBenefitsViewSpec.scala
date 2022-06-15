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

package views.benefits.fuel

import controllers.benefits.fuel.routes.CompanyCarBenefitsController
import forms.YesNoForm
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.fuel.CompanyCarBenefitsView

class CompanyCarBenefitsViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"

  object Selectors {
    val yesSelector = "#value"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val radioTextYes: String
    val radioTextNo: String
    val errorText: String
    val buttonText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did you get a company car benefit?"
    val expectedH1: String = "Did you get a company car benefit?"
    val expectedError: String = "Select yes if you got a company car benefit"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "A gawsoch fuddiant car cwmni?"
    val expectedH1: String = "A gawsoch fuddiant car cwmni?"
    val expectedError: String = "Dewiswch ëIawní os cawsoch fuddiant car cwmni"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client get a company car benefit?"
    val expectedH1: String = "Did your client get a company car benefit?"
    val expectedError: String = "Select yes if your client got a company car benefit"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "A gafodd eich cleient fuddiant car cwmni?"
    val expectedH1: String = "A gafodd eich cleient fuddiant car cwmni?"
    val expectedError: String = "Dewiswch ëIawní os cafodd eich cleient fuddiant car cwmni"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val radioTextYes: String = "Yes"
    val radioTextNo: String = "No"
    val errorText: String = "Error: "
    val buttonText: String = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val radioTextYes: String = "Iawn"
    val radioTextNo: String = "Na"
    val errorText: String = "Gwall: "
    val buttonText: String = "Yn eich blaen"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new FuelFormsProvider().companyCarForm(isAgent)

  private lazy val underTest = inject[CompanyCarBenefitsView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        radioButtonCheck(userScenario.commonExpectedResults.radioTextYes, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.radioTextNo, radioNumber = 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.buttonText)
        formPostLinkCheck(CompanyCarBenefitsController.submit(taxYearEOY, employmentId).url, Selectors.continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when a user submits an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.commonExpectedResults.errorText + userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
        radioButtonCheck(userScenario.commonExpectedResults.radioTextYes, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.radioTextNo, 2, checked = false)
        buttonCheck(userScenario.commonExpectedResults.buttonText)
        formPostLinkCheck(CompanyCarBenefitsController.submit(taxYearEOY, employmentId).url, Selectors.continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
