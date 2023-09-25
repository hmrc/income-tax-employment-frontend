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

package views.offPayrollWorking

import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.offPayrollWorking.EmployerOffPayrollWorkingStatusView

class EmployerOffPayrollWorkingStatusViewSpec extends ViewUnitTest {
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
  }

  trait CommonExpectedResults {
    val expectedParagraph1: String
    val expectedBullet1: String
    val expectedParagraph2: String
    val expectedButtonText: String
    val cancelLinkText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to change your employment details?"
    val expectedHeading = "Do you want to change your employment details?"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am newid eich manylion cyflogaeth?"
    val expectedHeading = "A ydych am newid eich manylion cyflogaeth?"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to change your client’s employment details?"
    val expectedHeading = "Do you want to change your client’s employment details?"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am newid manylion cyflogaeth eich cleient?"
    val expectedHeading = "A ydych am newid manylion cyflogaeth eich cleient?"

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedParagraph1 = "You are about to change information ABC Digital sent to HMRC:"
    val expectedBullet1 = "Off-payroll working status"
    val expectedParagraph2 = "HMRC may review this change."
    val expectedButtonText = "Confirm"
    val cancelLinkText = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedParagraph1 = "Rydych ar fin newid manylion a anfonwyd at CThEF gan ABC Digital Ltd:"
    val expectedBullet1 = "Statws gweithio oddi ar y gyflogres"
    val expectedParagraph2 = "Mae’n bosibl y bydd CThEF yn adolygu’r newid hwn"
    val expectedButtonText = "Cadarnhau"
    val cancelLinkText = "Canslo"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[EmployerOffPayrollWorkingStatusView]

  private val cancelUrl = controllers.offPayrollWorking.routes.EmployerOffPayrollWorkingController.show(taxYear).url
  private val continueUrl = s"http://localhost:9302/update-and-submit-income-tax-return/$taxYear/income-tax-return-overview"

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYear,
          continueUrl,
          cancelUrl
        )

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        textOnPageCheck(expectedParagraph1, paragraph1Selector)
        textOnPageCheck(expectedBullet1, bullet1Selector)
        textOnPageCheck(expectedParagraph2, paragraph2Selector)
        buttonCheck(expectedButtonText, continueButtonSelector)
        linkCheck(cancelLinkText, cancelLinkSelector, cancelUrl)
        welshToggleCheck(userScenario.isWelsh)
      }

    }
  }
}
