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

package views.details

import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.details.pages.EmployerTaxWarningPageBuilder.aEmployerTaxWarningPage
import views.html.details.EmployerTaxWarningView

class EmployerTaxWarningViewSpec extends ViewUnitTest {

  private val employerName = anEmploymentDetails.employerName
  object Selectors {
    val paragraph1Selector = "#main-content > div > div > p:nth-child(2)"
    val bullet1Selector: String = "#main-content > div > div > ul > li"
    val paragraph2Selector = "#main-content > div > div > p:nth-child(4)"
    val continueButtonSelector: String = "#continue"
    val cancelLinkSelector = "#main-content > div > div > div.govuk-button-group > a.govuk-link"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraph2: String
  }

  trait CommonExpectedResults {
    val expectedParagraph1: String
    val expectedBullet1: String
    val expectedButtonText: String
    val cancelLinkText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to change your employment details?"
    val expectedHeading = "Do you want to change your employment details?"
    val expectedParagraph2 = "This change affects tax you owe and will be reviewed by HMRC."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am newid eich manylion cyflogaeth?"
    val expectedHeading = "A ydych am newid eich manylion cyflogaeth?"
    val expectedParagraph2 = "Mae’r newid hwn yn effeithio ar y dreth sydd arnoch, a bydd CThEF yn ei adolygu."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to change your client’s employment details?"
    val expectedHeading = "Do you want to change your client’s employment details?"
    val expectedParagraph2 = "This change affects tax your client owes and will be reviewed by HMRC."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am newid manylion cyflogaeth eich cleient?"
    val expectedHeading = "A ydych am newid manylion cyflogaeth eich cleient?"
    val expectedParagraph2 = "Mae’r newid hwn yn effeithio ar y dreth sydd ar eich cleient, a bydd CThEF yn ei adolygu."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedParagraph1 = s"You are about to change information $employerName sent to HMRC:"
    val expectedBullet1 = "UK tax taken from pay"
    val expectedButtonText = "Confirm"
    val cancelLinkText = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedParagraph1 = s"Rydych ar fin newid manylion a anfonwyd at CThEF gan $employerName:"
    val expectedBullet1 = "Treth y DU a dynnwyd o’r cyflog"
    val expectedButtonText = "Cadarnhau"
    val cancelLinkText = "Canslo"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[EmployerTaxWarningView]

  private val cancelUrl = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/check-employment-details?employmentId=employmentId"

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aEmployerTaxWarningPage.copy(isAgent = userScenario.isAgent)
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        textOnPageCheck(expectedParagraph1, paragraph1Selector)
        textOnPageCheck(expectedBullet1, bullet1Selector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph2, paragraph2Selector)
        buttonCheck(expectedButtonText, continueButtonSelector)
        linkCheck(cancelLinkText, cancelLinkSelector, cancelUrl)
        welshToggleCheck(userScenario.isWelsh)
      }

    }
  }
}
