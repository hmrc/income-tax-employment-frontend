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

import controllers.benefits.fuel.routes.CarVanFuelBenefitsController
import forms.YesNoForm
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.benefits.fuel.CarVanFuelBenefitsView

class CarVanFuelBenefitsViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"

  object Selectors {
    val thisIncludesSelector: String = "#main-content > div > div > p.govuk-body"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
    val thisIncludes: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any car, van or fuel benefits from this company?"
    val expectedH1 = "Did you get any car, van or fuel benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got car, van or fuel benefits"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch unrhyw fuddiannau car, fan neu danwydd gan y cwmni hwn?"
    val expectedH1 = "A gawsoch unrhyw fuddiannau car, fan neu danwydd gan y cwmni hwn?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ëIawní os cawsoch fuddiannau trethadwy car, fan neu danwydd"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any car, van or fuel benefits from this company?"
    val expectedH1 = "Did your client get any car, van or fuel benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got car, van or fuel benefits"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient unrhyw fuddiannau car, fan neu danwydd gan y cwmni hwn?"
    val expectedH1 = "A gafodd eich cleient unrhyw fuddiannau car, fan neu danwydd gan y cwmni hwn?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ëIawní os cafodd eich cleient fuddiannau car, fan neu danwydd"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val thisIncludes = "This includes benefits such as company cars or vans, company car or van fuel, and privately owned vehicle mileage allowances."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
    val thisIncludes = "Mae hyn yn cynnwys buddiannau fel ceir neu faniau cwmni, tanwydd ar gyfer car neu fan cwmni, a lwfansau milltiroedd ar gyfer cerbydau syín eiddo preifat."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new FuelFormsProvider().carVanFuelForm(isAgent)

  private lazy val underTest = inject[CarVanFuelBenefitsView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      import Selectors._
      import userScenario.commonExpectedResults._
      "render page with no pre-filled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(thisIncludes, thisIncludesSelector)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(CarVanFuelBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
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
        textOnPageCheck(thisIncludes, thisIncludesSelector)
        radioButtonCheck(yesText, radioNumber = 1, checked = true)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(CarVanFuelBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when a user submits an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY, employmentId)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(thisIncludes, thisIncludesSelector)
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(CarVanFuelBenefitsController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedError, Some("value"))
      }
    }
  }
}
